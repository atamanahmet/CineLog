# Usage: make <command> [service ...] [dev|neon]
# The mode and the services are plain words after the command. No mode word means dev.
# Rules for env files: docs/ENVIRONMENT.md

COMMANDS      := help start stop rebuild config db-init smoke embed
MODE_COMMANDS := start rebuild config db-init embed
MODES         := dev neon
MODE_ALIASES  := local
SERVICES      := backend rec-engine frontend postgres
WAIT_TIMEOUT  ?= 180
EMBED_PYTHON  ?= python
PAGES         ?= 500

COMMAND       := $(or $(firstword $(MAKECMDGOALS)),help)
WORDS         := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
MODE_WORDS    := $(filter $(MODES) $(MODE_ALIASES),$(WORDS))
SERVICE_WORDS := $(filter $(SERVICES),$(WORDS))
ACTIVE_MODE   := $(or $(patsubst local,dev,$(firstword $(MODE_WORDS))),dev)

ifeq ($(filter $(COMMAND),$(COMMANDS)),)
$(error Unknown command "$(COMMAND)". Run: make help)
endif
ifneq ($(filter-out $(MODES) $(MODE_ALIASES) $(SERVICES),$(WORDS)),)
$(error Unknown word: $(filter-out $(MODES) $(MODE_ALIASES) $(SERVICES),$(WORDS)). Modes: $(MODES) (local is dev). Services: $(SERVICES))
endif
ifneq ($(word 2,$(MODE_WORDS)),)
$(error Use one mode word only, got: $(MODE_WORDS))
endif
ifeq ($(filter $(COMMAND),$(MODE_COMMANDS)),)
ifneq ($(WORDS),)
$(error "make $(COMMAND)" does not take extra words)
endif
endif
ifneq ($(SERVICE_WORDS),)
ifneq ($(COMMAND),rebuild)
$(error Service words work only with rebuild)
endif
endif
ifeq ($(COMMAND),rebuild)
ifeq ($(SERVICE_WORDS),)
$(error Usage: make rebuild <$(SERVICES)> [$(MODES)])
endif
endif

ENV_FILES := .env $(if $(filter neon,$(ACTIVE_MODE)),.env.neon)
ifneq ($(filter $(COMMAND),start stop rebuild config db-init),)
$(foreach f,$(ENV_FILES),$(if $(wildcard $(f)),,$(error $(f) not found, copy $(f:%=%.example) to $(f))))
endif

COMPOSE      := docker compose $(foreach f,$(ENV_FILES),--env-file $(f)) -f docker-compose.yml -f docker-compose.$(ACTIVE_MODE).yml
UP           := $(COMPOSE) up -d --build --wait --wait-timeout $(WAIT_TIMEOUT)
PSQL_EXEC    := $(COMPOSE) exec -T postgres sh -c
PSQL         := psql -v ON_ERROR_STOP=1 -U "$$POSTGRES_USER"
service_url   = http://localhost:$$($(COMPOSE) port $(1) $(2) | sed 's/.*://')

ifneq ($(WORDS),)
.PHONY: $(WORDS)
$(WORDS):
	@:
endif

.DEFAULT_GOAL := help
.PHONY: help start stop rebuild config db-up db-init smoke embed

# Shows the available commands.
help:
	@echo "make start [neon]                  Build and start all, wait until healthy"
	@echo "make stop                          Stop all, data volumes are kept"
	@echo "make rebuild <service ...> [neon]  Rebuild and restart services, dependencies are not touched"
	@echo "make config [neon]                 Check env files and compose files, prints no secret"
	@echo "make db-init                       Create the rec database and apply SQL (dev only)"
	@echo "make smoke                         Check /ready and /rec/update auth wiring"
	@echo "make embed [local|neon] [PAGES=<n>]  Embed new TMDB titles, then reload the rec engine"
	@echo ""
	@echo "Modes: $(MODES). local is another word for dev, the default (local Postgres). neon uses the Neon database."
	@echo "Services: $(SERVICES)"

# Validates env files and compose files. Prints nothing when all is fine.
config:
	@$(COMPOSE) config --quiet

# Starts all services. In dev mode db-init runs first, so roles and schema exist
# before rec-engine connects. Orphans are removed, so switching from dev to neon
# removes the local Postgres container. Volumes are kept.
start: $(if $(filter dev,$(ACTIVE_MODE)),db-init)
	$(UP) --remove-orphans
	@echo ""
	@echo "Frontend   : $(call service_url,frontend,80)"
	@echo "Backend    : $(call service_url,backend,8080)"
	@echo "Rec engine : $(call service_url,rec-engine,8181)"

# Starts only the local Postgres container and waits until it is healthy.
db-up:
	$(UP) --remove-orphans postgres

ifeq ($(ACTIVE_MODE),dev)
# Creates the rec database, then applies catalog_embedding DDL and local role
# passwords to it. The backend database is never touched, so Flyway keeps a
# clean schema. Safe to run more than once, all SQL files are idempotent.
db-init: db-up
	$(PSQL_EXEC) '$(PSQL) -d "$$POSTGRES_DB" -v rec_db="$$REC_DB_NAME" -f -' < embedding-script/sql/001_local_database.sql
	$(PSQL_EXEC) '$(PSQL) -d "$$REC_DB_NAME" -f -' < embedding-script/sql/001_catalog_embedding.sql
	$(PSQL_EXEC) '$(PSQL) -d "$$REC_DB_NAME" -f -' < embedding-script/sql/002_catalog_embedding_genres.sql
	$(PSQL_EXEC) '$(PSQL) -d "$$REC_DB_NAME" -v rec_pw="$$REC_ENGINE_DB_PASSWORD" -v script_pw="$$EMBEDDING_SCRIPT_DB_PASSWORD" -f -' < embedding-script/sql/001_local_roles.sql
else
# There is no local Postgres in neon mode, so there is nothing to do.
db-init:
	@echo "db-init does nothing in neon mode, there is no local postgres"
endif

# Stops all project containers. Mode files are not loaded, so neon variables
# are not needed. dev-only services such as postgres are removed too.
# Volumes stay (no -v).
stop:
	docker compose --env-file .env -f docker-compose.yml down --remove-orphans

# Rebuilds the given services for the mode. Dependencies are not touched.
rebuild:
	$(UP) --no-deps $(SERVICE_WORDS)

# Empty loved returns 400, that status still proves UPDATE_TOKEN auth passed.
smoke:
	@curl -sf http://127.0.0.1:8181/ready | python3 -c "import sys,json; d=json.load(sys.stdin); assert d.get('status')=='ok' and int(d.get('rows',0))>0, d"
	@code=$$(curl -s -o /dev/null -w '%{http_code}' -X POST http://127.0.0.1:8181/rec/update -H 'Content-Type: application/json' -H 'Authorization: Bearer wrong-token' -d '{"loved":[],"limit":1}'); test "$$code" = "401"
	@token=$$(docker exec cinelog-rec-engine printenv UPDATE_TOKEN); code=$$(curl -s -o /dev/null -w '%{http_code}' -X POST http://127.0.0.1:8181/rec/update -H 'Content-Type: application/json' -H "Authorization: Bearer $$token" -d '{"loved":[],"limit":1}'); test "$$code" = "400" -o "$$code" = "200"

# Embeds TMDB titles that are not stored yet, then asks the rec engine to reload.
# Stored titles are skipped. Runs on the host. The script reads .env and,
# for neon, .env.neon on top, chosen by CINELOG_MODE.
embed:
	CINELOG_MODE=$(ACTIVE_MODE) $(EMBED_PYTHON) embedding-script/embed.py --pages $(PAGES)

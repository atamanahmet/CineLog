.PHONY: start stop rebuild help

include .env
export

help:
	@echo ""
	@echo "==============================="
	@echo "  CineLog Makefile Commands"
	@echo "==============================="
	@echo "  make start                        - Build and start all services"
	@echo "  make stop                         - Stop all services"
	@echo "  make rebuild service=<name>       - Rebuild and restart a single service"
	@echo ""
	@echo "  Available service names:"
	@echo "    cinelog-postgres"
	@echo "    cinelog-backend"
	@echo "    cinelog-rec-engine"
	@echo "    cinelog-frontend"
	@echo "==============================="

start:
	docker compose up --build -d
	@echo "Waiting for postgres to be ready..."
	@until docker inspect -f '{{.State.Health.Status}}' cinelog-postgres 2>/dev/null | grep -q "healthy"; do sleep 2; done
	@echo "Waiting for backend to be ready..."
	@until docker inspect -f '{{.State.Health.Status}}' cinelog-backend 2>/dev/null | grep -q "healthy"; do sleep 2; done
	@echo "Waiting for rec-engine to be ready..."
	@until docker inspect -f '{{.State.Health.Status}}' cinelog-rec-engine 2>/dev/null | grep -q "healthy"; do sleep 2; done
	@echo "Waiting for frontend to be ready..."
	@until curl -s -o /dev/null -w "%{http_code}" http://localhost:80 | grep -q "200"; do sleep 2; done
	@echo ""
	@echo "==============================="
	@echo "  CineLog is running"
	@echo "==============================="
	@echo "  Frontend   : http://localhost:80"
	@echo "  Backend    : http://localhost:$(SERVER_PORT)"
	@echo "  Rec Engine : http://localhost:8181"
	@echo "==============================="
	@echo ""
	@echo "  Commands:"
	@echo "  make stop                         - Stop all services"
	@echo "  make rebuild service=<name>       - Rebuild and restart a single service"
	@echo ""
	@echo "  Available service names:"
	@echo "    cinelog-postgres"
	@echo "    cinelog-backend"
	@echo "    cinelog-rec-engine"
	@echo "    cinelog-frontend"
	@echo "==============================="

stop:
	docker compose down

rebuild:
	@if [ -z "$(service)" ]; then echo "Usage: make rebuild service=<service-name>"; exit 1; fi
	docker compose stop $(service)
	docker compose build $(service)
	docker compose start $(service)
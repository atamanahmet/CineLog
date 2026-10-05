# syntax=docker/dockerfile:1
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install -r requirements.txt
RUN useradd --create-home --uid 1000 appuser
COPY --chown=appuser:appuser \
     config.py logging_setup.py auth.py catalog.py catalog_store.py catalog_loader.py \
     recommender.py request_parsing.py routes.py app.py wsgi.py gunicorn.conf.py ./
USER appuser
EXPOSE 8181
CMD ["gunicorn", "--config", "gunicorn.conf.py", "wsgi:app"]
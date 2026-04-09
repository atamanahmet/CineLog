# syntax=docker/dockerfile:1
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install torch==2.11.0+cpu --index-url https://download.pytorch.org/whl/cpu && \
    pip install -r requirements.txt
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('all-MiniLM-L6-v2')"
COPY RecommendationService.py .
EXPOSE 8181
CMD ["python", "RecommendationService.py"]
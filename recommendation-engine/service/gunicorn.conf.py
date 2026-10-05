import os

bind = f"0.0.0.0:{os.getenv('PORT', '8000')}"
# Must stay 1 because catalog and rate limiter live in process memory.
workers = 1
worker_class = "gthread"
threads = 4
timeout = 120

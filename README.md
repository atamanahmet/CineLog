# CineLog

A movie tracking and recommendation app. Browse films via TMDB, manage watchlist/watched/loved lists, and get content-based recommendations from a standalone Python microservice.

## Prerequisites

- **Java 17+ JVM** - backend
- **PostgreSQL** - database
- **Python 3.x** - recommendation service

## How it works

**Frontend**
- Built with React
- JWT auth stored in HTTP-only cookies
- TMDB powered browsing. Includes posters, trailers, cast and full metadata
- Watchlist, watched, and loved list management
- Recommendations update automatically when the loved list changes

**Backend**
- Built with Java and Spring Boot
- Handles auth, list management, and TMDB metadata
- A startup class that starts everything needed on boot

**Recommendation Engine**
- Standalone Python/Flask microservice to keep ML logic out of the Java backend. Python has better ML libraries
- Generates embeddings with `sentence-transformers` (all-MiniLM-L6-v2) for movie titles and overviews
- Ranks results via weighted cosine similarity across title, overview, and genre, factoring in popularity
- Updates the index automatically when the loved list changes

## Getting Started

**Frontend**
- Run `npm install` in the frontend directory
- Run `npm run dev`

**Backend**
- Copy `.env.example` to `.env` and fill in DB credentials
- Run `./mvnw spring-boot:run`

**Recommendation Engine**
- `pip install -r requirements.txt`
- `python app.py`

## Notes

- Recommendation weights (`overview: 0.8, popularity: 0.15, genre: 0.05`) and the similarity threshold (`0.4`) are hand-picked, not trained on data

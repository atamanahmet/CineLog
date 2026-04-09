from flask import Flask, jsonify, request  # type: ignore
import requests  # type: ignore
from sentence_transformers import SentenceTransformer  # type: ignore
from sklearn.metrics.pairwise import cosine_similarity  # type: ignore
import numpy as np  # type: ignore
from sklearn.preprocessing import MultiLabelBinarizer  # type: ignore

app = Flask(__name__)

model = SentenceTransformer("all-MiniLM-L6-v2")
mlb = MultiLabelBinarizer()

WEIGHT_OVERVIEW = 0.5
WEIGHT_TITLE = 0.2
WEIGHT_GENRE = 0.3

SIMILARITY_THRESHOLD = 0.4

recommendations = []
sorted_idx = []


def normalize(vecs):
    norms = np.linalg.norm(vecs, axis=1, keepdims=True)
    norms[norms == 0] = 1
    return vecs / norms


def prepare_features(movies, fit_mlb=True):
    overviews = [m.get("overview", "") or "" for m in movies]
    titles = [m.get("title", "") or "" for m in movies]
    genres = [m.get("genre_ids", []) for m in movies]

    emb_overview = model.encode(overviews, convert_to_numpy=True)
    emb_title = model.encode(titles, convert_to_numpy=True)

    if fit_mlb:
        genre_enc = mlb.fit_transform(genres)
    else:
        genre_enc = mlb.transform(genres)

    emb_overview_norm = normalize(emb_overview)
    emb_title_norm = normalize(emb_title)
    genre_norm = normalize(genre_enc.astype(float))

    combined = np.hstack(
        [
            emb_overview_norm * WEIGHT_OVERVIEW,
            emb_title_norm * WEIGHT_TITLE,
            genre_norm * WEIGHT_GENRE,
        ]
    )

    return combined


@app.route("/rec/update", methods=["POST"])
def rec_update():
    global recommendations, sorted_idx

    try:
        movie_db_response = requests.get("http://cinelog-backend:8080/api/movies")
        movie_db = movie_db_response.json()
    except Exception as e:
        return jsonify({"error": f"Failed to load movie DB: {str(e)}"}), 500

    combined_all = prepare_features(movie_db, fit_mlb=True)

    loved_movies = request.get_json()
    if not loved_movies:
        recommendations = []
        sorted_idx = []
        return jsonify([])

    combined_loved = prepare_features(loved_movies, fit_mlb=False)
    if combined_loved.shape[0] == 0:
        recommendations = []
        sorted_idx = []
        return jsonify([])

    user_vec = np.mean(combined_loved, axis=0).reshape(1, -1)

    similarities = cosine_similarity(user_vec, combined_all)[0]

    loved_ids_set = set(m.get("id") for m in loved_movies if "id" in m)

    recommendations = [
        (movie_db[i], sim)
        for i, sim in enumerate(similarities)
        if movie_db[i].get("id") not in loved_ids_set and sim >= SIMILARITY_THRESHOLD
    ]

    if not recommendations:
        sorted_idx = []
        return jsonify([])

    popularity_scores = np.array(
        [movie.get("popularity", 0) for movie, _ in recommendations]
    )

    if popularity_scores.max() - popularity_scores.min() > 1e-8:
        pop_norm = (popularity_scores - popularity_scores.min()) / (
            popularity_scores.max() - popularity_scores.min()
        )
    else:
        pop_norm = np.zeros_like(popularity_scores)

    sim_scores = np.array([sim for _, sim in recommendations])

    loved_genres = set(
        gid for movie in loved_movies for gid in movie.get("genre_ids", [])
    )

    def genre_overlap(movie):
        movie_genres = set(movie.get("genre_ids", []))
        return len(loved_genres.intersection(movie_genres))

    genre_overlaps = np.array([genre_overlap(movie) for movie, _ in recommendations])

    overviewFactor = 0.8
    popularityFactor = 0.15
    genreFactor = 0.05

    final_scores = (
        overviewFactor * sim_scores
        + popularityFactor * pop_norm
        + genreFactor * genre_overlaps
    )

    sorted_idx = np.argsort(final_scores)[::-1]

    top_recs = [recommendations[i][0] for i in sorted_idx]
    return jsonify(top_recs)


@app.route("/health")
def health():
    return jsonify({"status": "ok"}), 200


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8181, debug=True)

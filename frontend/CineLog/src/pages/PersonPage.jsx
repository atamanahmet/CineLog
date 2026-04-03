import { react, useState, useEffect } from "react";
import { useUser } from "../context/UserContext";

export default function PersonPage() {
  return (
    <>
      {detail && (
        <div className="min-h-screen bg-amber-950 text-white pb-20 ">
          {/* Backdrop */}
          <div className="relative h-96 md:h-[500px] overflow-hidden ">
            <img
              src={detail.backdrop_path}
              alt={detail.title}
              className="w-full h-full object-cover"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-amber-950 via-amber-950/60 to-transparent" />
          </div>

          {/* Main Content */}
          <div className="relative -mt-32 md:-mt-117 px-4 md:px-8 max-w-7xl mx-auto">
            <div className="flex flex-col md:flex-row gap-8">
              {/* Poster */}
              <div className="flex-shrink-0">
                <img
                  src={detail.poster_path}
                  alt={detail.title}
                  className="w-64 md:w-80 rounded-lg shadow-2xl mx-auto md:mx-0"
                />
              </div>

              {/* detail Info */}
              <div className="flex-1 pt-8 md:pt-16 -mt-16">
                <div className="mb-4">
                  <h1 className="text-4xl md:text-6xl font-bold mb-2">
                    {detail.title}
                  </h1>
                  {detail.original_title !== detail.title && (
                    <p className="text-xl text-gray-400 italic">
                      {detail.original_title}
                    </p>
                  )}
                </div>

                {/* Rating and Meta Info */}
                <div className="flex flex-wrap details-center gap-6 mb-6">
                  <div className="flex details-center gap-2">
                    <Star className="w-5 h-5 text-yellow-400 fill-current" />
                    <span
                      className={`text-lg font-semibold ${getRatingColor(
                        detail.vote_average
                      )}`}
                    >
                      {detail.vote_average.toFixed(1)}
                    </span>
                    <span className="text-gray-400">
                      ({detail.vote_count.toLocaleString()} votes)
                    </span>
                  </div>

                  <div className="flex details-center gap-2">
                    <Calendar className="w-5 h-5 text-blue-400" />
                    <span>{formatDate(detail.release_date)}</span>
                  </div>

                  <div className="flex details-center gap-2">
                    <Globe className="w-5 h-5 text-green-400" />
                    <span className="uppercase">
                      {detail.original_language}
                    </span>
                  </div>

                  {detail.adult && (
                    <div className="bg-red-600 px-2 py-1 rounded text-sm font-semibold">
                      18+
                    </div>
                  )}
                </div>

                {/* Genres */}
                <div className="mb-6">
                  <div className="flex flex-wrap gap-2">
                    {detail.genre_ids.map((genreId) => (
                      <span
                        key={genreId}
                        className="bg-gray-700 hover:bg-gray-600 transition-colors px-3 py-1 rounded-full text-sm"
                      >
                        {genreMap[genreId] || "Unknown"}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Overview */}
                <div className="mb-8">
                  <h2 className="text-2xl font-semibold mb-4">Overview</h2>
                  <p className="text-gray-300 leading-relaxed text-lg max-w-4xl">
                    {detail.overview}
                  </p>
                </div>

                {/* Action Buttons */}
                <div className="flex flex-wrap gap-4">
                  {/* <a href={trailer}> */}
                  <button
                    className="bg-amber-600 hover:bg-amber-700 transition-colors px-6 py-3 rounded-lg flex details-center gap-2 font-semibold"
                    onClick={openVideoModal}
                    disabled={!trailer}
                  >
                    <Play className="w-5 h-5" />
                    {trailer != null ? "Watch Trailer" : "No Trailer Available"}
                  </button>
                  {/* </a> */}
                  {user && <ListButton item={detail} style={style} />}
                  {/* <button className="bg-gray-700 hover:bg-gray-600 transition-colors px-6 py-3 rounded-lg font-semibold">
                    Add to Watchlist
                  </button> */}
                </div>
                {/* Cast Section */}
                {cast.length > 0 && (
                  <div className="mt-12">
                    <h2 className="text-2xl font-semibold mb-6 flex items-center gap-2">
                      <Users className="w-6 h-6 text-amber-400" />
                      Cast
                    </h2>
                    <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-4">
                      {cast.map((actor) => (
                        <div key={actor.id} className="text-center">
                          <div className="relative mb-3">
                            <img
                              src={
                                actor.profile_path.endsWith("null")
                                  ? missing
                                  : actor.profile_path
                              }
                              alt={actor.name}
                              className="w-full aspect-square object-cover shadow-lg rounded-full bg-amber-700"
                              onError={(e) => {
                                e.target.src = { profile };
                              }}
                            />
                          </div>
                          <h3 className="text-sm font-semibold text-white mb-1 truncate">
                            {actor.name}
                          </h3>
                          <p className="text-xs text-gray-400 truncate">
                            {actor.character || actor.job}
                          </p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
                {/* Additional Stats */}
                <div className="mt-8 grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div className="bg-gray-800 p-4 rounded-lg text-center">
                    <div className="text-2xl font-bold text-blue-400">
                      {detail.popularity.toFixed(0)}
                    </div>
                    <div className="text-sm text-gray-400">Popularity</div>
                  </div>
                  <div className="bg-gray-800 p-4 rounded-lg text-center">
                    <div className="text-2xl font-bold text-green-400">
                      {detail.vote_count.toLocaleString()}
                    </div>
                    <div className="text-sm text-gray-400">Reviews</div>
                  </div>
                  <div className="bg-gray-800 p-4 rounded-lg text-center">
                    <div className="text-2xl font-bold text-purple-400">
                      #{detail.id}
                    </div>
                    <div className="text-sm text-gray-400">detail ID</div>
                  </div>
                  <div className="bg-gray-800 p-4 rounded-lg text-center">
                    <div className="text-2xl font-bold text-yellow-400">
                      {detail.video ? "Yes" : "No"}
                    </div>
                    <div className="text-sm text-gray-400">Has Video</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <VideoModal
            isOpen={isVideoModalOpen}
            onClose={closeVideoModal}
            videoUrl={trailer}
            title={detail.title}
          />
        </div>
      )}
    </>
  );
}

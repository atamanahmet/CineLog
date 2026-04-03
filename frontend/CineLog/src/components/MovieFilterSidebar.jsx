import React, { useState } from "react";
import {
  ChevronLeft,
  ChevronRight,
  Filter,
  Star,
  Calendar,
  Tag,
  Globe,
  ArrowDownWideNarrow,
} from "lucide-react";
import { useUser } from "../context/UserContext";

const MovieFilterSidebar = () => {
  const { mediaType, filters, setFilters } = useUser();
  const [isCollapsed, setIsCollapsed] = useState(true);
  const [expandedSections, setExpandedSections] = useState({
    sort: true,
    genre: false,
    year: true,
    rating: true,
    duration: false,
    language: false,
  });

  const toggleSection = (section) => {
    setExpandedSections((prev) => ({
      ...prev,
      [section]: !prev[section],
    }));
  };

  const handleGenreToggle = (genre) => {
    setFilters((prev) => ({
      ...prev,
      genres: prev.genres.includes(genre.id)
        ? prev.genres.filter((g) => g !== genre.id)
        : [...prev.genres, genre.id],
    }));
  };

  const handleSortChange = (sortValue) => {
    setFilters((prev) => ({
      ...prev,
      sort: sortValue,
    }));
  };

  const handleLanguageToggle = (language) => {
    setFilters((prev) => ({
      ...prev,
      languages: prev.languages.includes(language.language)
        ? prev.languages.filter((l) => l !== language.language)
        : [...prev.languages, language.language],
    }));
  };

  const handleYearChange = (index, value) => {
    const newYearRange = [...filters.yearRange];
    newYearRange[index] = Number(value);
    setFilters((prev) => ({
      ...prev,
      yearRange: newYearRange,
    }));
  };

  const handleRatingChange = (index, value) => {
    const newRating = [...filters.rating];
    newRating[index] = Number(value);
    setFilters((prev) => ({
      ...prev,
      rating: newRating,
    }));
  };

  const sortSelection = [
    { sort: "popularity.desc", name: "Popularity" },
    { sort: "vote_average.desc", name: "Rating" },
    {
      sort: mediaType === "movie" ? "release_date.desc" : "first_air_date.desc",
      name: "Release Date",
    },
  ];

  const genres = [
    { id: 28, name: "Action" },
    { id: 12, name: "Adventure" },
    { id: 16, name: "Animation" },
    { id: 35, name: "Comedy" },
    { id: 80, name: "Crime" },
    { id: 99, name: "Documentary" },
    { id: 18, name: "Drama" },
    { id: 10751, name: "Family" },
    { id: 14, name: "Fantasy" },
    { id: 36, name: "History" },
    { id: 27, name: "Horror" },
    { id: 10402, name: "Music" },
    { id: 9648, name: "Mystery" },
    { id: 10749, name: "Romance" },
    { id: 878, name: "Science Fiction" },
    { id: 10770, name: "TV Movie" },
    { id: 53, name: "Thriller" },
    { id: 10752, name: "War" },
    { id: 37, name: "Western" },
  ];

  const languages = [
    { language: "en", name: "English" },
    { language: "tr", name: "Turkish" },
    { language: "es", name: "Spanish" },
    { language: "fr", name: "French" },
    { language: "de", name: "German" },
    { language: "it", name: "Italian" },
    { language: "ja", name: "Japanese" },
    { language: "ko", name: "Korean" },
    { language: "zh", name: "Mandarin" },
  ];

  const FilterSection = ({
    title,
    icon: Icon,
    isExpanded,
    onToggle,
    children,
  }) => (
    <div className="border-b border-amber-900 last:border-b-0">
      <button
        onClick={onToggle}
        className="w-full flex items-center justify-between p-2.5 hover:bg-amber-900 transition-colors"
        type="button"
      >
        <div className="flex items-center gap-3">
          <Icon size={18} className="text-amber-500" />
          {!isCollapsed && (
            <span className="font-medium text-amber-100">{title}</span>
          )}
        </div>
        {!isCollapsed && (
          <ChevronRight
            size={16}
            className={`text-amber-500 transition-transform ${
              isExpanded ? "rotate-90" : ""
            }`}
          />
        )}
      </button>
      {!isCollapsed && (
        <div
          className={`overflow-hidden transition-all duration-300 ${
            isExpanded ? "max-h-96 pb-4" : "max-h-0"
          }`}
        >
          <div className="px-4">{children}</div>
        </div>
      )}
    </div>
  );

  const CheckboxGroup = ({
    items,
    selectedItems,
    onToggle,
    maxHeight = "max-h-48",
    itemKey = "name",
    valueKey = "id",
  }) => (
    <div className={`space-y-2 overflow-y-auto ${maxHeight} custom-scrollbar`}>
      {items.map((item) => {
        const displayValue = item[itemKey];
        const checkValue = item[valueKey];
        return (
          <label
            key={checkValue}
            className="flex items-center gap-3 cursor-pointer group"
          >
            <input
              type="checkbox"
              checked={selectedItems.includes(checkValue)}
              onChange={() => onToggle(item)}
              className="w-4 h-4 text-amber-500 bg-amber-700 border-amber-600 rounded focus:ring-amber-500 focus:ring-2"
            />
            <span className="text-sm text-amber-400 group-hover:text-amber-100 transition-colors">
              {displayValue}
            </span>
          </label>
        );
      })}
    </div>
  );

  return (
    <div
      className={`bg-amber-800 transition-all duration-300 ease-in-out ${
        isCollapsed ? "w-10" : "w-60"
      } border-r border-amber-700 flex flex-col`}
    >
      {/* Header */}
      <div className="flex items-center justify-between border-b border-amber-700">
        {!isCollapsed && (
          <div className="flex items-center gap-3">
            <Filter className="text-amber-500 ml-3" size={15} />
            <h2 className="text-lg font-semibold text-white">Filters</h2>
          </div>
        )}
        <button
          onClick={() => setIsCollapsed(!isCollapsed)}
          className="hover:bg-amber-700 rounded-lg transition-colors w-15 p-2 text-center"
          type="button"
        >
          {isCollapsed ? (
            <div className="flex justify-center items-center">
              <Filter className="text-amber-500" size={15} />
              <ChevronRight className="text-amber-500 ml-1" size={10} />
            </div>
          ) : (
            <>
              <ChevronLeft className="text-amber-500" size={15} />
              <p className="-mt-5 ml-6 text-amber-100">X</p>
            </>
          )}
        </button>
      </div>

      {/* Filters */}
      <div className="flex-1 overflow-y-auto custom-scrollbar flex flex-col">
        <FilterSection
          title="Sort"
          icon={ArrowDownWideNarrow}
          isExpanded={expandedSections.sort}
          onToggle={() => toggleSection("sort")}
        >
          <div className="space-y-2 max-h-48 overflow-y-auto custom-scrollbar px-2">
            {sortSelection.map((sortOption) => (
              <label
                key={sortOption.name}
                className="flex items-center gap-3 cursor-pointer group"
              >
                <input
                  type="radio"
                  name="sort"
                  value={sortOption.sort}
                  checked={filters.sort === sortOption.sort}
                  onChange={() => handleSortChange(sortOption.sort)}
                  className="w-4 h-4 text-amber-500 bg-amber-700 border-amber-600 rounded focus:ring-amber-500 focus:ring-2"
                />
                <span className="text-sm text-amber-400 group-hover:text-amber-100 transition-colors">
                  {sortOption.name}
                </span>
              </label>
            ))}
          </div>
        </FilterSection>

        <FilterSection
          title="Genres"
          icon={Tag}
          isExpanded={expandedSections.genre}
          onToggle={() => toggleSection("genre")}
        >
          <CheckboxGroup
            items={genres}
            selectedItems={filters.genres}
            onToggle={handleGenreToggle}
            itemKey="name"
            valueKey="id"
          />
        </FilterSection>

        <FilterSection
          title="Release Year"
          icon={Calendar}
          isExpanded={expandedSections.year}
          onToggle={() => toggleSection("year")}
        >
          <div className="flex gap-2 px-4 py-2">
            <input
              type="number"
              value={filters.yearRange[0]}
              onChange={(e) => handleYearChange(0, e.target.value)}
              min={1900}
              max={2050}
              className="w-1/2 rounded bg-amber-700 text-amber-100 p-1"
              placeholder="Min Year"
            />
            <input
              type="number"
              value={filters.yearRange[1]}
              onChange={(e) => handleYearChange(1, e.target.value)}
              min={1900}
              max={2050}
              className="w-1/2 rounded bg-amber-700 text-amber-100 p-1"
              placeholder="Max Year"
            />
          </div>
        </FilterSection>

        <FilterSection
          title="Rating"
          icon={Star}
          isExpanded={expandedSections.rating}
          onToggle={() => toggleSection("rating")}
        >
          <div className="flex gap-2 px-4 py-2">
            <input
              type="number"
              min={0}
              max={10}
              step={0.1}
              value={filters.rating[0]}
              onChange={(e) => handleRatingChange(0, e.target.value)}
              className="w-1/2 rounded bg-amber-700 text-amber-100 p-1"
              placeholder="Min Rating"
            />
            <input
              type="number"
              min={0}
              max={10}
              step={0.1}
              value={filters.rating[1]}
              onChange={(e) => handleRatingChange(1, e.target.value)}
              className="w-1/2 rounded bg-amber-700 text-amber-100 p-1"
              placeholder="Max Rating"
            />
          </div>
        </FilterSection>

        <FilterSection
          title="Language"
          icon={Globe}
          isExpanded={expandedSections.language}
          onToggle={() => toggleSection("language")}
        >
          <CheckboxGroup
            items={languages}
            selectedItems={filters.languages}
            onToggle={handleLanguageToggle}
            itemKey="name"
            valueKey="language"
            maxHeight="max-h-32"
          />
        </FilterSection>
      </div>
    </div>
  );
};

export default MovieFilterSidebar;

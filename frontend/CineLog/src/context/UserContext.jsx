import { createContext, useEffect, useState, useContext } from "react";
import { useNavigate } from "react-router";
import api from "../api/axiosInstance";

const UserContext = createContext();

export const useUser = () => useContext(UserContext);

export const UserProvider = ({ children }) => {
  const navigate = useNavigate();
  const [mediaType, setMediaType] = useState("movie");
  const [watchlist, setWatchlist] = useState(new Set());
  const [watchlistIdSet, setWatchlistIdSet] = useState(new Set());
  const [watchedlistIdSet, setWatchedlistIdSet] = useState(new Set());
  const [watchedlist, setWatchedlist] = useState(new Set());
  const [lovedlist, setLovedlist] = useState(new Set());
  const [recommendation, setRecommendation] = useState();
  const [lovedlistIdSet, setLovedlistIdSet] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [profilePictureUrl, setProfilePictureUrl] = useState(null);
  const [user, setUser] = useState(null);
  const [logoutResult, setLogoutResult] = useState(null);
  const [searchResponse, setSearchResponse] = useState(null);
  const [detail, setDetail] = useState(null);
  const [cast, setCast] = useState([]);
  const [movieData, setMovieData] = useState(null);
  const [isFetching, setIsFetching] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [byPass, setByPass] = useState(false);

  const [filters, setFilters] = useState({
    genres: [],
    yearRange: [1900, 2040],
    rating: [0, 10],
    duration: [60, 180],
    languages: [],
    sort: "",
  });

  const storedPhoto = sessionStorage.getItem("profilePhoto");

  const navigateToDetails = (media) => {
    api
      .get(`/${mediaType}/${media.id}/credits`)
      .then((res) => setCast(res.data))
      .catch((err) => console.log("Error: " + err));
    setDetail(media);
    navigate("/details");
  };

  const fetchUser = () => {
    setLoading(true);
    api
      .get("/api/me")
      .then((res) => {
        login(res.data);
        getProfilePhoto();
        getWatchList();
      })
      .catch((err) => {
        console.log("Error: " + err);
        setUser(null);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchUser();
  }, [location.pathname]);

  useEffect(() => {
    if (user) {
      getWatchList();
      getProfilePhoto();
    }
  }, [user]);

  async function handleUpload(file) {
    if (file != null) {
      const formData = new FormData();
      formData.append("profilePicture", file);
      await api
        .post("/user/upload", formData, {
          headers: { "content-type": "multipart/form-data" },
        })
        .catch((err) => console.log("Error: " + err));
      getProfilePhoto();
    }
  }

  async function getProfilePhoto() {
    if (user) {
      try {
        const response = await api.get("/user/photo", {
          responseType: "blob",
        });
        if (response.status === 200) {
          const imageObjectUrl = URL.createObjectURL(response.data);
          setProfilePictureUrl(imageObjectUrl);

          const reader = new FileReader();
          reader.onloadend = () => {
            sessionStorage.setItem("profilePhoto", reader.result);
          };
          reader.readAsDataURL(response.data);
        }
      } catch (error) {}
    }
  }

  async function searchHandler(searchQuery) {
    if (searchQuery != null) {
      searchQuery = searchQuery.replace(" ", "+");
      try {
        const response = await api.get(`/${mediaType}/search/${searchQuery}`);
        if (response.status === 200) {
          setSearchResponse(response);
          navigate("/search");
        } else {
          console.error("Search failed with status:", response.status);
        }
      } catch (err) {
        console.log("Search error:", err);
      }
    }
  }

  function handleToggle() {
    setMediaType((prev) => (prev === "movie" ? "tv" : "movie"));
  }

  const getRecommendation = async () => {
    setRecommendation(null);
    if (user) {
      await api
        .get(`/user/recommendation?page=${currentPage}`)
        .then((res) => {
          setRecommendation((prev) => {
            if (!prev || currentPage === 1) return res.data;
            const existingIds = new Set(prev.map((item) => item.id));
            const filteredNewItems = res.data.filter(
              (item) =>
                !existingIds.has(item.id) && !watchedlistIdSet.has(item),
            );
            return [...prev, ...filteredNewItems];
          });
        })
        .catch((err) => console.log(err));
    }
  };

  useEffect(() => {
    getRecommendation();
  }, [currentPage]);

  const login = (username) => setUser(username);

  const logOut = async () => {
    try {
      await api.post("/logout", {}).then((res) => setLogoutResult(res.data));
      sessionStorage.clear();
      setUser(null);
    } catch (err) {
      console.log("Error during logout: ", err);
    }
  };

  const handleList = async (mediaType, movieId, actionType, listType) => {
    if (movieId && actionType) {
      try {
        const res = await api.get(
          `/user/list/${mediaType}/${listType}/${movieId}/${actionType}`,
        );
        if (res.status === 200) {
          await getWatchList();
        }
      } catch (err) {
        console.error("Backend error:", err);
      }
    }
  };

  const getWatchList = async () => {
    if (user) {
      await api
        .get("/user/lists")
        .then((res) => {
          setWatchlist(new Set([...res.data.watchlist]));
          setWatchedlist(new Set(res.data.watchedlist));
          setLovedlist(new Set(res.data.lovedlist));
          setWatchlistIdSet(new Set(...[res.data.watchlistIdSet]));
          setWatchedlistIdSet(new Set(res.data.watchedlistIdSet));
          setLovedlistIdSet(new Set(res.data.lovedlistIdSet));
        })
        .catch((err) => console.log(err));
    }
  };

  return (
    <UserContext.Provider
      value={{
        user,
        login,
        loading,
        logOut,
        watchlist,
        watchedlist,
        lovedlist,
        lovedlistIdSet,
        getWatchList,
        watchlistIdSet,
        watchedlistIdSet,
        searchHandler,
        getRecommendation,
        searchResponse,
        handleUpload,
        profilePictureUrl,
        getProfilePhoto,
        storedPhoto,
        recommendation,
        handleList,
        navigateToDetails,
        cast,
        detail,
        mediaType,
        setMediaType,
        handleToggle,
        filters,
        setFilters,
        movieData,
        isFetching,
        currentPage,
        setCurrentPage,
      }}
    >
      {children}
    </UserContext.Provider>
  );
};

import { createContext, useEffect, useState, useContext } from "react";
import { useNavigate } from "react-router";
import axios from "axios";

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
  const [profilePictureUrl, setProfilePictureUrl] = useState(null); //local url for response blob
  const [user, setUser] = useState(null);
  const [userPhoto, setUserPhoto] = useState(null);
  const [logoutResult, setLogoutResult] = useState(null);
  const [searchResponse, setSearchResponse] = useState(null);
  const [detail, setDetail] = useState(null);
  const [cast, setCast] = useState([]);
  const [movieData, setMovieData] = useState(null);
  const [isFetching, setIsFetching] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [person, setPerson] = useState();
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

  // const fetchData = async () => {
  //   if (isFetching) return;
  //   setIsFetching(true);
  //   try {
  //     const url = `http://localhost:8080/list`;

  //     try {
  //       const res = await axios.get(url, { withCredentials: true });
  //       setByPass(true);
  //     } catch (error) {
  //       return;
  //     }

  //     setMovieData((prev) => {
  //       if (!prev || currentPage == 1) return res.data;
  //       const existingIds = new Set(prev.map((item) => item.id));
  //       const filteredNewItems = res.data.filter(
  //         (item) => !existingIds.has(item.id)
  //       );
  //       return [...prev, ...filteredNewItems];
  //     });
  //   } catch (err) {
  //     console.error("Backend error:", err);
  //   } finally {
  //     setIsFetching(false);
  //   }
  // };

  // useEffect(() => {
  //   fetchData();
  // }, [currentPage, mediaType, filters]);

  const navigateToDetails = (media) => {
    axios
      .get(`http://localhost:8080/${mediaType}/${media.id}/credits`, {
        withCredentials: true,
      })
      .then((res) => {
        setCast(res.data);
      })
      .catch((err) => {
        console.log("Error: " + err);
      });
    setDetail(media);
    navigate("/details");
  };

  const fetchUser = () => {
    setLoading(true);
    axios
      .get("http://localhost:8080/api/me", { withCredentials: true })
      .then((res) => {
        login(res.data);
        getProfilePhoto();
        getWatchList();
      })
      .catch((err) => {
        console.log("Error: " + err);
        setUser(null);
      })
      .finally(() => {
        setLoading(false);
      });
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
      console.log("file not null");
      const formData = new FormData();
      formData.append("profilePicture", file);

      const response = await axios
        .post("http://localhost:8080/user/upload", formData, {
          headers: {
            "content-type": "multipart/form-data",
          },
          withCredentials: true,
        })
        .catch((err) => console.log("Error: " + err));
      getProfilePhoto();
    }
  }

  async function getProfilePhoto() {
    if (user) {
      try {
        const response = await axios.get(`http://localhost:8080/user/photo`, {
          responseType: "blob",
          withCredentials: true,
        });
        if (response.status === 200) {
          const imageObjectUrl = URL.createObjectURL(res.data);
          setProfilePictureUrl(imageObjectUrl);

          const reader = new FileReader();
          reader.onloadend = () => {
            const base64data = reader.result;
            sessionStorage.setItem("profilePhoto", base64data);
          };
          reader.readAsDataURL(res.data);
        }
      } catch (error) {}
    }
  }

  async function searchHandler(searchQuery) {
    if (searchQuery != null) {
      searchQuery = searchQuery.replace(" ", "+");

      try {
        const response = await axios.get(
          "http://localhost:8080/" + mediaType + "/search/" + searchQuery,
          {
            withCredentials: true,
          }
        );

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
    if (mediaType == "movie") {
      setMediaType("tv");
    } else if (mediaType == "tv") {
      setMediaType("movie");
    } else {
      setMediaType("movie");
    }
  }

  const getRecommendation = async () => {
    setRecommendation(null);
    if (user) {
      await axios
        .get("http://localhost:8080/user/recommendation?page=" + currentPage, {
          withCredentials: true,
        })
        .then((res) => {
          setRecommendation((prev) => {
            if (!prev || currentPage == 1) return res.data;
            const existingIds = new Set(prev.map((item) => item.id));
            const filteredNewItems = res.data.filter(
              (item) => !existingIds.has(item.id) && !watchedlistIdSet.has(item)
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

  const login = (username) => {
    setUser(username);
  };

  const logOut = async () => {
    console.log("Log out requested");
    const url = "http://localhost:8080/logout";
    try {
      await axios
        .post(
          url,
          {},
          {
            withCredentials: true,
          }
        )
        .then((res) => setLogoutResult(res.data));

      console.log("Logged out");
      sessionStorage.clear();
      setUser(null);
    } catch (err) {
      console.log("Error during logout: ", err);
    }
  };

  const handleList = async (mediaType, movieId, actionType, listType) => {
    // console.log("watchlist context called");

    if (movieId && actionType) {
      try {
        const res = await axios.get(
          `http://localhost:8080/user/list/${mediaType}/${listType}/${movieId}/${actionType}`,
          { withCredentials: true }
        );
        if (res.status === 200) {
          await getWatchList(); //backend fetch update
        }
      } catch (err) {
        console.error("Backend error:", err);
      }
    }
  };

  // helper for list updaTE
  function updateList(setter, setIdSetter, currentSet, idSet, id, action) {
    const newSet = new Set([...currentSet]);
    const newIdSet = new Set([...idSet]);
    if (action === "del") {
      newSet.delete(id);
      newIdSet.delete(id);
    } else {
      newSet.add(id);
      newIdSet.add(id);
    }
    setter(newSet);
    setIdSetter(newIdSet);
  }
  const getWatchList = async () => {
    if (user) {
      await axios
        .get("http://localhost:8080/user/lists", {
          withCredentials: true,
        })
        .then((res) => {
          setWatchlist(new Set([...res.data.watchlist]));
          setWatchedlist(new Set(res.data.watchedlist));
          setLovedlist(new Set(res.data.lovedlist));
          // getRecommendation();
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
        // fetchData,
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

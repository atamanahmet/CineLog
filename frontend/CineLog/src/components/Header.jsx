import movieLogo from "/movie.png";
import profile from "../assets/profile.png";
import { useState } from "react";
import { useUser } from "../context/UserContext";
import { useNavigate } from "react-router-dom";

export default function Header({ showHeader }) {
  const navigate = useNavigate();

  const {
    user,
    logOut,
    getWatchList,
    searchHandler,
    profilePictureUrl,
    storedPhoto,
    getRecommendation,
  } = useUser();

  const [searchQuery, setSearchQuery] = useState("");

  const handleChange = (e) => {
    setSearchQuery(e.target.value);
  };

  const navigateRegister = () => navigate("/register");
  const navigateLogin = () => navigate("/login");
  const navigateProfile = () => {
    getWatchList();
    getRecommendation();
    navigate("/profile");
  };
  const handleLogOut = () => logOut();

  return (
    <nav
      className={`bg-amber-600 dark:bg-amber-600 fixed top-0 left-0 w-full z-50 transition-transform duration-300 ${
        showHeader ? "translate-y-0" : "-translate-y-full"
      }`}
    >
      <div className="max-w-screen-xxl flex flex-wrap items-center justify-between p-3 header">
        <a href="/" className="flex items-center space-x-3 rtl:space-x-reverse">
          <img src={movieLogo} className="h-8" alt="Logo" />
          <span className="self-center text-2xl font-semibold whitespace-nowrap dark:text-white">
            PerMovDb
          </span>
        </a>

        <div className="flex md:order-2 gap-3">
          <a href="/top">
            <button className="bg-amber-900 text-amber-50 rounded-lg text-sm py-2 px-4 me-1 top-buttons">
              Top
            </button>
          </a>
          <a href="/new">
            <button className="bg-amber-900 text-amber-50 rounded-lg text-sm py-2 px-4 me-1 top-buttons">
              New+
            </button>
          </a>

          <div className="relative hidden md:block">
            <div className="absolute inset-y-0 start-0 flex items-center ps-3 pointer-events-none">
              <svg
                className="w-4 h-4 text-amber-500 dark:text-amber-600"
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 20 20"
              >
                <path
                  stroke="currentColor"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="m19 19-4-4m0-7A7 7 0 1 1 1 8a7 7 0 0 1 14 0Z"
                />
              </svg>
            </div>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                searchHandler(searchQuery);
              }}
            >
              <input
                type="text"
                className="block w-full p-2 ps-10 text-sm text-amber-900 border border-amber-300 rounded-lg bg-amber-50 focus:ring-orange-500 focus:border-orange-500 dark:bg-amber-800 dark:border-amber-600 dark:placeholder-amber-400 dark:text-white dark:focus:ring-orange-500 dark:focus:border-orange-500"
                placeholder="Search..."
                value={searchQuery}
                onChange={handleChange}
              />
            </form>
          </div>

          <div className="ml-1 smallPX flex gap-3">
            {user && (
              <img
                className="w-10 h-10 p-1 rounded-full ring-2 ring-gray-300 dark:ring-gray-500 cursor-pointer"
                src={storedPhoto ? storedPhoto : profile}
                alt="avatar"
                onClick={navigateProfile}
              />
            )}
            {!user && (
              <>
                <button
                  className="bg-amber-900 text-amber-50 rounded-lg text-sm py-2 px-4 me-1 top-buttons"
                  onClick={navigateRegister}
                >
                  Register
                </button>
                <button
                  className="bg-amber-900 text-amber-50 rounded-lg text-sm py-2 px-4 me-1 top-buttons"
                  onClick={navigateLogin}
                >
                  Login
                </button>
              </>
            )}
            {user && (
              <button
                className="bg-amber-900 text-amber-50 rounded-lg text-sm py-2 px-4 me-1 top-buttons"
                onClick={handleLogOut}
              >
                Log Out
              </button>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}

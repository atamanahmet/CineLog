import { Routes, Route, Navigate, useParams } from "react-router";
import SearchPage from "./pages/SearchPage";

import "./App.css";

import DiscoverPage from "./pages/DiscoverPage";

import Layout from "./components/Layout";
import ScrollToTop from "./components/ScrollToTop";
import FilterLayout from "./components/FilterLayout";
import AuthModal from "./components/AuthModal";
import ProfilePage from "./pages/ProfilePage";
import SettingsPage from "./pages/SettingsPage";
import NewReleasesPage from "./pages/NewReleasesPage";
import TopPage from "./pages/TopPage";
import UpcomingPage from "./pages/UpcomingPage";
import DetailsPage from "./pages/DetailsPage";
import ActorPage from "./pages/ActorPage";
import useThemeClass from "./hooks/useThemeClass";

/**
 * Old /actor/:id bookmarks → /person/:id.
 */
function ActorLegacyRedirect() {
  const { id } = useParams();
  return <Navigate to={`/person/${id}`} replace />;
}

function App() {
  useThemeClass();

  return (
    <>
      <ScrollToTop />
      <Routes>
        <Route element={<Layout />}>
          <Route element={<FilterLayout />}>
            <Route path="/" element={<DiscoverPage />} />
            <Route path="/new" element={<NewReleasesPage />} />
            <Route path="/upcoming" element={<UpcomingPage />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="/top" element={<TopPage />} />
          </Route>
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/details/:mediaType/:id" element={<DetailsPage />} />
          <Route path="/person/:id" element={<ActorPage />} />
          <Route path="/actor/:id" element={<ActorLegacyRedirect />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/upload" element={<Navigate to="/settings#profile" replace />} />
        </Route>
      </Routes>
      <AuthModal />
    </>
  );
}

export default App;

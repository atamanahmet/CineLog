import api from "../../../api/axiosInstance";

export const fetchAccount = async () => (await api.get("/user/account")).data;

export const updateEmail = async ({ email, currentPassword }) =>
  (await api.put("/user/account/email", { email, currentPassword })).data;

export const updatePassword = async ({ currentPassword, newPassword }) => {
  await api.put("/user/account/password", { currentPassword, newPassword });
};

export const fetchRecommendationSettings = async () =>
  (await api.get("/user/account/recommendation-settings")).data;

export const updateRecommendationSettings = async (settings) =>
  (await api.put("/user/account/recommendation-settings", settings)).data;

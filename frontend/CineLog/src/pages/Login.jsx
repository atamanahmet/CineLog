import { React, useState } from "react";
import axios from "axios";
// import collage from "../assets/collage.jpg";
import { useNavigate } from "react-router-dom";
import { useUser } from "../context/UserContext";

export default function Login() {
  const navigate = useNavigate();
  const { login } = useUser();
  const [stateInfo, setStateInfo] = useState("Cmon");
  const [formData, setFormData] = useState({
    username: "",
    password: "",
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: value,
    }));
  };

  const handleSubmit = async () => {
    console.log("login requested");
    // console.log("username:" + formData.username);
    // console.log("password:" + formData.password);

    try {
      const apiResponse = await axios.post(
        "http://localhost:8080/login",
        {
          username: formData.username,
          password: formData.password,
        },
        { withCredentials: true }
      );
      if (apiResponse.status == 200) {
        setStateInfo("Logged-in succesfully. Redirecting...");
        login(apiResponse.data);
        setTimeout(function () {
          navigate("/");
        }, 2000);
      }
    } catch (err) {
      console.log("Error :" + err);
      setStateInfo("Username or password not matching.");
    }
  };

  return (
    <>
      <section className="mt-10">
        <div className="flex flex-col my-5 px-6 py-8 md:h-screen lg:py-0 login-form">
          <a
            href="#"
            className="flex items-center mb-6 text-2xl font-semibold text-amber-50 dark:text-amber-700"
          >
            {stateInfo}
          </a>
          <div className="w-full rounded-lg shadow dark:border md:mt-0 sm:max-w-md xl:p-0 dark:bg-amber-700 dark:border-amber-700">
            <div className="p-6 space-y-4 md:space-y-6 sm:p-8">
              <h1 className="text-xl font-bold leading-tight tracking-tight text-amber-50 md:text-2xl dark:text-amber-50">
                Log-in to your accout
              </h1>
              <form
                className="space-y-4 md:space-y-6"
                onSubmit={(e) => {
                  e.preventDefault();
                  handleSubmit();
                }}
              >
                <div>
                  <label
                    htmlFor="username"
                    className="block mb-2 text-sm font-medium text-amber-50 dark:text-amber-50"
                  >
                    Your username
                  </label>
                  <input
                    type="text"
                    name="username"
                    id="username"
                    className="bg-amber-50 border border-amber-300 text-amber-900 text-sm rounded-lg focus:ring-primary-600 focus:border-primary-600 block w-full p-2.5 dark:bg-amber-50 dark:border-amber-600 dark:placeholder-amber-700 dark:text-amber-900 dark:focus:ring-amber-700 dark:focus:border-amber-700"
                    placeholder="PapilioFerox"
                    value={formData.username}
                    onChange={handleChange}
                    required
                  />
                </div>
                <div>
                  <label
                    htmlFor="password"
                    className="block mb-2 text-sm font-medium text-amber-50 dark:text-amber-50"
                  >
                    Password
                  </label>
                  <input
                    type="password"
                    name="password"
                    id="password"
                    placeholder="••••••••"
                    className="bg-amber-50 border border-amber-300 text-amber-50 text-sm rounded-lg focus:ring-primary-600 focus:border-primary-600 block w-full p-2.5 dark:bg-amber-50 dark:border-amber-600 dark:placeholder-amber-700 dark:text-amber-700 dark:focus:ring-amber-700 dark:focus:border-amber-700"
                    value={formData.password}
                    onChange={handleChange}
                    required
                  />
                </div>

                <div className="flex items-start">
                  <div className="flex items-center h-5">
                    <input
                      id="remember"
                      name="remember"
                      aria-describedby="remember"
                      type="checkbox"
                      className="w-4 h-4 border border-amber-300 rounded bg-amber-50 focus:ring-3 focus:ring-primary-300 dark:bg-amber-700 dark:border-amber-600 dark:focus:ring-primary-600 dark:ring-offset-amber-700"
                      required
                    />
                  </div>
                  <div className="ml-3 text-sm">
                    <label
                      htmlFor="remember"
                      className="text-amber-50 dark:text-amber-50"
                    >
                      Remember me!{null}
                    </label>
                  </div>
                </div>
                <button
                  type="submit"
                  className="w-full text-amber-50 bg-amber-600 hover:bg-primary-700 focus:ring-4 focus:outline-none focus:ring-primary-300 font-medium rounded-lg text-sm px-5 py-2.5 text-center dark:bg-primary-600 dark:hover:bg-primary-700 dark:focus:ring-primary-800"
                  id="submit"
                >
                  Log-in
                </button>
                <p className="text-sm font-light text-amber-50 dark:text-amber-50">
                  Forget password?{null}
                  <a
                    href="/login"
                    className="font-medium text-primary-600 hover:underline dark:text-amber-300 ml-1"
                  >
                    Reset here
                  </a>
                </p>
              </form>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}

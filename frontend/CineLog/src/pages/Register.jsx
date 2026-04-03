import Header from "../components/Header";
import profile from "../assets/profile.png";
import { React, useState } from "react";
import axios from "axios";
import collage from "../assets/collage.jpg";
import { useNavigate } from "react-router-dom";

export default function Register() {
  const navigate = useNavigate();
  const [response, setResponse] = useState("Register");
  const [file, setFile] = useState(null);

  const [formData, setFormData] = useState({
    username: "",
    password: "",
    confirmPassword: "",
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: value,
    }));
  };
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (formData.password !== formData.confirmPassword) {
      alert("Passwords do not match!");
      return;
    }
    // const data = new FormData();
    // data.append("username", formData.username);
    // data.append("password", formData.password);
    // if (file) {
    //   data.append("file", file);
    // }

    try {
      const response = await axios.post(
        "http://localhost:8080/register",
        { username: formData.username, password: formData.password },
        { withCredentials: true }
      );

      if (response.status === 201) {
        setResponse("Account created succesfully. Redirecting...");
        setTimeout(function () {
          //do what you need here
          navigate("/login");
        }, 2000);
      }
    } catch (err) {
      console.log(err);
      setResponse("Got some issue. Try again");
    }
  };

  return (
    <>
      <div className="flex flex-row justify-center register">
        <section className="mt-10 min-w-220 register-parent">
          <a
            href="#"
            className="flex justify-center mb-6 text-2xl font-semibold text-amber-50 dark:text-amber-700"
          >
            {response}
          </a>
          <div className="flex flex-row gap-20 justify-center">
            <div className="register-form rounded-lg shadow dark:border md:mt-0  xl:p-0 dark:bg-amber-700 dark:border-amber-700">
              <div className="p-6  md:space-y-6 sm:p-8">
                <h1 className="text-xl font-bold leading-tight tracking-tight text-amber-50 md:text-2xl dark:text-amber-50">
                  Create an account
                </h1>
                <form
                  className="space-y-4 md:space-y-6"
                  onSubmit={handleSubmit}
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
                  <div>
                    <label
                      htmlFor="confirmPassword"
                      className="block mb-2 text-sm font-medium text-amber-50 dark:text-amber-50"
                    >
                      Confirm password
                    </label>
                    <input
                      type="password"
                      name="confirmPassword"
                      id="confirmPassword"
                      placeholder="••••••••"
                      className="bg-amber-50 border border-amber-300 text-amber-700 text-sm rounded-lg focus:ring-primary-600 focus:border-primary-600 block w-full p-2.5 dark:bg-amber-50 dark:border-amber-600 dark:placeholder-amber-700 dark:text-amber-900 dark:focus:ring-amber-700 dark:focus:border-amber-700"
                      value={formData.confirmPassword}
                      onChange={handleChange}
                      required
                    />
                  </div>
                  <div className="flex items-start">
                    <div className="flex items-center h-5">
                      <input
                        id="terms"
                        name="terms"
                        aria-describedby="terms"
                        type="checkbox"
                        className="w-4 h-4 border border-amber-300 rounded bg-amber-50 focus:ring-3 focus:ring-primary-300 dark:bg-amber-700 dark:border-amber-600 dark:focus:ring-primary-600 dark:ring-offset-amber-700"
                        required
                      />
                    </div>
                    <div className="ml-3 text-sm">
                      <label
                        htmlFor="terms"
                        className="font-light text-amber-50 dark:text-amber-50"
                      >
                        I accept the{null}
                        <a
                          className="font-medium text-primary-600 hover:underline dark:text-amber-300 ml-1"
                          href="/terms-and-conditions"
                        >
                          Terms and Conditions
                        </a>
                      </label>
                    </div>
                  </div>
                  <button
                    type="submit"
                    className="w-full text-amber-50 bg-amber-600 hover:bg-primary-700 focus:ring-4 focus:outline-none focus:ring-primary-300 font-medium rounded-lg text-sm px-5 py-2.5 text-center dark:bg-primary-600 dark:hover:bg-primary-700 dark:focus:ring-primary-800"
                    id="submit"
                  >
                    Create an account
                  </button>
                  <p className="text-sm font-light text-amber-50 dark:text-amber-50">
                    Already have an account?{null}
                    <a
                      href="/login"
                      className="font-medium text-primary-600 hover:underline dark:text-amber-300 ml-1"
                    >
                      Login here
                    </a>
                  </p>
                </form>
              </div>
            </div>
            {/* <div className="flex flex-col items-center">
              <label
                htmlFor="profilePicture"
                className="mb-2 text-sm font-medium flex justify-center w-5/4  text-amber-50 dark:text-amber-50"
              >
                <img src={file} alt="" className="rounded-full w-2/6" />
              </label>
              <input
                type="file"
                name="profilePicture"
                id="profilePicture"
                onChange={(e) => setFile(e.target.files[0])}
                className="block bg-amber-700 w-full text-sm text-amber-800 border border-amber-300 rounded-lg cursor-pointer  dark:text-amber-100 focus:outline-none upload-area"
                accept="image/*"
              />
            </div> */}
          </div>
        </section>
      </div>

      {/* <img src={collage} alt="" />  */}
    </>
  );
}

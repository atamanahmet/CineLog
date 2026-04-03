import axios from "axios";
import { useNavigate } from "react-router";
import { useUser } from "../context/UserContext";
import profilePlaceholder from "../assets/profile.png";
import { useState } from "react";

export default function Upload() {
  const [file, setFile] = useState(null);
  //   const [response, setResponse] = useState(null);

  const { user, handleUpload } = useUser();

  //   async function handleUpload(file) {
  //     console.log(file);
  //     if (file != null) {
  //       console.log("file not null");
  //       const formData = new FormData();
  //       formData.append("profilePicture", file);

  //       const response = async () => {
  //         await axios
  //           .post("http://localhost:8080/user/photo", formData, {
  //             headers: {
  //               "content-type": "multipart/form-data",
  //             },
  //             withCredentials: true,
  //           })
  //           .then((res) => setResponse(res))
  //           .catch((err) => console.log("Error: " + err));
  //       };
  //       response();

  //       console.log(response.data);

  //       if (response && response.status == 200) {
  //         console.log("Upload successful");
  //         useNavigate("/profile");
  //       }
  //     }
  //   }

  return (
    <>
      <div className="flex flex-col items-center">
        <label
          htmlFor="profilePicture"
          className="mb-2 text-sm font-medium flex justify-center   text-amber-50 dark:text-amber-50"
        >
          <img
            src={file ? file : profilePlaceholder}
            alt=""
            className="rounded-full w-2/6"
          />
        </label>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            console.log("submitted");
            handleUpload(file);
          }}
        >
          <input
            type="file"
            name="profilePicture"
            id="profilePicture"
            onChange={(e) => setFile(e.target.files[0])}
            className="block bg-amber-700 w-full text-sm text-amber-800 border border-amber-300 rounded-lg cursor-pointer  dark:text-amber-100 focus:outline-none upload-area"
            accept="image/*"
          />
          <button
            className="bg-amber-900 text-amber-50  rounded-lg text-sm py-2 px-4 me-1 top-buttons"
            type="submit"
          >
            Upload
          </button>
        </form>
      </div>
    </>
  );
}

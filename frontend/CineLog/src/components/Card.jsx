import RatingCircle from "./RatingCircle";
import missing from "../assets/missing.png";
import { Tooltip } from "@mui/material";
import { useUser } from "../context/UserContext";
function Card({ item }) {
  const { mediaType } = useUser();

  const imageUrl =
    item.poster_path?.endsWith("null") || !item.poster_path
      ? missing
      : item.poster_path;

  let release_year = "Unknown";
  const dateStr = item.release_date || item.first_air_date;

  if (dateStr) {
    const d = new Date(dateStr);
    if (!isNaN(d.getTime())) {
      release_year = d.getFullYear();
    }
  }
  const isValid = dateStr && !isNaN(new Date(dateStr));

  return (
    <>
      {item && (
        <div className="card flex flex-col justify-between">
          <Tooltip
            title={
              <>
                <span className="font-bold">Title: </span>
                {item.original_title}
                <br />
                <span className="font-bold"> Release Date: </span>
                {item.release_date}
                <br />
                <span className="font-bold"> Original Language: </span>
                {item.original_language}
              </>
            }
            placement="right-start"
          >
            <img src={imageUrl} alt="" className="poster" />
          </Tooltip>
          <div className="mb-9">
            <div className="info flex flex-row justify-between items-center  mx-3 my-3">
              <div className="">
                <p className="title text-sm">
                  {item.original_language == "en"
                    ? item.original_title
                    : item.title}
                </p>
                <p className="text-xs text-amber-500">
                  {isValid
                    ? new Date(dateStr).toISOString().slice(0, 10)
                    : "Unknown"}
                </p>
              </div>

              <div className="text-shadow-emerald-50">
                <RatingCircle percentage={item.vote_average?.toFixed(1) || 0} />
              </div>
            </div>
          </div>
        </div>
      )}
      {!item && (
        <>
          <p>Loading..</p>
        </>
      )}
    </>
  );
}
export default Card;

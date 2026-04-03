import CardPlate from "./CardPlate";
import { useUser } from "../context/UserContext";

export default function Search() {
  const { searchResponse } = useUser();
  if (searchResponse == null) {
    return (
      <>
        <div className="text-center mt-10">
          <h2>Searching..</h2>
        </div>
      </>
    );
  }
  return (
    <>
      <h2 className="text-center p-7 text-amber-100 font-bold text-4xl page-title">
        Search results
      </h2>
      <hr className="opacity-20 text-amber-700 horiz mb-11" />
      <main className=" my-10 flex flex-row flex-wrap gap-5 flex-8/12 justify-center discoverPage">
        <CardPlate data={searchResponse.data} message={"Searching.."} />
      </main>
    </>
  );
}

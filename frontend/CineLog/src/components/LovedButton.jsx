// import { useUser } from "../context/UserContext";
// import emptyHeart from "../assets/emptyHeart.png";
// import solidHeart from "../assets/solidHeart.png";

// export default function LovedButton({ item }) {
//   const { handleList, lovedlistIdSet } = useUser();

//   const isInLovedlist = lovedlistIdSet.has(item.id);
//   console.log(item.title + ": " + isInLovedlist);
//   const buttonType = "lovedlist";
//   return (
//     <>
//       <button
//         className=" h-7 w-7 text-amber-100 bg-amber-200 rounded z-0 addButton absolute ml-19"
//         onClick={() => {
//           handleList(item.id, isInWatchedlist ? "del" : "add", buttonType);
//         }}
//       >
//         {isInLovedlist ? (
//           <span className="text-amber-100 add size-4 ">
//             <img
//               src={solidHeart}
//               alt=""
//               className=" closedEye max-w-10/12 scale-90 "
//             />
//           </span>
//         ) : (
//           <span className="text-amber-100 add size-6">
//             <img
//               src={emptyHeart}
//               alt=""
//               className="closedEye max-w-10/12  scale-90"
//             />
//           </span>
//         )}
//       </button>

//       <button
//         className=" h-7 w-7 text-amber-100 bg-amber-200 rounded z-0 addButton absolute ml-10"
//         onClick={() => {
//           handleList(item.id, isInWatchedlist ? "del" : "add", buttonType);
//         }}
//       >
//         {isInWatchedlist ? (
//           <span className="text-amber-100 add size-5 opacity-80">
//             <img src={closedEye} alt="" className=" closedEye max-w-10/12" />
//           </span>
//         ) : (
//           <span className="text-amber-100 add size-6 ">
//             <img src={openedEye} alt="" className="closedEye max-w-10/12" />
//           </span>
//         )}
//       </button>
//       {/* Watchlist Button */}
//     </>
//   );
// }

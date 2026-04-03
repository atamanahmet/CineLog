import { Navigate } from "react-router";
import { useUser } from "../context/UserContext";
import { useNavigate } from "react-router";
import Card from "./Card";
import ListButton from "./ListButton";
import { Tooltip } from "@mui/material";
import { useState } from "react";
import ToogleSwitch from "./ToggleSwitch";

export default function TopSelection({ data, addOrRemove, message }) {
  const { user, navigateToDetails } = useUser();

  const style = {
    watchlist:
      "absolute h-7 w-7 text-amber-100 bg-amber-200 rounded left-1 z-0 addButton",
    watchedlist:
      " h-7 w-7 text-amber-100 bg-amber-200 rounded z-0 addButton absolute ml-10",
    lovedlist:
      " h-7 w-7 text-amber-100 bg-amber-200 rounded z-0 addButton absolute ml-19",
  };

  const navigate = useNavigate();

  function onCardClick(item) {
    navigateToDetails(item, item.first_air.date != null); //tv or movie
  }

  if (data == null) {
    return (
      <>
        <div className="text-center">
          <h2>{message}</h2>
        </div>
      </>
    );
  }

  return (
    <>
      {Array.from(data).map((item) => (
        <div key={item.id} className="relative mb-8">
          {user && <ListButton item={item} style={style} />}
          <div
            onClick={() => onCardClick(item)}
            className="z-1 relative text-left"
          >
            <Card item={item} />
          </div>
        </div>
      ))}
    </>
  );
}

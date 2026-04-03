import React from "react";

const RatingCircle = ({
  percentage = 75,
  size = 40,
  stroke = 4,
  color = percentage <= 5
    ? "rgb(205, 0, 0)"
    : percentage < 6
    ? "#ecec00"
    : percentage < 8
    ? "#b7ec00"
    : "#36ec00",
  bg = "#eee",
}) => {
  const radius = (size - stroke) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (percentage / 10) * circumference;

  return (
    <svg width={size} height={size}>
      <circle
        cx={size / 2}
        cy={size / 2}
        r={radius}
        stroke={bg}
        strokeWidth={stroke}
        fill="none"
      />
      <circle
        cx={size / 2}
        cy={size / 2}
        r={radius}
        stroke={color}
        strokeWidth={stroke}
        fill="black"
        strokeDasharray={circumference}
        strokeDashoffset={offset}
        strokeLinecap="round"
        transform={`rotate(-90 ${size / 2} ${size / 2})`}
      />
      <text
        x="49%"
        y="52.4%"
        dominantBaseline="middle"
        textAnchor="middle"
        fontSize="1em"
        fontFamily="sans-serif"
        fill="white"
      >
        {percentage}
      </text>
    </svg>
  );
};

export default RatingCircle;

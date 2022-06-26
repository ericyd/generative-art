import { svg, tag } from "../lib/tag.js";
import { array } from "../lib/util.js";
import { randomPoint, smoothBezier, move } from "../lib/path.js";

export function draw() {
  const root = svg({
    fill: "#000000",
    stroke: "#ffffff",
    style: "background: #000000",
    height: "100%",
    width: "100%",
  });

  const path = tag("path", { fill: "none", "stroke-width": "0.1" });

  const colors = ["#fff", "#faf", "#fb4", "#4ff"];

  const children = array(100).map((i) =>
    path.withAttrs({ d: d(), stroke: colors[i % 4] })
  );

  return root.withChildren(children).draw();
}

function d() {
  const start = randomPoint(0, 100, 0, 100)
  return [
    move(start).toString(),
    smoothBezier(randomPoint(20, 80, 20, 80), randomPoint(20, 80, 20, 80)).toString(),
    smoothBezier(randomPoint(20, 80, 20, 80), randomPoint(20, 80, 20, 80)).toString(),
    smoothBezier(randomPoint(20, 80, 20, 80), randomPoint(20, 80, 20, 80)).toString(),
    smoothBezier(randomPoint(20, 80, 20, 80), randomPoint(20, 80, 20, 80)).toString(),
    smoothBezier(randomPoint(20, 80, 20, 80), randomPoint(20, 80, 20, 80)).toString(),
    smoothBezier(randomPoint(20, 80, 20, 80), randomPoint(20, 80, 20, 80)).toString(),
    smoothBezier(randomPoint(20, 80, 20, 80), randomPoint(20, 80, 20, 80)).toString(),
  ].join(' ')
}
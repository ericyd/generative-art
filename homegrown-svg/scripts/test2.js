import { svg, tag } from "../lib/tag.js";
import { array } from "../lib/util.js";
import { pathBuilder, randomPoint } from "../lib/path.js";
import { rngFactory } from "../lib/random.js";

export function draw() {
  const root = svg({
    fill: "#000000",
    stroke: "#fffffff",
    style: "background: #000000",
    height: "100%",
    width: "100%",
  });

  const path = tag("path", { fill: "none", "stroke-width": "0.1" });

  const colors = ["#fff", "#faf", "#fb4", "#4ff"];

  const seed = Date.now().toString();
  console.log({ seed });
  window.location.hash = seed;
  const rng = rngFactory(seed);

  const children = array(10).map((i) =>
    path.withAttrs({
      d: pathBuilder((d) => {
        d.move(randomPoint(20, 80, 20, 80, rng));
        let i = 0;
        while (i++ < 50) {
          if (i % 15 === 0) {
            d.smoothBezier(
              randomPoint(20, 80, 20, 80, rng),
              randomPoint(20, 80, 20, 80, rng),
              "absolute"
            );
          } else if (i % 3 === 0) {
            d.smoothBezier(
              randomPoint(-5, 5, -5, 5, rng),
              randomPoint(-5, 5, -5, 5, rng),
              "relative"
            );
          } else {
            d.smoothBezier(
              randomPoint(-3, 3, -3, 3, rng),
              randomPoint(-3, 3, -3, 3, rng),
              "relative"
            );
          }
        }
      }),
      stroke: colors[i % 4],
    })
  );

  return root.withChildren(children).draw();
}

import { svg, tag } from "../lib/tag.js";
import { array } from "../lib/util.js";
import { pathBuilder, randomPoint } from "../lib/path.js";
import { rngFactory } from "../lib/random.js";
import { writeFileSync } from 'fs'

export function draw() {
  const root = svg({
    fill: "#000000",
    stroke: "#ffffff",
    style: "background: #000000",
    height: "2000px",
    width: "2000px",
  });

  const path = tag("path", { fill: "none", "stroke-width": "1.1" });
  const layer = tag("g", { style: "filter: drop-shadow(1px 1px 0.5px #222);" })

  const colors = ["#fff", "#faf", "#fb4", "#4ff"];

  const seed = Date.now().toString();
  console.log({ seed });
  try {
    window.location.hash = seed;
  } catch (e) {
    // window is probably not defined, no biggie
  }
  const rng = rngFactory(seed);

  const children = array(10).map((i) =>
  layer.withChildren([
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
      }),
    ])
  );

  return root.withChildren(children).draw();
}

export function write() {
  writeFileSync(`output-${Date.now()}.svg`, draw())
}

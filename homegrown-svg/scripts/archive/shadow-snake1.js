import { svg, tag } from "../lib/tag.js";
import { array } from "../lib/util.js";
import { pathBuilder, randomPoint } from "../lib/path.js";
import { rngFactory } from "../lib/random.js";

export function draw() {
  const w = 100;
  const h = 100;
  const root = svg({
    fill: "#ffffff",
    stroke: "#ffffff",
    style: "background: #000000",
    height: '100%',
    width: '100%',
    viewBox: `0 0 ${w} ${h}`,
    // height: "2000px",
    // width: "2000px",
  });

  const seed = Date.now().toString();
  console.log({ seed });
  try {
    window.location.hash = seed;
  } catch (e) {
    // window is probably not defined, no biggie
  }
  const rng = rngFactory(seed);

  const children = [
    tag('rect', {width:w, height:h, fill: '#fff'}),
    tag('path', {
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
              randomPoint(-10, 10, -10, 10, rng),
              randomPoint(-10, 10, -10, 10, rng),
              "relative"
            );
          } else {
            d.smoothBezier(
              randomPoint(-6, 6, -6, 6, rng),
              randomPoint(-6, 6, -6, 6, rng),
              "relative"
            );
          }
        }
      }),
      stroke: '#fff',
      fill: 'none',
      'stroke-width': '3',
      style: "filter: drop-shadow(1px 1px 2.5px #222);"
    }),
  ]

  return root.withChildren(children).draw();
}


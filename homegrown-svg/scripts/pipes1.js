import { svg, tag } from "../lib/tag.js";
import { array, grid, map, range, rangeWithIndex } from "../lib/util.js";
import { point, pathBuilder, randomPoint } from "../lib/path.js";
import { random, rngFactory } from "../lib/random.js";

export function draw() {
  const w = 100;
  const h = 100;
  const root = svg({
    fill: "#000000",
    stroke: "#ffffff",
    style: "background: #000000",
    height: "100%",
    viewBox: `0 0 ${w} ${h}`,
    // height: "2000px",
    // width: "2000px",
  });

  const path = tag("path", {
    fill: "none",
    "stroke-width": "1.1",
    'stroke-linecap': 'round',
    'stroke-linejoin': 'round'
  });

  const seed = Date.now().toString();
  console.log({ seed });
  try {
    window.location.hash = seed;
  } catch (e) {
    // window is probably not defined, no biggie
  }
  const rng = rngFactory(seed);

  const children = [];

  const xMin = 0;
  const xMax = w;
  const xStep = 2;
  const yMin = 0;
  const yMax = h;
  const yStep = 2;
  const ys = range(yMin, yMax, yStep);
  let yi = 0;
  const xs = range(xMin, xMax, xStep);
  let xi = 0;
  while (yi < ys.length) {
    while (xi < xs.length) {
      if (random(0, 1, rng) < map(0, ys.length - 1, 0.0, 0.3, yi) && xi < xs.length - 1) {
        let pt = point(xs[xi], ys[yi]);
        const segment1 = path.withAttrs({
          d: pathBuilder((d) => {
            d.move(pt, "absolute");
            d.line(point(pt.x + xStep, pt.y + yStep));
          }),
        });
        pt = point(xs[xi + 1], ys[yi]);
        const segment2 = path.withAttrs({
          d: pathBuilder((d) => {
            d.move(pt, "absolute");
            d.line(point(pt.x - xStep, pt.y + yStep));
          }),
          // This odd drop-shadow is to avoid artifacts where the segment appears to "shadow itself".
          // Change the offset-x from "-1px" to "1px" to see the ugly artifacts
          style: "filter: drop-shadow(-1px 1px 0.5px #222);",
        });
        children.push(segment1, segment2);
        xi += 2;
      } else {
        const pt = point(xs[xi], ys[yi]);
        const segment = path.withAttrs({
          d: pathBuilder((d) => {
            d.move(pt, "absolute");
            d.line(point(pt.x, pt.y + yStep));
          }),
        });
        children.push(segment);
        xi++;
      }
    }
    xi = 0;
    yi++;
  }
  for (const [y, yi] of rangeWithIndex(yMin, yMax, yStep)) {
    for (const [x, xi] of rangeWithIndex(xMin, xMax, xStep)) {
    }
  }

  return root.withChildren(children).draw();
}

import { Defs, Rect, svg, tag } from "../lib/tag.js";
import { array, grid, map, observe, range, rangeWithIndex } from "../lib/util.js";
import { point, pathBuilder, randomPoint } from "../lib/path.js";
import { random, rngFactory } from "../lib/random.js";
import { ColorHex } from "../lib/color.js";
import { glowOnly } from "../lib/filters/glow-only.js";
import { vec2 } from "../lib/Vector2.js";

export function draw() {
  const w = 100;
  const h = 100;
  const root = svg(
    {
      fill: ColorHex.black,
      height: "100%",
      viewBox: `0 0 ${w} ${h}`,
      // height: "2000px",
      // width: "2000px",
    },
    [
      Defs(glowOnly),
      Rect({
        x: 0,
        y: 0,
        width: w,
        height: h,
        fill: ColorHex.black,
        stroke: ColorHex.black,
      }),
    ]
  );

  const path = tag("path", {
    fill: "none",
    "stroke-width": "1.1",
    'stroke-linecap': 'round',
    'stroke-linejoin': 'round',
    stroke: ColorHex.white
  });

  const seed = Date.now().toString();
  observe(seed)
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
        let pt = vec2(xs[xi], ys[yi]);
        const segment1 = path.withAttrs({
          d: pathBuilder((d) => {
            d.move(pt, "absolute");
            d.line(vec2(pt.x + xStep, pt.y + yStep));
          }),
        });
        pt = vec2(xs[xi + 1], ys[yi]);
        const segment2 = path.withAttrs({
          d: pathBuilder((d) => {
            d.move(pt, "absolute");
            d.line(vec2(pt.x - xStep, pt.y + yStep));
          }),
          // This odd drop-shadow is to avoid artifacts where the segment appears to "shadow itself".
          // Change the offset-x from "-1px" to "1px" to see the ugly artifacts
          style: "filter: drop-shadow(-1px 1px 0.5px #222);",
        });
        children.push(segment1, segment2);
        xi += 2;
      } else {
        const pt = vec2(xs[xi], ys[yi]);
        const segment = path.withAttrs({
          d: pathBuilder((d) => {
            d.move(pt, "absolute");
            d.line(vec2(pt.x, pt.y + yStep));
          }),
        });
        children.push(segment);
        xi++;
      }
    }
    xi = 0;
    yi++;
  }

  return root.withChildren(children).draw();
}

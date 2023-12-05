/**
 * pipes1 used segments to ensure that the piece moving to the right was always "on top"
 * pipes2 uses continuous segments and their position is ... not randomized but maybe it could be!
 */
import { Defs, svg, tag, Rect, Path, Circle } from "../lib/tag.js";
import {
  array,
  grid,
  map,
  observe,
  range,
  rangeWithIndex,
} from "../lib/util.js";
import {
  pathBuilder,
  move,
  line,
  smoothBezier,
  cubicBezier,
} from "../lib/path.js";
import { random, rngFactory, shuffle, randomInt } from "../lib/random.js";
import { hsl, hsla, ColorHex } from "../lib/color.js";
import { glowOnly } from "../lib/filters/glow-only.js";
import { vec2 } from "../lib/Vector2.js";
import {
  LinearGradient,
  linearGradientDirection,
  linearGradientStop,
} from "../lib/gradients/linear.js";

export function draw() {
  const w = 100;
  const h = 100;
  const colors = [
    hsl(222, 50, 49),
    hsl(34, 47, 51),
    hsl(90, 36, 47),
    hsl(273, 24, 48),
    hsl(342, 61, 46),
  ];
  const basePathColor = hsl(220, 10, 70);
  const topToBottom = linearGradientDirection(w / 2, w / 2, 0, h);
  const linearGradients = colors.map((c, i) =>
    LinearGradient({ id: `linear-gradient-${i}`, ...topToBottom }, [
      linearGradientStop(20, hsl(c.h, (c.s * 4) / 8, c.l)),
      linearGradientStop(80, basePathColor),
    ])
  );
  const root = svg(
    {
      fill: "#000000",
      style: "background: #000000",
      height: "100%",
      viewBox: `0 0 ${w} ${h}`,
      // height: "2000px",
      // width: "2000px",
    },
    [
      Defs(glowOnly, ...linearGradients),
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

  const path = Path({
    fill: "none",
    "stroke-width": "0.65",
    "stroke-linecap": "round",
    "stroke-linejoin": "round",
  });

  const seed = Date.now().toString();
  observe(seed);
  const rng = rngFactory(seed);

  const xMin = 0;
  const xMax = w;
  const xStep = 2;
  const yMin = 0;
  const yStep = 2;
  const yMax = h + yStep * 10;
  const ys = range(yMin, yMax, yStep);
  // start at 1 b/c the pathBuilders are already being initialized at yMin
  let yi = 1;
  const xs = range(xMin, xMax, xStep);
  let pathBuilders = array(xs.length).map((i) => [move(vec2(xs[i], yMin))]);
  while (yi < ys.length) {
    let i = 0;
    while (i < pathBuilders.length) {
      const chanceOfCrossing = map(
        Math.round(h / 8),
        ys.length - 1,
        0.0,
        0.4,
        yi
      );

      // cross 2 adjacent
      if (random(0, 1, rng) < chanceOfCrossing && i < pathBuilders.length - 1) {
        // // straight lines
        // pathBuilders[i].push(line(vec2(xStep, yStep), "relative"));
        // pathBuilders[i + 1].push(
        //   line(vec2(-xStep, yStep), "relative")
        // );

        // // Cubic bezier
        const next1 = vec2(xStep, yStep);
        const ctrl1a = vec2(0, yStep);
        const ctrl1b = vec2(xStep, 0);
        pathBuilders[i].push(cubicBezier(ctrl1a, ctrl1b, next1, "relative"));

        const next2 = vec2(-xStep, yStep);
        const ctrl2a = vec2(0, yStep);
        const ctrl2b = vec2(-xStep, 0);
        pathBuilders[i + 1].push(
          cubicBezier(ctrl2a, ctrl2b, next2, "relative")
        );

        // swap pathBuilders to preserve ordering
        [pathBuilders[i], pathBuilders[i + 1]] = [
          pathBuilders[i + 1],
          pathBuilders[i],
        ];
        i++;

      // // cross 2 non-adjacent
      // // this is interesting, but not sure its the look I want
      // } else if (
      //   random(0, 1, rng) < chanceOfCrossing / 4 &&
      //   i < pathBuilders.length - 2
      // ) {
      //   // Cubic bezier
      //   const next1 = vec2(xStep * 2, yStep);
      //   const ctrl1a = vec2(0, yStep);
      //   const ctrl1b = vec2(xStep * 2, 0);
      //   pathBuilders[i].push(cubicBezier(ctrl1a, ctrl1b, next1, "relative"));

      //   const next2 = vec2(-xStep * 2, yStep);
      //   const ctrl2a = vec2(0, yStep);
      //   const ctrl2b = vec2(-xStep * 2, 0);
      //   pathBuilders[i + 2].push(
      //     cubicBezier(ctrl2a, ctrl2b, next2, "relative")
      //   );

      //   // swap pathBuilders to preserve ordering
      //   [pathBuilders[i], pathBuilders[i + 2]] = [
      //     pathBuilders[i + 2],
      //     pathBuilders[i],
      //   ];
      //   i++;
      //   i++;
      } else {
        pathBuilders[i].push(line(vec2(0, yStep), "relative"));
      }

      i++;
    }
    yi++;
  }

  const children = shuffle(pathBuilders).map((d) =>
    path.withAttrs({
      d,
      stroke: `url(#linear-gradient-${randomInt(
        0,
        linearGradients.length,
        rng
      )})`,
      style: `filter: drop-shadow(0.5px 0.5px 1px ${hsl(220, 0, 0)});`,
    })
  );

  return root.withChildren(children).draw();
}

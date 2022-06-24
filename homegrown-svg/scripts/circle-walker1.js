import { svg, tag } from "../lib/tag.js";
import { array, degToRad, map, quantize } from "../lib/util.js";
import { pathBuilder, point } from "../lib/path.js";
import { jitter, random, rngFactory, shuffle } from "../lib/random.js";
import { hsl, hsla } from "../lib/color.js";

export function draw() {
  const w = 100;
  const h = 100;
  const center = point(w / 2, h / 2);
  const root = svg(
    {
      style: "background: #000000",
      height: "100%",
      viewBox: `0 0 ${w} ${h}`,
      // height: "2000px",
      // width: "2000px",
    },
    [
      tag("defs", {}, [
        tag(
          "filter",
          {
            id: "glow",
            filterUnits: "userSpaceOnUse",
            primitiveUnits: "userSpaceOnUse",
          },
          [
            tag("feMorphology", {
              id: "morphology",
              operator: "dilate",
              radius: "4.5",
              in: "SourceGraphic",
              result: "thicken",
            }),
            tag("feGaussianBlur", {
              id: "gaussian",
              stdDeviation: "4.5",
              in: "thicken",
              result: "coloredBlur",
            }),

            tag("feMerge", {}, [
              tag("feMergeNode", { in: "coloredBlur" }),
              tag("feMergeNode", { in: "SourceGraphic" }),
            ]),
          ]
        ),

        // same as #glow but does not blend with source graphic
        tag(
          "filter",
          {
            id: "glow-only",
            filterUnits: "userSpaceOnUse",
            primitiveUnits: "userSpaceOnUse",
          },
          [
            tag("feMorphology", {
              id: "morphology",
              operator: "dilate",
              radius: "4.5",
              in: "SourceGraphic",
              result: "thicken",
            }),
            tag("feGaussianBlur", {
              id: "gaussian",
              stdDeviation: "4.5",
              in: "thicken",
              result: "coloredBlur",
            }),
          ]
        ),

        tag(
          "filter",
          {
            id: "blur",
            filterUnits: "userSpaceOnUse",
            primitiveUnits: "userSpaceOnUse",
          },
          [
            tag("feGaussianBlur", {
              id: "gaussian",
              stdDeviation: "0.2",
              in: "thicken",
              result: "coloredBlur",
            }),
          ]
        ),
      ]),
      tag("rect", {
        x: 0,
        y: 0,
        width: w,
        height: h,
        fill: "#000",
        stroke: "#000",
      }),
    ]
  );

  const circle = tag("circle", {
    r: 15,
  });
  const blackCircle = circle.withAttrs({
    fill: "hsla(0, 0%, 0%, 0.5)",
  });

  const path = tag("path", {
    fill: "none",
    "stroke-width": "2.1",
    "stroke-linecap": "round",
    "stroke-linejoin": "round",
    style: "filter: drop-shadow(1px 1px 0.5px hsla(0, 0%, 40%, 0.5));",
  });

  const baseColor = { h: 260, s: 25, l: 8 };

  // const seed = 1656037905577;
  const seed = Date.now().toString();
  console.log({ seed });
  try {
    window.location.hash = seed;
  } catch (e) {
    // window is probably not defined, no biggie
  }
  const rng = rngFactory(seed);

  const nChildren = 300;
  const children = []
  const paths = shuffle(array(nChildren), rng).map((i) => {
    const startAngle = degToRad(map(0, nChildren - 1, 0, 360, i));
    const startPoint = point(
      center.x + Math.cos(startAngle) * w,
      center.y + Math.sin(startAngle) * h
    );

    return path.withAttrs({
      d: pathBuilder((d) => {
        // ensure the path points towards center
        const direction = quantize(Math.PI / 6, startAngle) - Math.PI;
        const genAngle = genAngleFactory(rng, direction, Math.PI / 6);
        d.move(startPoint);

        const velocity = 2;
        const length = 50;
        let i = 0;
        while (i++ < length) {
          const angle = genAngle(d.cursor(), center);
          d.line(
            point(Math.cos(angle) * velocity, Math.sin(angle) * velocity),
            // Consider making "relative" the default. I feel like it will be more common than absolute for this API
            "relative"
          );
        }
      }),
      // possible to do a gradient with the color in the stroke?
      // https://developer.mozilla.org/en-US/docs/Web/SVG/Tutorial/Gradients
      stroke: hsla(
        jitter(10, baseColor.h, rng),
        jitter(10, baseColor.s, rng),
        jitter(10, baseColor.l, rng),
        0.5
      ),
    });
  });

  // children.push(...paths);

  const cy = h/2
  children.push(
    circle.withAttrs({cx: w*2/8, cy, filter: 'url(#glow-only)', fill: 'hsl(222, 50%, 49%)'}),
    blackCircle.withAttrs({cx: w*2/8, cy}),
    circle.withAttrs({cx: w*3/8, cy, filter: 'url(#glow-only)', fill: 'hsl(34, 47%, 51%)'}),
    blackCircle.withAttrs({cx: w*3/8, cy}),
    circle.withAttrs({cx: w*4/8, cy, filter: 'url(#glow-only)', fill: 'hsl(90, 36%, 47%)'}),
    blackCircle.withAttrs({cx: w*4/8, cy}),
    circle.withAttrs({cx: w*5/8, cy, filter: 'url(#glow-only)', fill: 'hsl(273, 24%, 48%)'}),
    blackCircle.withAttrs({cx: w*5/8, cy}),
    circle.withAttrs({cx: w*6/8, cy, filter: 'url(#glow-only)', fill: 'hsl(342, 61%, 46%)'}),
    blackCircle.withAttrs({cx: w*6/8, cy}),
  );

  return root.withChildren(children).draw();
}

/**
 * @param {RNG} rng
 * @param {Radians} dominantDirection
 * @param {Radians} variantAngle
 * @param {number} chanceOfStraightLine range in [0.0, 1.0], as percentage chance that line will be straight
 * @returns
 */
function genAngleFactory(
  rng = Math.random,
  dominantDirection = 0,
  variantAngle = Math.PI / 6,
  chanceOfStraightLine = 0.85,
  maxAngleChanges = 3
) {
  let angle = dominantDirection;
  let angleChangeCount = 0;
  /**
   * generate a new angle based on the previous angle
   * @param {Point} cursor
   * @returns Radians
   */
  return function genAngle(cursor, center) {
    // if (angleChangeCount > maxAngleChanges) {
    //   return angle
    // }
    // continuously determine the dominant direction based on the current position
    const direction = Math.atan2(cursor.y - center.y, cursor.x - center.x);
    dominantDirection = quantize(variantAngle, direction - Math.PI);
    // encourage straight lines
    // if (random(0.0, 1.0, rng) < chanceOfStraightLine) {
    //   angleChangeCount++
    //   return dominantDirection;
    // }
    if (random(0.0, 1.0, rng) < chanceOfStraightLine) {
      return angle;
    }
    const chance = random(0.0, 1.0, rng);
    if (chance < 1 / 3) {
      angleChangeCount++;
      angle = dominantDirection - variantAngle;
    } else if (chance < 2 / 3) {
      angleChangeCount++;
      angle = dominantDirection + variantAngle;
    } else {
      // angleChangeCount++
      // angle = dominantDirection;
    }
    return angle;
  };
}

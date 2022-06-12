import { svg, tag } from "../lib/tag.js";
import { array, degToRad, map, quantize } from "../lib/util.js";
import { pathBuilder, point } from "../lib/path.js";
import { random, rngFactory } from "../lib/random.js";

export function draw() {
  const w = 100;
  const h = 100;
  const center = point(w / 2, h / 2);
  const root = svg(
    {
      fill: "#000000",
      stroke: "#ffffff",
      style: "background: #000000",
      height: "100%",
      // width: "100%",
      viewBox: `0 0 ${w} ${h}`,
      // height: "2000px",
      // width: "2000px",
    },
    [
      tag("defs", {}, [
        // https://css-tricks.com/look-svg-light-source-filters/
        tag("filter", { id: "demo4" }, [
          tag("feGaussianBlur", { stdDeviation: "20", result: "blur4" }),
          tag(
            "feSpecularLighting",
            {
              result: "spec4",
              in: "blur4",
              // the lower this is, the more of a "flood" effect it will have
              specularExponent: "300",
              "lighting-color": "#cccccc",
            },
            [tag("fePointLight", {
              x: "25",
              y: "25",
              // the further away (higher z), the more of a "flood" effect it has. (intensity does not decrease with distance)
              z: "50"
            })]
          ),
          // I'm having trouble building intuition for this part: https://codepen.io/jonitrythall/pen/OJwEoa
          tag("feComposite", {
            in: "SourceGraphic",
            in2: "spec4",
            operator: "arithmetic",
            k1: "0",
            k2: "1",
            k3: "1",
            k4: "0",
          }),
        ]),
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

  const path = tag("path", {
    fill: "none",
    "stroke-width": "1.1",
    "stroke-linecap": "round",
    "stroke-linejoin": "round",
    style: "filter: drop-shadow(1px 1px 0.7px #222);",
    // filter: "url(#demo4)",
  });

  const baseColor = { r: 196, g: 120, b: 47 };
  const colors = ["#fff", "#faf", "#fb4", "#4ff"];

  const seed = Date.now().toString();
  console.log({ seed });
  try {
    window.location.hash = seed;
  } catch (e) {
    // window is probably not defined, no biggie
  }
  const rng = rngFactory(seed);

  const nChildren = 300;
  const children = array(nChildren).map((i) => {
    const startAngle = degToRad(map(0, nChildren - 1, 0, 360, i));
    const startPoint = point(
      center.x + Math.cos(startAngle) * w,
      center.y + Math.sin(startAngle) * h
    );

    // see where the line starts
    // return tag('circle', { cx: startPoint.x, cy: startPoint.y, r: '2', fill: '#fff', stroke: '#fff'})

    return path.withAttrs({
      d: pathBuilder((d) => {
        // ensure the path points towards center
        const direction = quantize(Math.PI / 6, startAngle) - Math.PI;
        const genAngle = genAngleFactory(rng, direction, Math.PI / 6, 0.75);
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
      stroke: colors[i % 4],
    });
  });

  children.push(
    tag("polygon", {
      points: "33,66 50,33 66,66",
      fill: "#000",
      stroke: "#fff",
      "stroke-linecap": "round",
      "stroke-linejoin": "round",
      style: "filter: drop-shadow(0px 0px 10px #fff);",
      // style: "box-shadow: 0px 0px 10px 10px #fff;"
    })
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
  chanceOfStraightLine = 0.8
) {
  let angle = dominantDirection;
  /**
   * generate a new angle based on the previous angle
   * @param {Point} cursor
   * @returns Radians
   */
  return function genAngle(cursor, center) {
    // continuously determine the dominant direction based on the current position
    const direction = Math.atan2(cursor.y - center.y, cursor.x - center.x);
    dominantDirection = quantize(variantAngle, direction) - Math.PI;
    // encourage straight lines
    if (random(0.0, 1.0, rng) < chanceOfStraightLine) {
      return angle;
    }
    const chance = random(0.0, 1.0, rng);
    if (chance < 1 / 3) {
      angle = dominantDirection - variantAngle;
    } else if (chance < 2 / 3) {
      angle = dominantDirection + variantAngle;
    } else {
      angle = dominantDirection;
    }
    return angle;
  };
}

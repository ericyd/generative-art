import {
  renderSvg,
  map,
  vec2,
  randomSeed,
  createRng,
  random,
  circle,
  ColorRgb,
  randomFromArray,
  rect,
  hypot,
  Grid,
  range,
  hsl,
  lineSegment,
  Rectangle,
  randomInt,
  PI,
  cos,
  sin,
  clamp,
  ColorSequence,
  shuffle,
  Polygon,
  rangeWithIndex,
  createOscNoise,
  Hexagon,
  TAU,
  path,
  polyline,
  Polyline,
  createOscCurl,
  randomFromObject,
  PHI,
  LineSegment,
  Circle,
  Svg,
  ColorHsl,
  grid,
  Path,
  Vector3,
  Vector2,
  array,
  tag,
  Tag,
  LinearGradient,
  Layer,
} from "@salamivg/core";

const config = {
  width: 800,
  height: 800,
  scale: 1,
  loopCount: 1,
  // openEveryFrame: false
};

let seed = randomSeed();

renderSvg(config, (svg) => {
  const rng = createRng(seed);
  svg.filenameMetadata = { seed: String(seed) };
  svg.numericPrecision = 3;

  const gradients = {
    bg: svg.defineLinearGradient({
      colors: ["#f5b993ff", "#f4de7bfe"],
    }),
    rect1: svg.defineLinearGradient({
      colors: ["#8d42ac77", "#8d42ac00"],
      start: vec2(0, 0),
      end: vec2(0.7, 0),
    }),
    rect2: svg.defineLinearGradient({
      colors: ["#428bac77", "#428bac00"],
      start: vec2(0, 1),
      end: vec2(0, 0.3),
    }),
    rect3: svg.defineLinearGradient({
      colors: ["#cc9c43cd", "#cc9c4300"],
      start: vec2(0, 0),
      end: vec2(0, 0.7),
    }),
    rect4: svg.defineLinearGradient({
      colors: ["#4265ac77", "#4265ac00"],
      start: vec2(1, 0),
      end: vec2(0.3, 0),
    }),
    rect5: svg.defineLinearGradient({
      colors: ["#6dac4277", "#6dac4200"],
      start: vec2(0, 0),
      end: vec2(0.7, 0),
    }),
    rect6: svg.defineLinearGradient({
      colors: ["#e4b314ff", "#e4b31400"],
      start: vec2(0, 1),
      end: vec2(0, 0.3),
    }),
    rect7: svg.defineLinearGradient({
      colors: ["#f67f37d5", "#f67f3700"],
      start: vec2(0, 0),
      end: vec2(0, 0.7),
    }),
    rect8: svg.defineLinearGradient({
      colors: ["#c8265577", "#c8265500"],
      start: vec2(1, 0),
      end: vec2(0.3, 0),
    }),
    rect9: svg.defineLinearGradient({
      colors: ["#a62a2877", "#a62a2800"],
      start: vec2(0, 0),
      end: vec2(0.7, 0),
    }),
    rect10: svg.defineLinearGradient({
      colors: ["#5e388f87", "#5e388f00"],
      start: vec2(0, 1),
      end: vec2(0, 0.3),
    }),
    rect11: svg.defineLinearGradient({
      colors: ["#49a58d97", "#49a58d00"],
      start: vec2(0, 0),
      end: vec2(0, 0.7),
    }),
    rect12: svg.defineLinearGradient({
      colors: ["#fc8d2fa8", "#fc8d2f00"],
      start: vec2(1, 0),
      end: vec2(0.3, 0),
    }),
  };

  svg.setBackground(gradients.bg);
  svg.setAttributes({ "stroke-linecap": "round" });

  svg.layer(drawInscribedCircleLines);

  svg.layer(
    addRectGroup(
      vec2(config.width / 20, config.height / 10),
      gradients.rect1,
      gradients.rect2,
      gradients.rect3,
      gradients.rect4,
    ),
  );
  svg.layer(
    addRectGroup(
      vec2((config.width / 20) * 5, (config.height / 10) * 2),
      gradients.rect5,
      gradients.rect6,
      gradients.rect7,
      gradients.rect8,
    ),
  );
  svg.layer(
    addRectGroup(
      vec2((config.width / 20) * 9, (config.height / 10) * 3),
      gradients.rect9,
      gradients.rect10,
      gradients.rect11,
      gradients.rect12,
    ),
  );

  // define "film grain" filter, based on Inkscape's "Film grain" filter
  svg.addChild(
    tag("defs", (t) => {
      // lower is more coarse
      const coarseness = "0.8";

      t.addChild(
        tag("filter", (filterTag) => {
          filterTag.setAttributes({
            id: "film_gain_filter",
            x: "0",
            y: "0",
            width: "1",
            height: "1",
            style: "color-interpolation-filters:sRGB",
          });
          filterTag.addChild(
            tag("feTurbulence", (fe) =>
              fe.setAttributes({
                type: "fractalNoise",
                numOctaves: "3",
                baseFrequency: coarseness,
                seed: String(randomSeed()),
                result: "result0",
              }),
            ),
          );
          filterTag.addChild(
            tag("feColorMatrix", (fe) =>
              fe.setAttributes({
                result: "result4",
                values: "0",
                type: "saturate",
              }),
            ),
          );
          filterTag.addChild(
            tag("feComposite", (fe) =>
              fe.setAttributes({
                in: "SourceGraphic",
                in2: "result4",
                operator: "arithmetic",
                k1: "1.25",
                k2: "0.5",
                k3: "0.5",
                k4: "0",
                result: "result2",
              }),
            ),
          );
          filterTag.addChild(
            tag("feBlend", (fe) =>
              fe.setAttributes({
                result: "result5",
                mode: "normal",
                in: "result2",
                in2: "SourceGraphic",
              }),
            ),
          );
          filterTag.addChild(
            tag("feComposite", (fe) =>
              fe.setAttributes({
                in: "result5",
                in2: "SourceGraphic",
                operator: "in",
                result: "result3",
              }),
            ),
          );
        }),
      );
    }),
  );

  // "Film grain" layer
  svg.addChild(
    tag("g", (tag) =>
      tag.addChild(
        rect((r) => {
          r.x = 0;
          r.y = 0;
          r.width = config.width;
          r.height = config.height;
          r.fill = "#e7e7e769";
          r.setAttributes({ style: "filter:url(#film_gain_filter)" });
        }),
      ),
    ),
  );

  return () => {
    seed = randomSeed();
  };
});

function addRectGroup(
  start: Vector2,
  gradient1: LinearGradient,
  gradient2: LinearGradient,
  gradient3: LinearGradient,
  gradient4: LinearGradient,
) {
  const width = config.width / 3.33333333;
  const height = (config.width * 2) / 3.33333333;
  const borderRadius = width * 0.2;
  return (tag: Layer) => {
    tag.rect({
      x: start.x,
      y: start.y,
      width: width,
      height: height,
      borderRadius: borderRadius,
      fill: gradient1,
    });
    tag.rect({
      x: start.x + width / 3,
      y: start.y,
      width: width,
      height: height,
      borderRadius: borderRadius,
      fill: gradient2,
    });
    tag.rect({
      x: start.x + width / 3,
      y: start.y,
      width: width,
      height: height,
      borderRadius: borderRadius,
      fill: gradient3,
    });
    tag.rect({
      x: start.x + (width * 2) / 3,
      y: start.y,
      width: width,
      height: height,
      borderRadius: borderRadius,
      fill: gradient4,
    });
  };
}

function drawInscribedCircleLines(tag: Layer) {
  const center = vec2(config.width / 2, config.height / 2);
  const radius = config.width * 0.8 * 0.5;

  const strokeWidth = 0.75 * (config.width / 800);
  const stepSize = config.height * 0.006;

  let y = center.y - radius;
  while (y < center.y + radius) {
    const dy = y - center.y;
    const halfChord = Math.sqrt(Math.max(0, radius * radius - dy * dy));
    const xStart = center.x - halfChord;
    const xEnd = center.x + halfChord;
    const line = lineSegment(vec2(xStart, y), vec2(xEnd, y));
    line.stroke = "#0d0d0d81";
    line.strokeWidth = strokeWidth;
    tag.lineSegment(line);
    y += stepSize;
  }
}

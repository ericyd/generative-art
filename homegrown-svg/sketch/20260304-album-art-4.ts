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
  Group,
} from "@salamivg/core";

const config = {
  width: 800,
  height: 800,
  scale: 1,
  loopCount: 1,
  // openEveryFrame: false
};

const baseColors = [
  "#8d42ac77",
  "#428bac77",
  "#cc9c43cd",
  "#4265ac77",
  "#6dac4277", //
  "#e4b314ff",
  "#f67f37d5",
  "#c8265577",
  "#a62a2877",
  "#5e388f87",
  "#49a58d97", //
  "#fc8d2fa8",
];

let seed = randomSeed();
// #fc8d2fa8 #49a58d97
seed = 944556595314299;

renderSvg(config, (svg) => {
  const rng = createRng(seed);
  svg.colorFormat = "hex";
  svg.filenameMetadata = { seed: String(seed) };
  svg.numericPrecision = 3;

  const colors = shuffle(baseColors, rng).map(ColorRgb.fromHex);

  const bg = svg.defineLinearGradient({
    colors: ["#f5b993ff", "#f4de7bfe"],
  });
  console.log("colors", colors[0].toHex(), colors[1].toHex());

  const gradients = [
    ...colors.map((color) => {
      // Randomize start and end vectors for gradient direction
      // const angle = Math.random() * Math.PI * 2;
      // const len = 1;
      // const start = vec2(Math.cos(angle) * len, Math.sin(angle) * len);
      // const end = vec2(Math.cos(angle + Math.PI) * len, Math.sin(angle + Math.PI) * len);
      const start = vec2(0, 0);
      const end = vec2(0, 1);
      const invert = random(0, 1, rng) > 0.5;
      return svg.defineLinearGradient({
        colors: [color, color.opacify(0)],
        start: invert ? end : start,
        end: invert ? start : end,
      });
    }),
  ];

  svg.setBackground(bg);
  svg.setAttributes({ "stroke-linecap": "round" });

  svg.fill = null;

  // start: vec2(config.width / 20, config.height / 10);
  let i = 0;
  for (
    let x = config.width * 0.35;
    x < config.width * 0.67;
    x += config.width * 0.025 // let x = 0;
    // x < config.width;
  ) // x += config.width * 0.025
  {
    const width = config.width / 3.33333333;
    const height = config.height * 0.5;
    const borderRadius = width * 0.2;
    svg.circle(
      circle({
        x,
        y: config.height * 0.5,
        radius: height / 2,
        stroke: gradients[i % 2],
        strokeWidth: config.width * 0.05,
      }),
    );
    i++;
  }

  svg.group(drawInscribedCircleLines);

  // define "film grain" filter, based on Inkscape's "Film grain" filter
  svg.addChild(
    tag("defs", (t) => {
      t.addChild(tag("filter", filmGrainFilter));
    }),
  );

  // "Film grain" Group
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

function drawInscribedCircleLines(tag: Group) {
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

function filmGrainFilter(filterTag: Tag) {
  // lower is more coarse
  const coarseness = "0.8";
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
}

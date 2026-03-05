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

renderSvg(config, (svg) => {
  const rng = createRng(seed);
  svg.filenameMetadata = { seed: String(seed) };
  svg.numericPrecision = 3;

  const colors = ColorSequence.fromColors(shuffle(baseColors, rng));

  const gradients = {
    bg: svg.defineLinearGradient({
      colors: ["#f5b993ff", "#f4de7bfe"],
    }),
  };

  // define filters
  svg.addChild(
    tag("defs", (t) => {
      // t.addChild(tag("filter", filmGrainFilter));
      t.addChild(tag("filter", stippleFilter));
    }),
  );

  svg.setBackground(gradients.bg);
  svg.setAttributes({ "stroke-linecap": "round" });

  svg.group(drawInscribedCircleLines);

  const radius = 20;
  const gradientAngle = random(0, TAU, rng);
  const projExtent = [
    Math.min(
      0,
      config.width * cos(gradientAngle),
      config.height * sin(gradientAngle),
      config.width * cos(gradientAngle) + config.height * sin(gradientAngle),
    ),
    Math.max(
      0,
      config.width * cos(gradientAngle),
      config.height * sin(gradientAngle),
      config.width * cos(gradientAngle) + config.height * sin(gradientAngle),
    ),
  ];

  for (const [{ x, y }] of grid({
    xMin: radius * 1,
    xMax: config.width - radius * 1.5,
    yMin: radius * 1,
    yMax: config.height - radius * 1.5,
    xStep: radius * 2,
    yStep: radius * 2,
  })) {
    // if point is outside of the "circle",
    // discard it
    if (
      hypot(x - config.width / 2, y - config.height / 2) >
      config.width * 0.4
    ) {
      continue;
    }

    const angle = random(0, TAU, rng);
    const proj = x * cos(gradientAngle) + y * sin(gradientAngle);
    const baseColor = colors.at(
      (proj - projExtent[0]) / (projExtent[1] - projExtent[0]),
    );
    const val1 = vec2(
      map(-1, 1, 0, 1, Math.cos(angle)),
      map(-1, 1, 0, 1, Math.sin(angle)),
    );
    const val2 = vec2(
      map(-1, 1, 0, 1, -Math.cos(angle)),
      map(-1, 1, 0, 1, -Math.sin(angle)),
    );
    const invert = random(0, 1, rng) > 0.5;
    const grad = svg.defineLinearGradient({
      colors: [baseColor, baseColor.opacify(0)],
      start: vec2(Math.cos(angle), Math.sin(angle)),
      end: vec2(-Math.cos(angle), -Math.sin(angle)),
    });
    svg.circle(
      circle({
        x,
        y,
        radius,
        fill: grad,
        stroke: null,
        style: "filter:url(#stipple_filter)",
      }),
    );
  }

  // "Stippled" overlay - or apply to any element via filter:url(#stipple_filter)
  svg.addChild(
    tag("g", (g) =>
      g.addChild(
        rect((r) => {
          r.x = 0;
          r.y = 0;
          r.width = config.width;
          r.height = config.height;
          r.fill = "#80808040";
          r.setAttributes({ style: "filter:url(#stipple_filter)" });
        }),
      ),
    ),
  );

  return () => {
    seed = randomSeed();
  };
});

function stippleFilter(filterTag: Tag) {
  // lower = coarser dots, higher = finer stipple
  const baseFrequency = "0.4";
  // Subtle vintage paper texture (current - narrow range, centered)
  // const tableValues = "0.4 0.45 0.5 0.55 0.6";

  // High contrast stipple dots (wide range)
  // const tableValues = "0.1 0.5 0.9";

  // Bold 2-tone halftone (binary threshold)
  // const tableValues = "0.3 0.7";

  // Darker/moodier grain (values below 0.5)
  // const tableValues = "0.2 0.3 0.4 0.5";

  // Lighter/washed out (values above 0.5)
  // const tableValues = "0.5 0.6 0.7 0.8";

  // More gradual steps (smoother but still textured)
  const tableValues = "0.35 0.4 0.45 0.5 0.55 0.6 0.65";

  // Asymmetric - bright highlights, crushed shadows
  // const tableValues = "0.1 0.2 0.5 0.8 0.9";
  filterTag.setAttributes({
    id: "stipple_filter",
    x: "0",
    y: "0",
    width: "1",
    height: "1",
    style: "color-interpolation-filters:sRGB",
  });
  // generate noise
  filterTag.addChild(
    tag("feTurbulence", (fe) =>
      fe.setAttributes({
        type: "fractalNoise",
        numOctaves: "3",
        baseFrequency,
        seed: String(randomSeed()),
        result: "noise",
      }),
    ),
  );
  // quantize to discrete levels for stipple dots
  filterTag.addChild(
    tag("feComponentTransfer", (fe) => {
      fe.setAttributes({
        in: "noise",
        result: "stipple_gray",
      });
      for (const channel of ["feFuncR", "feFuncG", "feFuncB"]) {
        fe.addChild(
          tag(channel, (fn) =>
            fn.setAttributes({ type: "discrete", tableValues }),
          ),
        );
      }
      fe.addChild(
        tag("feFuncA", (fn) => fn.setAttributes({ type: "identity" })),
      );
    }),
  );
  // subtle blend with source using soft-light for vintage look
  filterTag.addChild(
    tag("feBlend", (fe) =>
      fe.setAttributes({
        in: "SourceGraphic",
        in2: "stipple_gray",
        mode: "soft-light",
        result: "blended",
      }),
    ),
  );
  // clip to original shape
  filterTag.addChild(
    tag("feComposite", (fe) =>
      fe.setAttributes({
        in: "blended",
        in2: "SourceGraphic",
        operator: "in",
        result: "final",
      }),
    ),
  );
}

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

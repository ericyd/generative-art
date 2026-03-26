import {
  renderSvg,
  map,
  vec2,
  randomSeed,
  createRng,
  random,
  circle,
  hypot,
  cos,
  sin,
  clamp,
  ColorSequence,
  shuffle,
  TAU,
  grid,
  Vector2,
  tag,
  rect,
  Tag,
  PI,
  lineSegment,
  createOscNoise,
  ColorRgb,
} from "@salamivg/core";

const config = { width: 800, height: 800, scale: 1, loopCount: 1 };

const baseColors = ["#88c4e0ff", "#cee5c3ff"];

const gradientColors = ["#fc8d2fa8", "#49a58d97"].map(ColorRgb.fromHex);

let seed = randomSeed();

renderSvg(config, (svg) => {
  const rng = createRng(seed);

  const gradients = [
    ...gradientColors.map((color, i) => {
      // Randomize start and end vectors for gradient direction
      // const angle = Math.random() * Math.PI * 2;
      // const len = 1;
      // const start = vec2(Math.cos(angle) * len, Math.sin(angle) * len);
      // const end = vec2(Math.cos(angle + Math.PI) * len, Math.sin(angle + Math.PI) * len);
      const start = vec2(0, 0);
      const end = vec2(0, 1);
      const invert = i === 0;
      return svg.defineLinearGradient({
        colors: [color, color.opacify(0)],
        start: invert ? end : start,
        end: invert ? start : end,
      });
    }),
  ];

  const noiseXyScale = random(0.015, 0.035, rng);
  const noiseZScale =
    config.width * map(0.015, 0.035, 0.035, 0.015, noiseXyScale);

  const noise = createOscNoise(seed, noiseXyScale, noiseZScale);
  svg.filenameMetadata = { seed: String(seed) };
  svg.numericPrecision = 3;

  const colors = ColorSequence.fromColors(shuffle(baseColors, rng));
  svg.setBackground(
    svg.defineLinearGradient({ colors: ["#cbebf2ff", "#f4fbfcff"] })
  );

  svg.addChild(
    tag("defs", (t) => {
      // t.addChild(tag("filter", filmGrainFilter));
      t.addChild(tag("filter", stippleFilter));
    })
  );

  const radius = config.width * 0.3;

  const center = vec2(config.width / 2, config.height / 2);

  // 0.75
  const strokeWidth = 0.75 * (config.width / 800);
  const stepY = strokeWidth * 5;
  const stepX = strokeWidth * 5;

  const yStart = center.y - radius;
  const yEnd = center.y + radius;
  for (let y = yStart; y < yEnd; y += stepY) {
    const dy = y - center.y;
    const halfChord = Math.sqrt(Math.max(0, radius * radius - dy * dy));
    const xStart = center.x - halfChord;
    const xEnd = center.x + halfChord;

    for (let x = xStart; x < xEnd * 2; x += stepX) {
      const t_x = map(xStart, xEnd, 0, 1, x, true);

      if (random(0, 1, rng) < t_x) {
        continue;
      }

      const t_y = map(yStart, yEnd, 0, 1, y, true);
      const offset = noise(x, y) * t_x;
      // const offset = map(0, 1, 0, config.width * 0.2, t_x ** 3) * t_y;

      const line = lineSegment(
        vec2(x, y + offset),
        vec2(x + stepX, y + offset)
      );

      // if resulting end point `line.points[1]` is outside the bounds of the circle, skip this line
      if (
        hypot(line.points[1].x - center.x, line.points[1].y - center.y) > radius
      ) {
        continue;
      }

      line.stroke = "#0d0d0d81";
      line.strokeWidth = strokeWidth;
      svg.lineSegment(line);
    }
  }

  // let i = 0;
  // for (
  //   let x = config.width * 0.35;
  //   x < config.width * 0.67;
  //   x += config.width * 0.025 // let x = 0; // x += config.width * 0.025 // x < config.width;
  // ) {
  //   i++;
  // }
  // const width = config.width / 3.33333333;
  const width = config.width * 0.025;
  const offset = config.width * 0.015;
  svg.circle(
    circle({
      x: config.width * 0.5,
      y: config.height * 0.5,
      radius: radius + offset,
      fill: "none",
      stroke: gradients[0],
      strokeWidth: width,
    })
  );
  svg.circle(
    circle({
      x: config.width * 0.5 - offset * 2,
      y: config.height * 0.5,
      radius: radius + offset,
      fill: "none",
      stroke: gradients[1],
      strokeWidth: width,
    })
  );

  // "Stippled" overlay - or apply to any element via filter:url(#stipple_filter)
  svg.group((g) => {
    g.addChild(
      rect((r) => {
        r.x = 0;
        r.y = 0;
        r.width = config.width;
        r.height = config.height;
        r.fill = "#80808040";
        r.setAttributes({ style: "filter:url(#stipple_filter)" });
      })
    );
  });

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
      })
    )
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
            fn.setAttributes({ type: "discrete", tableValues })
          )
        );
      }
      fe.addChild(
        tag("feFuncA", (fn) => fn.setAttributes({ type: "identity" }))
      );
    })
  );
  // subtle blend with source using soft-light for vintage look
  filterTag.addChild(
    tag("feBlend", (fe) =>
      fe.setAttributes({
        in: "SourceGraphic",
        in2: "stipple_gray",
        mode: "soft-light",
        result: "blended",
      })
    )
  );
  // clip to original shape
  filterTag.addChild(
    tag("feComposite", (fe) =>
      fe.setAttributes({
        in: "blended",
        in2: "SourceGraphic",
        operator: "in",
        result: "final",
      })
    )
  );
}

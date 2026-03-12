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
} from "@salamivg/core";

const config = { width: 800, height: 800, scale: 1, loopCount: 1 };

const baseColors = ["#88c4e0ff", "#ccf7b7ff"];

let seed = randomSeed();

renderSvg(config, (svg) => {
  const rng = createRng(seed);
  svg.filenameMetadata = { seed: String(seed) };
  svg.numericPrecision = 3;

  const colors = ColorSequence.fromColors(shuffle(baseColors, rng));
  svg.setBackground(
    svg.defineLinearGradient({ colors: ["#a3e0edff", "#effafcff"] })
  );

  svg.addChild(
    tag("defs", (t) => {
      // t.addChild(tag("filter", filmGrainFilter));
      t.addChild(tag("filter", stippleFilter));
    })
  );

  const radius = config.width * 0.4;
  const center = vec2(config.width / 2, config.height / 2);
  const ballRadius = 4;
  const gradientAngle = random(0, TAU, rng);
  const projExtent = [
    Math.min(
      0,
      config.width * cos(gradientAngle),
      config.height * sin(gradientAngle),
      config.width * cos(gradientAngle) + config.height * sin(gradientAngle)
    ),
    Math.max(
      0,
      config.width * cos(gradientAngle),
      config.height * sin(gradientAngle),
      config.width * cos(gradientAngle) + config.height * sin(gradientAngle)
    ),
  ];

  for (const [{ x, y }] of grid({
    xMin: 0,
    xMax: config.width,
    yMin: 0,
    yMax: config.height,
    xStep: ballRadius,
    yStep: ballRadius,
  })) {
    // discard if (x,y) is outside of circle defined by <radius,center>
    if (hypot(x - center.x, y - center.y) > radius) {
      continue;
    }

    // discard if chance is lower than threshold
    const discardThreshold = map(
      config.height * 0.5 - radius,
      config.height * 0.5 + radius * 0.5,
      0,
      1,
      y
    );
    if (random(0, 1, rng) < discardThreshold) {
      continue;
    }

    let pos = vec2(x, y).jitter(ballRadius, rng);

    const angle = random(0, TAU, rng);
    const proj = x * cos(gradientAngle) + y * sin(gradientAngle);
    const baseColor = colors.at(
      (proj - projExtent[0]) / (projExtent[1] - projExtent[0])
    );
    const grad = svg.defineLinearGradient({
      colors: [baseColor, baseColor.opacify(0)],
      start: vec2(Math.cos(angle), Math.sin(angle)),
      end: vec2(-Math.cos(angle), -Math.sin(angle)),
    });

    svg.circle(
      circle({
        center: pos,
        radius: ballRadius,
        fill: grad,
        stroke: null,
      })
    );
  }

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

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
} from "@salamivg/core";

const config = { width: 800, height: 800, scale: 1, loopCount: 1 };

const baseColors = ["#88c4e0ff", "#cee5c3ff"];

let seed = randomSeed();

renderSvg(config, (svg) => {
  const rng = createRng(seed);
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

  const radius = config.width * 0.2;
  const gradientAngle = random(0, TAU, rng);
  const center = vec2(config.width / 2, config.height / 2).add(
    Vector2.fromAngle(gradientAngle + PI).scale(radius)
  );
  const ballRadius = 3;
  const stepSize = ballRadius * 0.85;
  // projExtent defines the minimum and maximum projected values along the gradientAngle direction,
  // used to normalize positions into [0, 1] for color interpolation across the shape
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

  // calculate vectors and projections for gradientAngle
  const centerProj = center.dot(Vector2.fromAngle(gradientAngle));
  //# DEBUG
  // const centerGradientVector = Vector2.fromAngle(gradientAngle)
  //   .scale(radius)
  //   .add(center);
  // const line = lineSegment(centerGradientVector, center);
  // line.fill = "#00000000";
  // line.stroke = "#000000";
  // svg.lineSegment(line);
  //# /DEBUG
  const projMin = centerProj - radius;
  const projMax = centerProj + radius * 3;

  for (const [p] of grid({
    xMin: 0,
    xMax: config.width,
    yMin: 0,
    yMax: config.height,
    xStep: stepSize,
    yStep: stepSize,
  })) {
    const { x, y } = p;
    // calculate projected position along gradientAngle
    const proj = p.dot(Vector2.fromAngle(gradientAngle));

    // discard based on position relative to gradient center
    if (proj < centerProj) {
      // "below" center: must be inside the circle
      if (hypot(x - center.x, y - center.y) > radius) continue;
    } else {
      // map `projectedPercentage` to a "width" along an exponential curve
      const t = map(centerProj, projMax, 0, 1, proj, true);
      const projectedWidth = map(0, 1, radius, radius * 3, t ** 4);

      // Use a sigmoid ("S-shaped") curve to map t in [0,1] to [0,1]
      // Standard logistic sigmoid, centered at 0.5, with adjustable steepness
      // const steepness = 8; // higher = sharper S
      // const sShape = 1 / (1 + Math.exp(-steepness * (t - 0.5)));
      // const projectedWidth = map(0, 1, radius, radius / 3, sShape);

      // perpendicular distance from `p` to the line through `center` along `gradientAngle`
      const perpDist = Math.abs(
        (p.x - center.x) * sin(gradientAngle) -
          (p.y - center.y) * cos(gradientAngle)
      );
      // if the perpendicular distance is greater than the projected width, discard
      if (perpDist > projectedWidth) continue;
    }

    // discard if chance is lower than threshold
    // threshold is based on (x,y) position along gradientAngle
    // the percentage of the projected position along the gradientAngle
    const projectedPercentage = map(projMin, projMax, 0.3, 0.5, proj);
    if (random(0, 1, rng) < projectedPercentage) {
      continue;
    }

    let pos = vec2(x, y).jitter(ballRadius, rng);

    const angle = random(0, TAU, rng);
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
        // stroke: baseColor,
        // strokeWidth: ballRadius * 0.2,
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

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
  Group,
  Random,
  hsl,
  ColorRgb,
  polyline,
  path,
} from "@salamivg/core";

const config = { width: 800, height: 800, scale: 1, loopCount: 1 };

const baseColors = ["#88c4e0ff", "#cee5c3ff"];

let seed = randomSeed();
seed = 8715798022807551;

renderSvg(config, (svg) => {
  const rnd = Random.create(seed);

  const noiseXyScale = rnd.value(0.015, 0.035);
  const noiseZScale =
    config.width * map(0.015, 0.035, 0.035, 0.015, noiseXyScale);

  const noise = createOscNoise(seed, noiseXyScale, noiseZScale);
  svg.filenameMetadata = { seed: String(seed) };
  svg.numericPrecision = 3;

  const colors = ColorSequence.fromColors(["#cbebf2ff", "#f4fbfcff"]);

  svg.setBackground(
    svg.defineLinearGradient({ colors: ["#cbebf2ff", "#f4fbfcff"] }),
  );

  svg.group(
    halftoneStipple(rnd, ColorSequence.fromColors(["#ffffff", "#cdcdcd"])),
  );

  const radius = config.width * 0.3;

  const center = vec2(config.width / 2, config.height / 2);

  // 0.75
  const strokeWidth = 0.75 * (config.width / 800);
  const stepY = strokeWidth * 5;
  const stepX = strokeWidth * 5;

  const yStart = center.y - radius;
  const yEnd = center.y + radius;

  const strokeColor = ColorRgb.fromHex("#0d0d0d81");
  svg.group((group) => {
    group.stroke = "rgba(13, 13, 13, 0.5058823529411764)";
    group.fill = "none";
    for (let y = yStart; y < yEnd; y += stepY) {
      const dy = y - center.y;
      const halfChord = Math.sqrt(Math.max(0, radius * radius - dy * dy));
      const xStart = center.x - halfChord;
      const xEnd = center.x + halfChord;

      let p = path({
        style: `stroke: rgba(13, 13, 13, 0.5058823529411764)`,
        strokeWidth: strokeWidth,
      });
      p.moveTo(vec2(xStart, y));

      for (let x = xStart; x < xEnd * 2; x += stepX) {
        const t_x = map(xStart, xEnd, 0, 1, x, true);
        const offset = noise(x, y) * t_x;
        const endPoint = vec2(x + stepX, y + offset);

        if (rnd.value(0, 1) < t_x) {
          p.moveTo(endPoint);
          continue;
        }

        // if resulting end point `line.points[1]` is outside the bounds of the circle, skip this line
        if (hypot(endPoint.x - center.x, endPoint.y - center.y) > radius) {
          p.moveTo(endPoint);
          continue;
        }

        p.lineTo(endPoint);
      }
      group.path(p);
    }
  });

  return () => {
    seed = randomSeed();
  };
});

// create a "grid" (with jitter") of little rounded rectangles
function halftoneStipple(rnd: Random, colors: ColorSequence) {
  const baseWidth = config.width * 0.004;
  const baseHeight = config.height * 0.004;
  const borderRadius = config.width * 0.00175;
  return (g: Group) => {
    for (const [{ x, y }] of grid({
      xMin: 0,
      xMax: config.width,
      yMin: 0,
      yMax: config.height,
      xStep: baseWidth * 1.5,
      yStep: baseHeight * 1.5,
    })) {
      const pos = vec2(x, y).jitter(baseWidth * 0.1, rnd.rng);
      g.addChild(
        rect((r) => {
          r.x = pos.x;
          r.y = pos.y;
          r.width = rnd.jitter(baseWidth * 0.5, baseWidth);
          r.height = rnd.jitter(baseHeight * 0.5, baseHeight);
          r.borderRadius = borderRadius;

          let color = colors.at(map(0, config.height, 0, 1, pos.y));
          r.fill = color.opacify(0.6);
        }),
      );
    }
  };
}

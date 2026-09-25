import {
  renderSvg,
  randomSeed,
  PHI,
  clamp,
  shuffle,
  ColorSequence,
  Random,
} from "@salamivg/core";

const config = { width: 800, height: 800, scale: 1, loopCount: 1 };

/** height of each thin rectangle, in pixels */
const RECT_HEIGHT = 5;
/** how many times the remaining canvas is subdivided at the golden ratio */
const RECURSIONS = 10;
/** max random offset added to the color-sequence index for each rectangle */
const COLOR_INDEX_JITTER = 0.22;

const sunrisePalette = [
  "B6E4EA",
  "FFCB52",
  "FF9065",
  "FFBCAF",
  "D8B5EA",
  // "#ef7657",
  // "#f7a34e",
  // "#fbc95c",
  // "#fce7a1",
  ].map(h => `#${h}`);

let seed = randomSeed();

renderSvg(config, (svg) => {
  const rnd = Random.create(seed);
  svg.filenameMetadata = { seed: String(seed) };
  svg.numericPrecision = 3;

  svg.setBackground(sunrisePalette[0]);

  let cursorX = 0;
  let remaining = config.width;

  for (let i = 0; i < RECURSIONS; i++) {
    const panelWidth = remaining * (1 - (1 / PHI));
    const colorSequence = ColorSequence.fromColors(
      rnd.shuffle(sunrisePalette)
    );

    const rectCount = Math.ceil(config.height / RECT_HEIGHT);
    for (let k = 0; k < rectCount; k++) {
      const rectY = k * RECT_HEIGHT;
      const height = Math.min(RECT_HEIGHT, config.height - rectY);
      const t = (rectY + height / 2) / config.height;
      const index = clamp(0, 1, t + rnd.value(-COLOR_INDEX_JITTER, COLOR_INDEX_JITTER));

      svg.rect((r) => {
        r.x = cursorX;
        r.y = rectY;
        r.width = panelWidth;
        r.height = height;
        r.fill = colorSequence.at(index);
      });
    }

    cursorX += panelWidth;
    remaining -= panelWidth;
  }

  return () => {
    seed = randomSeed();
  };
});

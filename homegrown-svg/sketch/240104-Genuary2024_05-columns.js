/**
 * Genuary 2024, Day 5
 * https://genuary.art/prompts
 * 
 * """
 * In the style of Vera Molnár (1924-2023).
 *
 * Wikipedia: https://en.wikipedia.org/wiki/Vera_Moln%C3%A1r
 * """
 * 
 * This one is inspired by Vera Molnar, Lignes ou formes (“Lines or shapes”), 1983, e.g. https://assets-global.website-files.com/61e6c06a23cb13bf76051194/6303ac4fe078e95399392d51_Screen%20Shot%202022-08-22%20at%2018.15.38.png
 */
import { map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, jitter, cos } from 'artsvg'
import { renderSvg } from 'artsvg/render'

const config = {
  width: 100,
  height: 100,
  scale: 10,
  loopCount: 1,
}

let seed = randomSeed()
seed = 205108194658611

/**
 * Rules
 * 1. ~9 columns are generated across a central portion of the canvas, with the height randomly chosen at around (height * 0.25)
 * 2. Column length is chosen randomly at approximately (height * 0.4)
 * 3. Column angle is chosen randomly around PI/2 +/- (some amount)
 * 4. Thin semi-transparent rectangles are drawn along "column" at the given angle
 * 5. At a point randomly chosen for each column between 1/2 and 3/4 length, the column center position becomes perterbated by a sine wave
 * 6. The sine wave's amplitude, frequency, and phase are all randomly chosen in a set range
 * 7. Repeat steps 1-6 about 3 times, moving the height down by some amount on each repetition
 */

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)
  svg.setBackground('#f5f1eb')

  svg.stroke = null
  const rectWidth = svg.width / 10
  const rectHeight = svg.height / 200
  const baseHue = 230
  const baseSat = 0.48
  const baseLight = 0.68

  // Rule 7
  const nRows = 3
  for (const i of range(0, nRows)) {
    // Rule 1
    const columnCount = randomInt(6, 10, rng) 
    for (const c of range(0, columnCount)) {
      svg.fill = hsl(
        jitter(10, baseHue, rng),
        jitter(0.1, baseSat, rng),
        jitter(0.1, baseLight, rng),
        0.65
      )
      // Rule 1, part 2
      const baseY = jitter(svg.height * 0.05, map(0, nRows, svg.height * 0.125, svg.height * 0.7, i), rng)
      const baseX = jitter(svg.width * 0.04, map(0, columnCount, svg.width * 0.125, svg.width * 0.875, c), rng)
      // Rule 3
      const angle = random(Math.PI / 2 - 0.01, Math.PI / 2 + 0.01, rng)
      // Rule 2
      const length = randomInt(svg.height * 0.4, svg.height * 0.6, rng)
      // Rule 6
      const sine = createSine(svg, rng)
      const sineInfluencePos = randomInt(length * 0.5, length * 0.75, rng)
      for (const j of range(0, length)) {
        const y = baseY + j * (rectHeight * 1.4)
        let x = baseX + cos(angle) * j
        if (j > sineInfluencePos) {
          // Rule 5
          x += sine(j - sineInfluencePos) * map(sineInfluencePos, length, 0, 1, j)
        }
        svg.rect({ x, y, width: rectWidth, height: rectHeight })
      }
    }
  }

  svg.fill = null
  svg.stroke = hsl(230, 0.48, 0.68, 1)
  svg.strokeWidth = 0.5
  const padding = 0.03
  svg.rect({ x: svg.width * padding, y: svg.height * padding, width: svg.width * (1 - padding * 2), height: svg.height * (1 - padding * 2) })

  return () => {
    seed = randomSeed()
  }
})

function createSine(svg, rng) {
  const amplitude = random(svg.width / 15, svg.width / 30, rng)
  const period = 2 * Math.PI / random(svg.height * 0.99, svg.height * 0.25, rng)
  const phase = random(0, 1, rng) < 0.5 ? 0 : Math.PI
  return (n) => {
    return amplitude * Math.sin(period * n + phase)
  }
}
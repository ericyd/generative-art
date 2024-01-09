/**
 * Noise heatmap, mostly for testing noise fn distribution
 */
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Oscillator } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 6,
  loopCount: 1,
}

let seed = randomSeed()
seed = 1526067440858517
seed = 5690597348582443
seed = 6709437957283031

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }

  svg.fill = null
  svg.stroke = null

  const noise = createOscNoise(seed)

  const scale = 0.100
  for (const [{ x, y }] of new Grid({ columnCount: svg.height, rowCount: svg.width })) {
    svg.rect({ x, y, width: 1, height: 1, fill: hsl(0, 0, map(-1, 1, 0, 1, noise(x * scale, y * scale))) })
  }

  return () => {
    seed = randomSeed()
  }
})

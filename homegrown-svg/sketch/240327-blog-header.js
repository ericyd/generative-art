// just making a header bg image for my blog
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle } from '@salamivg/core'

const boxSize = 18
const strokeWidth = 3
const config = {
  width: (boxSize + strokeWidth) * 15,
  height: (boxSize + strokeWidth) * 6,
  scale: 1,
  loopCount: 10,
}

let seed = randomSeed() // 2550571049143217

const colors = [
  hsl(261.0, 0.45, 0.43),
  hsl(255.0, 0.46, 0.86),
  hsl(29.0, 0.93, 0.83),
  hsl(194.0, 0.70, 0.85),
  hsl(255.0, 0.46, 0.86), // oops, dupe
  hsl(212.0, 0.67, 0.30),
]

renderSvg(config, (svg) => {
  const rng = createRng(seed)
  svg.filenameMetadata = { seed }
  svg.numericPrecision = 3
  svg.fill = null
  svg.stroke = null

  for (const [{ x, y }] of new Grid({ xMin: 0, xMax: svg.width, yMin: 0, yMax: svg.height, xStep: boxSize + strokeWidth, yStep: boxSize + strokeWidth })) {
    svg.rect(rect({ x, y, width: boxSize, height: boxSize, fill: randomFromArray(colors, rng) }))
  }

  return () => { seed = randomSeed() }
})

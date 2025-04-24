import { renderSvg, map, vec2, randomSeed, createRng, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle, Svg, ColorHsl, grid, Path, Vector3, Vector2, array } from '@salamivg/core'

const config = {
  width: 800,
  height: 800,
  scale: 1,
  loopCount: 1,
  // openEveryFrame: false
}

let seed = randomSeed()

const colors = [
  'D0513A',
  '8BA8F7',
  '787379',
  'F1E6D2',
  'E88A53'
]

renderSvg(config, (svg) => {
  const rng = createRng(seed)
  svg.filenameMetadata = { seed: String(seed) }
  svg.numericPrecision = 3
  svg.setBackground('#000')
  svg.setAttributes({'stroke-linecap': 'round' })

  const gradient = ColorSequence.fromColors(shuffle(colors, rng).map(ColorRgb.fromHex))
  const noise = createOscNoise(seed, 0.025)
  const circleSize = 3
  const stepSize = circleSize * 2

  const g = grid({ xMin: svg.width * 0.2, xMax: svg.width * 0.8, yMin: 0, yMax: svg.height, xStep: stepSize, yStep: stepSize })
  for (const [{ x, y }] of g) {
    svg.circle(circle({
      stroke: gradient.at(y / svg.height + noise(x, 1)),
      fill: 'none',
      x,
      y,
      radius: 5.
    }))
  }

  return () => { seed = randomSeed(); }
})

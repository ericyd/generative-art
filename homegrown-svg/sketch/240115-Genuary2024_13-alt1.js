/**
 * Genuary 2024, Day 10
 * https://genuary.art/prompts
 *
 * """
 * JAN. 10 (credit: greweb)
 *
 * Hexagonal.
 * """
 */
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 10,
  loopCount: 10,
}

let seed = randomSeed()

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)
  const grad = svg.defineLinearGradient({
    colors: [
      '#eaceed',
      '#f7cbad',
    ],
  })
  svg.setBackground(grad)

  svg.numericPrecision = 3
  const angleMin = random(-PI, PI, rng)
  const noiseFn = createOscCurl(seed)
  const noiseFn2 = createOscNoise(seed)

  const spectrum = new ColorSequence([
    [0.1, ColorRgb.fromHex('#3C73A3')],
    [0.3, ColorRgb.fromHex('#7FB4C9')],
    [0.7, ColorRgb.fromHex('#8351AF')],
    [0.9, ColorRgb.fromHex('#2A4593')]
  ])

  svg.fill = null
  svg.stroke = ColorRgb.Black
  svg.strokeWidth = 0.2


  const nPoints = 1000
  // this is clumsy but works for now
  const points = new Array(nPoints).fill(0)
    .map(() => Vector2.random(0, svg.width, 0, svg.height, rng))

  const scale = random(0.02, 0.04, rng)
  const visited = []
  const padding = 0.85
 
  for (const startPoint of points) {
    let cursor = startPoint.jitter(0.3, rng)
    const color = spectrum.at(random(0, 1, rng))

    for (let i = 0; i < 100; i++) {
      const angle = map(-1, 1, angleMin, angleMin + TAU, noiseFn2(cursor.x * scale, cursor.y * scale))
      const vec = noiseFn(cursor.x * scale, cursor.y * scale).add(vec2(cos(angle), sin(angle)))
      const next = cursor.add(vec)
      if (!nearAnyPoint(next, visited, padding)) {
        svg.circle({ x: next.x, y: next.y, r: padding / 3, fill: color, stroke: color })
        visited.push(next)
      }
      cursor = next
    }
  }

  return () => {
    seed = randomSeed()
  }
})

function nearAnyPoint(point, others, padding = 0.1) {
  for (const other of others) {
    if (point.distanceTo(other) < padding) {
      return true
    }
  }
  return false
}

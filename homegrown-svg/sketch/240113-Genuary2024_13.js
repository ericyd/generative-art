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
  loopCount: 1,
}

let seed = randomSeed()
seed = 3831459812932491

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)
  const bg = ColorRgb.fromHex('#E9EFF2')
  svg.setBackground(bg)

  svg.numericPrecision = 3
  const noiseFn = createOscCurl(seed)

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

  const scale = random(0.03, 0.06, rng)
  const visited = []
  const padding = 0.55
 
  for (const startPoint of points) {
    let cursor = startPoint.jitter(0.3, rng)
    const color = spectrum.at(random(0, 1, rng))

    for (let i = 0; i < 100; i++) {
      const vec = noiseFn(cursor.x * scale, cursor.y * scale)
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

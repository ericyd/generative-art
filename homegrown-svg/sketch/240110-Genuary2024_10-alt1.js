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
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 6,
  loopCount: 1,
}

let seed = randomSeed()
// seed = 2273104791494449
// seed = 795629943372339
seed = 2469707569356721

/**
 * Rules
 * 0. Draw a flow field with non-overlapping points
 * 1. a hexagon is placed randomly on the canvas
 * 2. the hexagon grid grows semi-randomly
 * 3. the flow field is rendered inside of the hexagons.
 */

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)
  svg.setBackground('#fff')

  svg.fill = null
  svg.stroke = ColorRgb.Black
  svg.strokeWidth = 0.25
  svg.numericPrecision = 3
  const center = vec2(svg.width, svg.height).div(2)

  const angleMin = random(-PI, PI, rng)
  const hexCenter = center.add(vec2(cos(angleMin), sin(angleMin)).multiply(random(center.length() * 0.35, center.length() * 0.45, rng)))
  const hexagons = [new Hexagon({ center: hexCenter, circumradius: hypot(svg.width, svg.height) * 0.1 })]
  let hexagon = hexagons[0]
  for (let i = 0; i < 20; i++) {
    const hex = randomFromArray(hexagon.neighbors(), rng)
    hexagons.push(hex)
    hexagon = hex
  }

  const nPoints = 1000
  // this is clumsy but works for now
  const points = new Array(nPoints).fill(0)
    .map(() => Vector2.random(0, svg.width, 0, svg.height, rng))

  const noise = createOscNoise(seed)
  const scale = random(0.04, 0.08, rng)
  const visited = []
  const paths = points.map(point => {
    const p = polyline(path => {
      path.push(point)
      for (let i = 0; i < 400; i++) {
        const angle = map(-1, 1, angleMin, angleMin + TAU, noise(path.cursor.x * scale, path.cursor.y * scale))
        const next = path.cursor.add(vec2(cos(angle), sin(angle)))
        if (nearAnyPoint(next, visited, 0.4)) {
          break
        }
        path.push(next)
      }
    })
    visited.push(...p.points)
    return p
  })

  for (const hex of hexagons) {
    svg.polygon(hex)
    for (const path of paths) {
      const points = path.points.filter(point => hex.contains(point))
      if (points.length > 0) {
        svg.polyline({
          points,
          stroke: ColorRgb.Black,
          strokeWidth: 0.1,
        })
      }
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

function insideAnyHexagon(point, hexagons) {
  for (const hex of hexagons) {
    if (hex.contains(point)) {
      return true
    }
  }
  return false
}

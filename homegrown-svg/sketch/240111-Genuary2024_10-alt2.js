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
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 6,
  loopCount: 10,
}

let seed = randomSeed()
// seed = 2273104791494449
// seed = 795629943372339
// seed = 2469707569356721

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

  svg.numericPrecision = 3
  const center = vec2(svg.width, svg.height).div(2)
  const noise = createOscNoise(seed)

  const angleMin = random(-PI, PI, rng)
  const hexCenter = center.add(vec2(cos(angleMin), sin(angleMin)).multiply(random(center.length() * 0.35, center.length() * 0.45, rng)))
  let hexagons = [new Hexagon({ center: hexCenter, circumradius: hypot(svg.width, svg.height) * 0.07 })]
  let hexagon = hexagons[0]
  for (let i = 0; i < 40; i++) {
    const hex = randomFromArray(hexagon.neighbors(), rng)
    hexagons.push(hex)
    hexagon = hex
  }
  hexagons = hexagons.filter(() => random(0, 1, rng) < 0.75)
  svg.fill = ColorRgb.Black
  svg.stroke = ColorRgb.White
  svg.strokeWidth = 0.7
  hexagons.forEach(hex => svg.polygon(hex))
  svg.fill = ColorRgb.White // ok this was NOT expected but this actually looks pretty cool
  svg.fill = null // this is what I meant to do
  // svg.stroke = ColorRgb.Black // goes well with "fill: white"
  svg.strokeWidth = 0.2
  

  const nPoints = 3000
  // this is clumsy but works for now
  const points = new Array(nPoints).fill(0)
    .map(() => Vector2.random(0, svg.width, 0, svg.height, rng))
  // const points = new Grid({
  //   xMin: svg.width * -0.2,
  //   xMax: svg.width * 1.2,
  //   xStep: 1.5,
  //   yMin: svg.height * -0.2,
  //   yMax: svg.height * 1.2,
  //   yStep: 1.5,
  // })

  const scale = random(0.03, 0.06, rng)
  const visited = []
 
  for (const startPoint of points) { // use this version with random points
  // for (const [startPoint] of points) { // use this version with "grid"
    let cursor = startPoint.jitter(0.3, rng)
    let line = new Polyline()

    for (let i = 0; i < 100; i++) {
      const angle = map(-1, 1, angleMin, angleMin + TAU, noise(cursor.x * scale, cursor.y * scale))
      const next = cursor.add(vec2(cos(angle), sin(angle)))
      if (nearAnyPoint(next, visited, 0.55)) {
        if (!line.empty()) {
          svg.polyline(line)
        }
        visited.push(...line.points)
        line = new Polyline()
        // continue // necessary??
      }
      // if the point is inside a hexagon, add it to the polyline.
      // otherwise, draw the polyline (if non-empty) and reset
      else if (insideAnyHexagon(next, hexagons)) {
        line.push(next)
      } else {
        if (!line.empty()) {
          svg.polyline(line)
        }
        visited.push(...line.points)
        line = new Polyline()
      }
      cursor = next
    }
    visited.push(...line.points)
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

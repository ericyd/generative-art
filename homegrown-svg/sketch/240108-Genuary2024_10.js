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
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 6,
  loopCount: 1,
}

let seed = randomSeed()

/**
 * Rules
 * 1. a hexagon is placed randomly on the canvas
 * 2. a bunch of points are placed inside the hexagon
 * 3. each point moves according to a vector/flow field.
 *    - movements are quantized to PI/3 (i.e. hexagonal moves)
 * 4. when a path moves outside of the hexagon, it triggers a new hexagon to be created.
 *    - new hexagons will be created in a hex grid layout
 */

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)
  svg.setBackground('#fff')

  svg.fill = null
  svg.stroke = ColorRgb.Black
  svg.strokeWidth = 0.25
  const center = vec2(svg.width, svg.height).div(2)

  const angleMin = random(-PI, PI, rng)
  const hexCenter = center.add(vec2(cos(angleMin), sin(angleMin)).multiply(random(center.length() * 0.5, center.length() * 0.9, rng)))
  const hexagons = [new Hexagon({ center: hexCenter, circumradius: hypot(svg.width, svg.height) * 0.1 })]
  svg.polygon(hexagons[0])

  for (let i = 0; i < 3; i++) {
    const startHex = randomFromArray(hexagons, rng)
    const nPoints = 100
    // this is clumsy but works for now
    const points = new Array(nPoints).fill(0)
      .map(() =>
        Vector2.random(
          startHex.boundingBox.x,
          startHex.boundingBox.x + startHex.boundingBox.width,
          startHex.boundingBox.y,
          startHex.boundingBox.y + startHex.boundingBox.height,
          rng
        )
      ).filter(point => startHex.contains(point))
  
    const noise = createOscNoise(seed)
    const scale = random(0.05, 0.13, rng)
    const visited = []
    for (const point of points) {
      const pathVisited = []
      svg.path(path => {
        path.moveTo(point)
        for (let i = 0; i < 1000; i++) {
          const angle = map(-1, 1, angleMin, angleMin + TAU, noise(path.cursor.x * scale, path.cursor.y * scale))
          const next = path.cursor.add(vec2(cos(angle), sin(angle)))
          if (nearAnyPoint(next, visited, 0.4)) {
            break
          }
          path.lineTo(next, 'absolute')
  
          // add new hexagons as needed
          if (!insideAnyHexagon(path.cursor, hexagons)) {
            for (let i = 0; i < hexagons.length; i++) {
              for (const hex of hexagons[i].neighbors()) {
                if (hex.contains(path.cursor)) {
                  hexagons.push(hex)
                  svg.polygon(hex)
                  i = hexagons.length
                  break
                }
              }
            }
          }
          pathVisited.push(path.cursor)
        }
      })
      visited.push(...pathVisited)
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

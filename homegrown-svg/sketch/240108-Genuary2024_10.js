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
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise } from '@salamivg/core'

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

  const center = Vector2.random(
    svg.width * 0.2,
    svg.width * 0.8,
    svg.height * 0.2,
    svg.height * 0.8,
    rng,
  )
  // circumradius is radius of circumcircle
  // apothem is radius of inscribed circle
  const circumradius = hypot(svg.width, svg.height) * 0.1
  const hexagon = new Polygon({
    points: range(0, 6)
      .map((i) => (Math.PI / 3) * i)
      .map((angle) =>
        center.add(vec2(cos(angle), sin(angle)).multiply(circumradius)),
      ),
  })

  svg.polygon(hexagon)

  const nPoints = 100
  // this is clumsy but works for now
  const points = new Array(nPoints).fill(0).map(() =>
    Vector2.random(hexagon.boundingBox.x, hexagon.boundingBox.x + hexagon.boundingBox.width, hexagon.boundingBox.y, hexagon.boundingBox.y + hexagon.boundingBox.height))

  const noise = createOscNoise(seed)
  for (const point of points) {
    svg.path(path => {
      path.moveTo(point)
      for (let i = 0; i < 100; i++) {
        const angle = map(-1, 1, -PI, PI, noise(path.cursor.x / 5, path.cursor.y / 5))
        console.log(noise(path.cursor.x / 10, path.cursor.y / 10))
        path.lineTo(vec2(cos(angle), sin(angle)), 'relative')
      }
    })
  }

  return () => {
    seed = randomSeed()
  }
})

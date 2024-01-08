/**
 * Genuary 2024, Day 8
 * https://genuary.art/prompts
 * 
 * """
 * JAN. 8 (credit: Darien Brito)
 *
 * Chaotic system.
 * """
 */
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 6,
  loopCount: 5,
}

let seed = randomSeed()

/**
 * Rules
 * 1. 6-40 lines begin at the bottom of the canvas
 * 2. Line motion is defined by cubic bezier curves
 * 3. The endpoint of the cubic bezier is always on the primary line, at a fixed distance from the start point
 * 4. The two control points are placed according to the following rules
 *    a. the first control point is placed relative to the start point.
 *    b. the distance from the start point starts very small and increases as the line approaches the top
 *    c. the angle from the start point is chosen randomly from a range.
 *       The range starts at [-PI/2, -PI/2] (i.e. straight up)
 *       and gradually becomes larger until at the very top, the range is [-3*PI/2, PI/2]
 *    d. the second control point follows the same rules, but it is relative to the end point,
 *       and the angle range is inverted, i.e. starts at [PI/2, PI/2] and ends at [3*PI/2, -PI/2]
 */

// this one goes top to bottom, I wanted bottom to top
renderSvg.skip(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)

  svg.fill = null
  svg.stroke = ColorRgb.Black
  svg.strokeWidth = 0.15

  const nLines = randomInt(6, 40, rng)
  for (const i of range(0, nLines)) {
    const x = map(0, nLines, 0, svg.width, i)
    const stepsize = svg.height / 50
    svg.path(path => {
      path.moveTo(vec2(x, 0))
      for (const y of range(stepsize, svg.height, stepsize)) {
        const start = path.cursor
        const end = vec2(x, y) // alternative, top to bottom
        const length = end.subtract(start).length()

        const control1Angle = random(
          map(0, svg.height, -PI/2, -3*PI/2, y),
          map(0, svg.height, -PI/2, PI/2, y),
          rng
        )
        const control1Length = random(
          map(0, svg.height, length * 0.1, length * 5, y),
          map(0, svg.height, length * 0.2, length * 10, y),
          rng
        )
        const control1 = start.add(vec2(cos(control1Angle), sin(control1Angle)).multiply(control1Length))

        const control2Angle = random(
          map(0, svg.height, PI/2, 3*PI/2, y),
          map(0, svg.height, PI/2, -PI/2, y),
          rng
        )
        const control2Length = random(
          map(0, svg.height, length * 0.1, length * 5, y),
          map(0, svg.height, length * 0.2, length * 10, y),
          rng
        )
        const control2 = end.add(vec2(cos(control2Angle), sin(control2Angle)).multiply(control2Length))
        path.cubicBezier(control1, control2, end)
      }
    })
  }

  return () => {
    seed = randomSeed()
  }
})


renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)

  svg.fill = null
  svg.stroke = ColorRgb.Black
  svg.strokeWidth = 0.15

  const nLines = randomInt(6, 40, rng)
  for (const i of range(0, nLines)) {
    const x = map(0, nLines, 0, svg.width, i)
    const stepsize = svg.height / 50
    svg.path(path => {
      path.moveTo(vec2(x, svg.height))
      for (const y of range(stepsize, svg.height, stepsize)) {
        const start = path.cursor
        const end = vec2(x, svg.height - y)
        const length = end.subtract(start).length()

        const control1Angle = random(
          map(svg.height, 0, -PI/2, -3*PI/2, svg.height - y),
          map(svg.height, 0, -PI/2, PI/2, svg.height - y),
          rng
        )
        const control2Angle = random(
          map(svg.height, 0, PI/2, 3*PI/2, svg.height - y),
          map(svg.height, 0, PI/2, -PI/2, svg.height - y),
          rng
        )
        const control1Length = random(
          map(svg.height, 0, length * 0.1, length * 5, svg.height - y),
          map(svg.height, 0, length * 0.2, length * 10, svg.height - y),
          rng
        )

        // swapping control angles makes this more "loopy", though it doesn't fit my original rule set
        // I think there's room for improvement here
        const control1 = start.add(vec2(cos(control2Angle), sin(control2Angle)).multiply(control1Length))

        
        const control2Length = random(
          map(svg.height, 0, length * 0.1, length * 5, svg.height - y),
          map(svg.height, 0, length * 0.2, length * 10, svg.height - y),
          rng
        )
        const control2 = end.add(vec2(cos(control1Angle), sin(control1Angle)).multiply(control2Length))
        path.cubicBezier(control1, control2, end)
      }
    })
  }

  return () => {
    seed = randomSeed()
  }
})

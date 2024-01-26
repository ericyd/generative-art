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
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 10,
  loopCount: 1,
}

let seed = randomSeed()
seed = 1546416152405953

const colors = [
  '#B9D6F5',
  '#D09AEA',
  '#CB769E',
  '#E09891',
  '#E8B9AB',
  '#C2E2B2'
]

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

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)
  svg.setBackground('#28466C')

  svg.fill = null
  svg.stroke = ColorRgb.Black
  svg.strokeWidth = 0.5

  const spectrum = ColorSequence.fromColors(shuffle(colors, rng))

  const nLines = randomInt(15, 25, rng)
  for (const i of range(0, nLines)) {
    const x = map(0, nLines, svg.width / nLines, svg.width, i)
    const stepsize = svg.height / 50
    svg.path(path => {
      path.stroke = spectrum.at(random(0, 1, rng))
      path.moveTo(vec2(x, svg.height))
      for (const y of range(stepsize, svg.height, stepsize)) {
        const start = path.cursor
        const end = vec2(x, svg.height - y)
        const length = end.subtract(start).length()

        const control1Angle = random(-3*PI/2, PI/2, rng)
        const control2Angle = random(3*PI/2, -PI/2, rng)
        const control1Length = clamp(0, 100, random(
          map(svg.height * 0.7, 0, length * 0.1, length * 5, svg.height - y),
          map(svg.height * 0.7, 0, length * 0.2, length * 10, svg.height - y),
          rng
        ))
        const control2Length = clamp(0, 100, random(
          map(svg.height * 0.85, 0, length * 0.1, length * 5, svg.height - y),
          map(svg.height * 0.85, 0, length * 0.2, length * 10, svg.height - y),
          rng
        ))

        // swapping control angles makes this more "loopy", though it doesn't fit my original rule set
        // I think there's room for improvement here
        const control1 = start.add(vec2(cos(control2Angle), sin(control2Angle)).multiply(control1Length))
        const control2 = end.add(vec2(cos(control1Angle), sin(control1Angle)).multiply(control2Length))
        path.cubicBezier(control1, control2, end)
      }
    })
  }

  return () => {
    seed = randomSeed()
  }
})

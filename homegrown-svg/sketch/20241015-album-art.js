// just making a header bg image for my blog
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle, Svg, ColorHsl } from '@salamivg/core'

const strokeWidth = 3
const config = {
  width: 800,
  height: 800,
  scale: 1,
  loopCount: 1,
}

let seed = randomSeed()

const colors = [
  ColorRgb.fromHex('785A96').toHsl(),
  ColorRgb.fromHex('E4BF70').toHsl(),
  ColorRgb.fromHex('B2C566').toHsl(),
  ColorRgb.fromHex('6887A1').toHsl(),
]

const bg = '#332C2B'

/**
 * props: l = left, r = right, t = top
 */
class SplitHex {
  // rounded corners
  // https://stackoverflow.com/a/38118843
  // https://stackoverflow.com/a/32875327

  constructor(center, rng, circumradius = 100, strokeWidth = 5) {
    this.color = randomFromArray(colors, rng)
    this.l = polyline(p => {
      p.fill = this.color.mix(ColorRgb.fromHex('000000').toHsl(), 0.25)
      p.stroke = bg
      p.strokeWidth = strokeWidth
      p.push(center)
      for (let i = 0; i < 3; i++) {
        const angle = (Math.PI / 3) * i + PI/2
        p.push(center.add(
          Vector2.fromAngle(angle).scale(circumradius)
        ))
      }
      p.push(center)
    })

    this.r = polyline(p => {
      p.fill = this.color.mix(ColorRgb.fromHex('ffffff').toHsl(), 0.25)
      p.stroke = bg
      p.strokeWidth = strokeWidth
      p.push(center)
      for (let i = 0; i < 3; i++) {
        const angle = (Math.PI / 3) * i - PI/6
        p.push(center.add(
          Vector2.fromAngle(angle).scale(circumradius)
        ))
      }
      p.push(center)
    })

    this.t = polyline(p => {
      p.fill = this.color
      p.stroke = bg
      p.strokeWidth = strokeWidth
      p.push(center)
      for (let i = 0; i < 3; i++) {
        const angle = (Math.PI / 3) * i + PI*7/6
        p.push(center.add(
          Vector2.fromAngle(angle).scale(circumradius)
        ))
      }
      p.push(center)
    })
  }

  draw(svg) {
    svg.polyline(this.l)
    svg.polyline(this.r)
    svg.polyline(this.t)
  }
}

renderSvg(config, (svg) => {
  const rng = createRng(seed)
  svg.filenameMetadata = { seed }
  svg.numericPrecision = 3
  svg.fill = bg
  svg.stroke = bg
  svg.setBackground(bg)
  svg.setAttributes({'stroke-linecap': 'round'})

  const n = 5
  const strokeWidth = 4
  const xStart = config.width * 0.15
  const xEnd = config.width * 0.85
  const xRange = xEnd - xStart
  const apothem = xRange / (n - 1) / 2 // trial-and-error 🤷
  const circumradius = (apothem * 2) / Math.sqrt(3) // from salamivg Hexagon.js
  for (let i = 0; i < n; i++) {
    const x = map(0, n-1, xStart, xEnd, i)
    new SplitHex(vec2(x, config.height/2), rng, circumradius, strokeWidth).draw(svg)
  }

  for (let i = 0; i < n-1; i++) {
    // offset x by "apothem" so they are staggered appopriately
    const x = map(0, n-1, xStart + apothem, xEnd + apothem, i)
    // offset y by "circumradius * 1.5" so the edges line up
    new SplitHex(vec2(x, config.height/2 - circumradius * 1.5), rng, circumradius, strokeWidth).draw(svg)
  }

  for (let i = 0; i < n-1; i++) {
    const x = map(0, n-1, xStart + apothem, xEnd + apothem, i)
    new SplitHex(vec2(x, config.height/2 + circumradius * 1.5), rng, circumradius, strokeWidth).draw(svg)
  }

  /**
   * Remaining work:
   * - I want a line or sequence of points which "snakes through" the hex grid
   * - Rules for the "snake":
   *    1. Cannot visit a "side" (l, r, t) more than once
   *    2. Can only travel to "adjacent" sides of neighbors, or unoccupied sides of self
   *    3. Adjacent sides of neighbors are defined as
   *      - top is adjacent to left and right of neighbor on top
   *      - left is adjacent to right of left neighbor, and top of bottom neighbor
   *      - right is adjacent to left of right neighbor, and top of bottom neighbor
   *    4. Snake enters a side perpendicular to it's edge, accounting for perspective
   *    5. Snake can change angles anywhere inside of the side/face that it is currently in
   *      - Snake can also go straight, e.g. from left to right of same SplitHex
   *    6. Snake should try to touch all hexes
   *    7. Snake enters from boundary of sketch, and moves straight until it hits the edge of the first hex
   * 
   * - I'm not exactly sure how to structure this or code the logic, needs more thought.
   */


  return () => { seed = randomSeed() }
})

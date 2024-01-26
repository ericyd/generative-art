/**
 * Genuary 2024, Day 27
 * https://genuary.art/prompts
 *
 * """
 * JAN. 27 (credit: Amy Goodchild)
 *
 * Code for one hour. At the one hour mark, you’re done.
 * """
 * 
 * 29 minutes spent coding! 💥
 * However, probably spent 30-60 minutes thinking about it beforehand 🤣
 */
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 10,
  loopCount: 10,
}

let seed = randomSeed()
// wow this algo is pretty dynamic!
seed = 8852037180828291 // probably the best
// seed = 7640603347606109 // slick
// seed = 2643689140155119 // cool
// seed = 3226440218479705 // nice
// seed = 6209898827657631 // sweet
// seed = 3491113169519419 // dope

const bg = '#2E4163'

const colors = [
  '#974F7A',
  '#D093C2',
  '#6F9EB3',
  '#E5AD5A',
  '#EEDA76',
  '#B5CE8D',
  '#DAE7E8',
  '#2E4163',
]

/*
Rules

1. Draw an equilateral triangle
2. Subdivide the triangle into 4 equal-sized smaller triangles
3. If less than max depth and <chance>, continue recursively subdividing
4. Each triangle gets a different fun-colored fill
*/

renderSvg(config, (svg) => {
  const rng = createRng(seed)
  const maxDepth = randomInt(5, 9, rng)
  svg.filenameMetadata = { seed, maxDepth }
  svg.setBackground(bg)
  svg.numericPrecision = 3
  svg.fill = null
  svg.stroke = null
  svg.strokeWidth = 0.25
  const spectrum = ColorSequence.fromColors(shuffle(colors, rng))

  function drawTriangle(a,b,c, depth = 0) {
    if (depth === 0 || random(0, 1, rng) < 0.5) {
      const offsetAmount = depth / 2 // looks cool when offset increases with depth
      const offset = vec2(random(-offsetAmount, offsetAmount, rng), random(-offsetAmount, offsetAmount, rng))
      svg.polygon({
        points: [
          // super normie
          // a, b, c,

          // extremely chaotic
          // a.jitter(1, rng),
          // b.jitter(1, rng),
          // c.jitter(1, rng),

          // juuuuuuust right
          a.add(offset),
          b.add(offset),
          c.add(offset),
        ],
        fill: spectrum.at(random(0, 1, rng))
      })
    }
    if (depth < maxDepth && (depth < 2 || random(0, 1, rng) < 0.75)) {
      const ab = Vector2.mix(a, b, 0.5)
      const ac = Vector2.mix(a, c, 0.5)
      const bc = Vector2.mix(b, c, 0.5)
      drawTriangle(ab, ac, bc, depth + 1)
      drawTriangle(a, ab, ac, depth + 1)
      drawTriangle(b, bc, ab, depth + 1)
      drawTriangle(c, bc, ac, depth + 1)
    }
  }
  const angle = random(0, TAU, rng)
  const a = svg.center.add(Vector2.fromAngle(angle).scale(45))
  const b = svg.center.add(Vector2.fromAngle(angle + PI * 2 / 3).scale(45))
  const c = svg.center.add(Vector2.fromAngle(angle + PI * 4 / 3).scale(45))
  drawTriangle(a, b, c)

  return () => { seed = randomSeed() }  
})

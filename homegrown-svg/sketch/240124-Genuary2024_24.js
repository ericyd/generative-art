/**
 * Genuary 2024, Day 24
 * https://genuary.art/prompts
 *
 * """
 * JAN. 24 (credit: Jorge Ledezma)
 *
 * Impossible objects (undecided geometry).
 * """
 */
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 10,
  loopCount: 1,
}

let seed = randomSeed()

const colors = {
  dark: '#2E4163',
  light: '#DAE7E8',
  medium: '#B8C3CB',
}

/*
Rules

0. A perspective is chosen. It will be approximately at the vector (1, 1, 1) because I'm boring.
1. Stacks of "cards" are placed on one axis or along some vector
2. Stacks of "cards" are placed on an orthogonal axis
3. The two stacks kinda-intersect, but the spatial relationship at the intersection doesn't quite make sense
4. Do this more than once if possible
*/

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)
  svg.setBackground(colors.dark)
  svg.numericPrecision = 3
  svg.fill = colors.dark
  svg.stroke = colors.light
  svg.strokeWidth = 0.25

  const shortPerspective = 3
  const longPerspective = 10

  const polygons = []
  const cubes = []

  for (let x = svg.width * 0.1; x < svg.width * 0.53; x += 2) {
    const y = svg.height * 0.3
    if (x > svg.width * 0.5) {
      cubes.push(cube(x, y - shortPerspective, longPerspective, shortPerspective))
      x += longPerspective
    }
    polygons.push(diamond(x, y, shortPerspective, longPerspective))
  }

  let i = 0
  for (let y = svg.height * 0.1; y < svg.height * 0.8; y += 2) {
    const x = svg.width * 0.3
    console.log(y, svg.height * 0.5, svg.height * 0.52)
    if (y > svg.height * 0.5 && y < svg.height * 0.53) {
      cubes.push(cube(x, y, longPerspective, shortPerspective))
      y += longPerspective
    } else {
      polygons.splice(i, 0, diamond(x, y, longPerspective, shortPerspective))
    }
    i += 2
  }

  // svg.polygons(shuffle(polygons, rng))
  svg.polygons(polygons)
  svg.polygons(cubes)

  for (let x = svg.width * 0.6; x < svg.width * 0.8; x += 2) {
    const y = svg.height * 0.3
    svg.polygon(diamond(x, y, shortPerspective, longPerspective))
  }

  return () => { seed = randomSeed() }  
})

function diamond(x, y, xPerspective, yPerspective) {
  return new Polygon({
    points: [
      vec2(x - xPerspective, y), // left
      vec2(x, y - yPerspective), // top
      vec2(x + xPerspective, y), // right
      vec2(x, y + yPerspective), // bottom
    ] 
  })
}

// meh.... todo
// this only works in 1 orientation
function cube(x, y, xPerspective, yPerspective) {
  return new Polygon({
    points: [
      vec2(x - xPerspective, y), // (top) left
      vec2(x, y - yPerspective), // (top) top
      vec2(x + xPerspective, y), // (top) right
      vec2(x, y + yPerspective), // (top) bottom

      vec2(x, y + yPerspective + xPerspective), // (left) bottom
      vec2(x - xPerspective, y + xPerspective), // (left) left
      vec2(x - xPerspective, y), // (left) top
      vec2(x, y + yPerspective), // (left) right

      vec2(x, y + yPerspective + xPerspective), // (right) bottom
      vec2(x + xPerspective, y + xPerspective), // (right) right
      vec2(x + xPerspective, y), // (right) top

      // closing
      vec2(x, y + yPerspective), // (top) bottom
      vec2(x - xPerspective, y), // (top) left
    ] 
  })
}
// just making a header bg image for my blog
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle, Svg } from '@salamivg/core'

const strokeWidth = 3
const config = {
  width: 1000,
  height: 1000,
  scale: 1,
  loopCount: 1,
}

let seed = randomSeed()

// const colors = [
//   hsl(261.0, 0.45, 0.43),
//   hsl(255.0, 0.46, 0.86),
//   hsl(29.0, 0.93, 0.83),
//   hsl(194.0, 0.70, 0.85),
//   hsl(255.0, 0.46, 0.86), // oops, dupe
//   hsl(212.0, 0.67, 0.30),
// ]

const taken = []

renderSvg(config, (svg) => {
  const rng = createRng(seed)
  svg.filenameMetadata = { seed }
  svg.numericPrecision = 3
  svg.fill = fill()
  svg.stroke = stroke()
  svg.setBackground(fill())

  let centers = [vec2(svg.width * 0.95, svg.height * 0.95)]
  const treeDepth = 5
  for (let i = 0; i < treeDepth; i++) {
    const next = []
    for (const center of centers) {  
      const chance = random(0, 1, rng)
      // const chance = 0.9
      if (chance > 0.66) {
        genCircle(svg, center, rng)
      } else if (chance > 0.33) {
        genArc(svg, center, rng)
      } else {
        genSwirl(svg, center, rng)
      }

      next.push(...randomCentersFromSeed(center, rng, i))
    }
    centers = next
  }

  return () => { seed = randomSeed() }
})

/**
 * Try this rule!
 * 1. Point starts in bottom right corner
 * 2. Point generates 2 or 3 branches
 * 3. Each branch has a length of random(70, 90)
 * 4. Each branch has an angle between (PI * 0.9) and (PI * 1.6)
 * 5. Check for overlaps still
 * 
 * This ^^ is my Friday task
 * 
 * 
 * @param {Vector2} seed 
 * @param {Rng} rng 
 * @param {number} attempts 
 * @returns {Array<Vector2>}
 */
function randomCentersFromSeed(seed, rng, depth) {
  const padding = 80
  const n = randomInt(2, 4, rng)
  const centers = new Array(n).fill(null).map((_,i) => {
    let point = null
    for (let attempts = 0; attempts < 50; ++attempts) {
      const angle = random(PI * 0.9, PI * 1.6, rng)
      const distance = randomInt(padding, padding * 1.5, rng)
      point = seed.add(Vector2.fromAngle(angle).scale(distance))

      if (taken.some(t => t.distanceTo(point) < padding) || isOutOfBounds(point, config.width, config.height)) {
        point = null // must nullify so that we don't push invalid data to centers
        continue
      }
      taken.push(point)
      break
    }
    return point
  })
  console.log({c: centers.filter(Boolean).length, t: taken.length})
  return centers.filter(Boolean)
}

function isOutOfBounds(point, maxX, maxY) {
  return point.x < 0 || point.x > maxX || point.y < 0 || point.y > maxY
}

/**
 * @param {Svg} svg 
 * @param {Vector2} center
 * @param {Rng} rng 
 */
function genCircle(svg, center, rng) {
  const minRadius = 5
  let radius = randomInt(10, 60, rng)
  while (radius > minRadius) {
    svg.strokeWidth = strokeWidth
    svg.circle(circle({ ...center, radius, fill: fill(), stroke: stroke(), strokeWidth }))
    radius -= strokeWidth * 2
  }
}

/**
 * @param {Svg} svg
 * @param {Vector2} center
 * @param {Rng} rng
 */
function genArc(svg, center, rng) {
  const minRadius = 15

  const rotation = random(0, TAU, rng)
  const arcLength = random(PI / 4, PI, rng)
  let radius = randomInt(40, 70, rng)
  const starts = []
  const ends = []
  while (radius > minRadius) {
    const start = vec2(center.x + cos(rotation) * radius, center.y + sin(rotation) * radius)
    const end = vec2(center.x + cos(rotation + arcLength) * radius, center.y + sin(rotation + arcLength) * radius)
    starts.push(start)
    ends.push(end)

    // only fill if this isn't the "last" one
    const pathFill = radius - strokeWidth * 2 > minRadius ? fill() : null

    svg.path(p => {
      p.setAttributes({ "stroke-linecap": "round" })
      p.strokeWidth = strokeWidth
      p.moveTo(start)
      p.arc({
        rx: radius,
        ry: radius,
        largeArcFlag: false,
        sweepFlag: true,
        end
      })
      p.fill = pathFill;
      p.stroke = stroke();
    })
    radius -= strokeWidth * 2
  }

  // add "endpoint circles"
  // this can be more randomized but just trying to get a rough feel for now
  const meanStart = meanVector2(starts)
  genCircle(svg, meanStart, rng)
  const meanEnd = meanVector2(ends)
  genCircle(svg, meanEnd, rng)
}

/**
 * @param {Svg} svg
 * @param {Vector2} center
 * @param {Rng} rng
 */
function genSwirl(svg, center, rng) {
  const minRadius = 5
  const direction = random(0, 1, rng) > 0.5 ? 1 : -1
  const rotation = random(0, TAU, rng) * direction
  let radius = randomInt(20, 60, rng)
  const tailLength = radius * 2


  /**
   * we need to find the "cutoff" point for each circle
   * idea being: starting at the center, we will "swirl" off from the circle at the angle `rotation` and move tangent to the circle at that point.
   * The tangent should act as a hard cutoff point for all other rings of the circle -- the lines should not cross the inner most tangent.
   * (outer rings will draw their own tangents, but they will be on the outside of the inner tangent so won't affect other calculations.)
   * 
   * The easiest way to use this information is to identify the angle at which a given circle's boundary/circumference intersects the innermost tangent.
   * We can imagine a right triangle created from three points:
   * 1. the circle center
   * 2. the point at which the innermost tangent intersects the innermost circle
   * 3. the intersection point of the innermost tangent and the outer circle's circumference
   * 
   * When the angle of these three points is less than or equal to the radius of the outer circle, then we have our "cutoff" point.
   */
  let innermostRadius = radius;
  while (innermostRadius > (minRadius - strokeWidth * 2)) { innermostRadius -= strokeWidth * 2 }

  const starts = [] // probably not gonna use starts but whatever
  const ends = []
  let odd = false
  while (radius > minRadius) {
    const naturalStart = center.add(Vector2.fromAngle(rotation).scale(radius))
    const end = naturalStart.add(Vector2.fromAngle(rotation + (PI / 2) * direction).scale(tailLength))
    starts.push(naturalStart)
    ends.push(end)

    /**
     * calculate the start from the cutoff with the innermost circle - see note above
     * 
     * right triangle, short side is `innermostRadius`.
     * hypotenuse is `radius`.
     * big angle is unknown.
     * small angle is unknown.
     * long side is unknown.
     * 
     * we want the big angle.
     * 
     * the cosine of the big angle is `innermostRadius / radius`, so `arccos(innermostRadius / radius)` is the angle.
     * We also need to add the rotation of the swirl.
     */
    const startAngle = Math.acos(innermostRadius / radius) * direction + rotation
    const start = center.add(Vector2.fromAngle(startAngle).scale(radius))

    svg.path(p => {
      p.setAttributes({ "stroke-linecap": "round" })
      p.strokeWidth = strokeWidth
      p.moveTo(start)
      const angleResolution = 0.15 * direction
      const max = TAU * direction + rotation

      // only difference is `r < max` vs `r > max`.
      // I could extract this into a closure and call it every time but I don't think I care that much.
      if (direction > 0) {
        for (let r = startAngle; r < max; r += angleResolution) {
          p.lineTo(center.add(Vector2.fromAngle(r).scale(radius)))
        }
      } else {
        for (let r = startAngle; r > max; r += angleResolution) {
          p.lineTo(center.add(Vector2.fromAngle(r).scale(radius)))
        }
      }
      p.lineTo(end)
      p.fill = fill();
      p.stroke = odd ? stroke() : fill();
    })
    // radius only shrinks by "strokeWidth" so we can draw a "fill" line in between. This is unnecessary for individual swirls, but it helps if a swirl overlaps another swirl
    radius -= strokeWidth
    odd = !odd
  }

  // test marker for natural start point
  // svg.circle(circle({ center:naturalStart, radius: 2, fill: '#f90', stroke: '#f00', strokeWidth }))

  // add "endpoint circles"
  // this can be more randomized but just trying to get a rough feel for now
  const meanStart = meanVector2(starts)
  // genCircle(svg, meanStart, rng)
  const meanEnd = meanVector2(ends)
  // genCircle(svg, meanEnd, rng)
}

function fill() {
  return '#000'
}

function stroke(rng = Math.random) {
  return '#fff'
  // return hsl(randomInt(0, 360, rng), 0.5, 0.5)
}

function meanVector2(vectors) {
  return vectors.reduce((acc, v) => acc.add(v), vec2(0)).scale(1 / vectors.length)
}

/**
 * OK there are some glaring omissions from SalamiVG
 * 1. Circle should accept strokeWidth as an attribute directly, rather than requiring inheritance
 * 2. basic math functions should be included like mean, etc
 * 3. meanVector2 (probably Vector2.mean()) is brilliant! Include it, and maybe others
 */

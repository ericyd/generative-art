/**
 * Genuary 2024, Day 29
 * https://genuary.art/prompts
 *
 * """
 * JAN. 29 (credit: Melissa Wiederrecht & Camille Roux)
 * 
 * Signed Distance Functions (if we keep trying once per year, eventually we will be good at it!).
 * """
 *
 */
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 10,
  loopCount: 1,
}

let seed = randomSeed()

const bg = ColorRgb.White

renderSvg(config, (svg) => {
  const rng = createRng(seed)
  svg.filenameMetadata = { seed }
  svg.setBackground(bg)
  svg.numericPrecision = 3
  svg.fill = null
  svg.stroke = ColorRgb.Black
  svg.strokeWidth = 0.25

  for (let i = 0; i < 10000; i++) {
    const point = Vector2.random(0, svg.width, 0, svg.height, rng)
    const dist = sdf(point)

    const gradient = calculateGradient(dist, point)

    svg.lineSegment(new LineSegment(point, point.add(gradient.multiply(1))))

    // const lineWidth = 2
    // const color = Math.abs(dist) < lineWidth / 2 ? hsl(0, 0, 0.8) : dist < -(lineWidth/2) ? hsl(0, 0.8, map(-(lineWidth/2), -50, 1, 0, dist)) : hsl(220, 0.8, map((lineWidth/2), 50, 1, 0, dist))
    // svg.circle({ center: point, radius: 0.5, fill: color })
  }

  return () => { seed = randomSeed() }  
})

function sdf(point) {
  const circle = sdfCircle(point)
  const lineX = sdfLine(point.y - config.height / 2)
  return Math.min(circle, lineX)
}

function sdfCircle(point, center = vec2(config.width / 2, config.height / 2), radius = 20) {
  return point.subtract(center).length() - radius
}

function sdfLine(t) {
  return t
}

// remember, the **smaller** sdf points towards the shape
// This actually works!
// TODO: Try writing a more clear description of why??? The math itself is less complex than I expected.
// Maybe turn it into a blog post, this is kinda interesting to me
// TODO: make this work with something other than the unit circle
function calculateGradient(dist, point) {
  const angles = [0, PI*2/3, PI*4/3]
  // this is a pair of [sdfDiff, angle]
  // note the `sdfDiff` -- this is the **difference** between the sdf of our point, and the sdf at a point on the unit circle at a given angle.
  // This is useful because the **diff** of sdfs should range from [-1, 1].
  // Why? Because the unit circle has a radius of 1, and therefore the point furthest away from the sdf on the unit circle should be a diff of -1, (remember, further away is larger)
  // and the point closest to the sdf on the unit circle should be a diff of 1.
  /** @typedef {number} SdfDiff */
  /** @typedef {number} Radians */
  /** @type {Array<[SdfDiff, Radians]>} */
  const sdfAnglePairs = angles.map(angle => [dist - sdf(point.subtract(Vector2.fromAngle(angle))), angle])
  // minSdfDiff represents the angle which is furthest away from the sdf.
  const minSdfDiff = Math.min(...sdfAnglePairs.map(([sdf]) => sdf))
  const twoClosest = sdfAnglePairs.filter(([sdf]) => sdf > minSdfDiff)
  // probably will never happen, but if exactly one, we can just use that angle
  if (twoClosest.length === 1) {
    return Vector2.fromAngle(twoClosest[0][1])
  }

  // the max possible sdf difference (from `point` to `sdf`) is 1 because
  // sdf is a measurement of distance, and we're using the unit circle to get our points of reference.
  // That could change if we do something more complex than `Vector2.fromAngle` to get our comparison points
  const maxPossibleSdfDiff = 1

  // in theory, the curve between the two min sdf angles should represent an arg, where the maxima of the arc equals the line of the gradient.
  // We know that the curve is of angular rotation PI*2/3 because that is the spacing of our comparison points
  // I **think** (but I don't know) that the arc of our "comparison circle" will map linearly to the sdfs.
  // Therefore, we should be able to map the two sdfs to an arc and identify where the "maximum" of the arc is.
  // We can essentially "flatten" the arc, because the polar coordinates of (θ,sdf) *should* be linearly.
  // Assuming that our system is correct, we can assume two things
  // 1. the angle should be in between the two angles of `twoClosest` var
  // 2. the rate of change of this linear mapping should be the same as the rate of change of the arc.
  // The arc's [min, max] should be [-1, 1], which occurs on an arc of 2π. That means that the change from -1 to 1 occurs over an angular rotation of π.
  // Since our points are spaced at π*2/3, we can say that the rate of change of the sdf w.r.t. angular rotation is 2/π. This is the "slope" of our "polar" arc line.
  // If the rate of change is 2/π, then we can simply do (1-sdf) * (2/π) to get the angle diff of the gradient (w.r.t. the sdf angle), where `sdf` is either of the two sdf diffs in `twoClosest`.
  // The major caveat here is we need to make sure we're moving in the right direction. The angle could be clockwise or anticlockwise from the sdf we choose.
  // Once we know the angle diff, we can identify clockwise or anticlockwise based on where the two points are relative to each other. That is, if we used the larger angle, then we should move anticlockwise;
  // if we used the smaller angle, then move clockwise. This is reversed if the two "best" angles are 0 and 4π/3, because in that case 0 is the "larger" angle.

  /** @typedef {number} SdfDiffPerRadian */
  /** @type {SdfDiffPerRadian} */
  const sdfDiffRateOfChange = (2/PI)
  /** @type {SdfDiff} */
  const pickOne = twoClosest[0]
  const sdfAngleDiff = (maxPossibleSdfDiff - pickOne[0]) / sdfDiffRateOfChange
  const angleDiff = pickOne[1] === 0 && twoClosest[1][1] === PI*4/3
    ? pickOne[1] - sdfAngleDiff
    // unless pickOne is 0, we know that the other angle is the larger one because the list starts sorted and never gets unsorted
    : pickOne[1] + sdfAngleDiff

  return Vector2.fromAngle(angleDiff)
}
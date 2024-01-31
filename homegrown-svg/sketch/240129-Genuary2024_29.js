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
 * I appreciated Piter's explanation here https://www.youtube.com/watch?v=KRB57wyo8_4
 */
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 10,
  loopCount: 1,
}

let seed = randomSeed()
seed = 3016182276706477

const bg = ColorRgb.White

/**
 * General idea: mix together a gradient field from a simple SDF with a noise function
 */
renderSvg(config, (svg) => {
  const rng = createRng(seed)
  svg.filenameMetadata = { seed }
  svg.setBackground(bg)
  svg.numericPrecision = 3
  svg.fill = null
  svg.stroke = ColorRgb.Black
  svg.strokeWidth = 0.25

  const nPoints = 3000
  const lineLength = 100
  const noiseFn = createOscCurl(seed)
  const scale = 0.2
  const visited = []
  const distResults = []
  const padding = 0.85
 
  for (let i = 0; i < nPoints; i++) {
    const startPoint = Vector2.random(0, svg.width, 0, svg.height, rng)
    if (!svg.contains(startPoint)) {
      continue
    }
    const line = polyline(line => {
      line.push(startPoint)
      for (let i = 0; i < lineLength; i++) {
        const noiseVec = noiseFn(line.cursor.x * scale, line.cursor.y * scale)
        const dist = sdf(line.cursor)
        const sdfVec = calculateGradient(dist, line.cursor)

        distResults.push(Math.abs(dist))
        const noiseInfluence = clamp(0, 1, map(30, 0, 0, 1, Math.abs(dist)))
        const next = line.cursor.add(sdfVec.scale(1 - noiseInfluence)).add(noiseVec.scale(noiseInfluence))
        if (nearAnyPoint(next, visited, padding)) {
          break
        }
        line.push(next)
      }
    })
    visited.push(...line.points)
    svg.polyline(line)
  }

  console.log(Math.max(...distResults))

  return () => { seed = randomSeed() }  
})

function sdf(point) {
  const center = vec2(config.width / 2, config.height / 2)
  const radius = 5
  const sdf1 = sdfCircle(point, circle({ center, radius }))

  return sdf1
  // const sdf2 = sdfCircle(point, circle({ center: center.add(vec2(1, 0).scale(20)), radius }))
  // const sdf3 = sdfCircle(point, circle({ center: center.add(vec2(-1, 0).scale(20)), radius }))
  // return Math.min(sdf2, sdf3)
}

function sdfCircle(point, circle) {
  return point.subtract(circle.center).length() - circle.radius
}

/*
The goal: calculate a gradient in a vector field defined by an SDF (signed distance function) at `point`.

The strategy:
1. Sample the SDF value at three equally-spaced points around the `point`.
2. Calculate the difference between the sample SDF values and the SDF value at `point`.
   Important insight: the max difference should be 1, because the sample points are taken from the unit circle around `point`.
   The unit circle has a radius of 1 and the SDF value is a simple measurement of distance.
   Therefore, the max difference should be equal to the radius of the circle from which the sample points were taken, which is 1.
3. Interpolate between the two highest values to find the angle at which the difference is `1`.
   The angle at which the SDF value difference is `1` indicates the gradient, because this is the maximum difference
   between the `point` and the SDF values around the point.
   Important insight: the SDF values become larger as you move away from the shape.
   The difference is taken from `point` to "sample angle", so the difference will be negative when
   the SDF value is larger than the SDF value at `point`.
   (Example: if SDF at `point` is 0.5 and SDF at the sample angle is 0.75, then the diff would be -0.25.)
   Therefore, the gradient moving *towards* the shape will always be positive, because this will indicate
   the direction of *decreasing* SDF values.

The result should be a Vector2 with length `1` which points in the direction of the gradient.

The detailed algorithm is as follows:
0. Define the radius of the circle from which we will sample the SDF.
   For the purpose of this explanation, we will use a radius of `1`.
   This value will define the magnitude of the maximum SDF difference.
   (see above "strategy" for justification of this assertion).
1. Define the three angles at which we will sample the SDF.
   For our purposes we will use three evenly spaced angles at `[0, 2π/3, 4π/3]`.
2. Sample the SDF at the three angles.
   For each sample point, calculate the difference between the raw SDF value and the SDF value at `point`.
   The SDF difference should be in range [-1, 1].
   Collect the result pairs of `[sdfDiff, angle]`.
3. Discard the lowest SDF difference. This indicates the point that is furthest away from the shape.
   The higher two values will be used to interpolate the gradient.
   - If there are two values with exactly the same SDF difference, and they are both lower than the third sample value,
     then we can assume that the third sample value defines the gradient. This should be *exceptionally* rare, but
     it is necessary to handle it to avoid `undefined` checks further in the algorithm.
4. Define the rate of change of the SDF diff in "units per radian".
   What is the purpose of this definition?
   The rate of change of the SDF diff should be linear with respect to the angle.
   Defining a linear rate of change allows us to interpolate the gradient angle,
   based on the known SDF values at known angles.
   Since the maximum and minimum SDF differences should be PI radians apart,
   the rate of change will always be 2r/π, where `r` is the radius of the circle from which we sampled the SDF.
5. Choose one of the two remaining sample points, and calculate the SDF difference between
   the sample point and the maximum (e.g. `1`).
6. Calculate the difference in radians between the sample point and the "maximum" point (i.e. gradient).
   This is done by dividing the SDF difference by the rate of change.
7. Calculate the absolute angle, in radians, of the gradient.
   The gradient will typically be between the two sample points. (There are cases where this might not be true, this algorithm
   probably has a bug around these cases.)
   This can be derived by taking the angle of the sample point and adding/subtracting the angle difference.
   Adding or subtracting is chosen based on the two remaining sample angles.
8. Return a normalized Vector2 pointing in the direction of the gradient.
*/
function calculateGradient(dist, point, sampleRadius = 1) {
  // Algorithm step 1
  const angles = [0, PI*2/3, PI*4/3]
  // Algorithm step 2
  /** @typedef {number} SdfDiff */
  /** @typedef {number} Radians */
  /** @type {Array<[SdfDiff, Radians]>} */
  const sdfAnglePairs = angles.map(angle => {
    const sdfSampleValue = sdf(point.subtract(Vector2.fromAngle(angle).scale(sampleRadius)))
    const diff = dist - sdfSampleValue
    if (diff > sampleRadius || diff < -sampleRadius) {
      console.warn(`diff is out of range [-${sampleRadius}, ${sampleRadius}]: ${diff}. This indicates a bug in the algorithm.`)
    }
    return [diff, angle]
  })
  // Algorithm step 3
  const minSdfDiff = Math.min(...sdfAnglePairs.map(([sdf]) => sdf))
  const twoClosest = sdfAnglePairs.filter(([sdf]) => sdf > minSdfDiff)
  if (twoClosest.length === 1) {
    return Vector2.fromAngle(twoClosest[0][1])
  }

  // Algorithm step 4
  // Note: messing with this value creates some interesting patterns!
  /** @typedef {number} SdfDiffPerRadian */
  /** @type {SdfDiffPerRadian} */
  const sdfDiffRateOfChange = (sampleRadius * 2 / PI)

  // Algorithm step 5
  /** @type {SdfDiff} */
  const pickOne = twoClosest[0]
  const sdfDiff = sampleRadius - pickOne[0]

  // Algorithm step 6
  const sdfAngleDiff = sdfDiff / sdfDiffRateOfChange

  // Algorithm step 7
  const angleDiff = pickOne[1] === 0 && twoClosest[1][1] === PI*4/3
    ? pickOne[1] - sdfAngleDiff
    // unless pickOne is 0, we know that the other angle is the larger one because the list starts sorted and never gets unsorted
    : pickOne[1] + sdfAngleDiff

  // Algorithm step 8
  return Vector2.fromAngle(angleDiff)
}

/**
 * @param {Vector2} point
 * @param {Vector2[]} others 
 * @param {number} padding 
 * @returns
 */
function nearAnyPoint(point, others, padding) {
  // hypothesis: points are more likely to be near the most recently-placed points than near the oldest points
  for (let i = others.length - 1; i >= 0; i--) {
    if (point.distanceTo(others[i]) < padding) {
      return true
    }
  }
  return false
}

import { renderSvg, map, vec2, randomSeed, createRng, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle, Svg, ColorHsl, grid, Path } from '@salamivg/core'
import { calcContour, calcTriangles, connectContourSegments } from './20250308-walking-triangles-util.ts'

/**
 * There are a few primary "styles", each with some subtle variations:
 * 1. exponential curve, vs sinusoidal curve
 * 2. high vs low terminal randomness
 * 3. low vs high exponent
 */

const config = {
  width: 800,
  height: 800,
  scale: 1,
  loopCount: 1,
  // openEveryFrame: false
}

let seed = randomSeed()
seed = 5823994192365817

const colors = [
  '785A96',
  'E4BF70',
  'B2C566',
  '6887A1',
  'CC7171',
  'E2A554',
  'A4CAC8',
  '9D689C',
].map(h => ColorRgb.fromHex(h).toHsl())

const bg = '#332C2B'

renderSvg(config, (svg) => {

  const rng = createRng(seed)
  svg.filenameMetadata = { seed }
  svg.numericPrecision = 3
  svg.setBackground('#fff')
  svg.setAttributes({'stroke-linecap': 'round' })

  /**
   * This feels like a LOT of ceremony for contour lines. Can't this be simplified?
   */
  const stepSize = 1
  const g = grid({xMin: 0, xMax: config.width, yMin: 0, yMax: config.height, xStep: stepSize, yStep: stepSize });
  const scale = 0.005
  const zScale = 10
  const noise = createOscNoise(seed, scale, zScale)

  console.log('Triangulating ...');
  const ts = calcTriangles(noise, g)
  console.log(ts.length, 'triangles')

  const nThresholds = 10
  const thresholds = Array(nThresholds).fill(0).map((_, i) => map(0, nThresholds - 1, -zScale, zScale, i))
  console.log('calculating contours')
  // this MUST be flatMapped to ensure we get an unordered list of contour segments for the given threshold
  // each segments represents an intersection of a single triangle.
  const segments = thresholds.flatMap(threshold => calcContour(threshold, ts))
  console.log('segments length', segments.length)
  const connected = connectContourSegments(segments, stepSize)

  console.log('connected length', connected.length)
  // for (const p of connected) {
  for (let i = 0; i < connected.length; i++) {
    // if (i % 3 !== 0) continue
    const p = connected[i]
    const contour = Path.fromPoints(p, false, 'absolute')
    contour.fill = null
    contour.stroke = '#000'
    svg.path(contour)
  }

  return () => { seed = randomSeed(); }
})

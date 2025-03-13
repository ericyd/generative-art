import { renderSvg, map, vec2, randomSeed, createRng, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle, Svg, ColorHsl, grid, Path, Vector3, Vector2, array, vec3, contoursFromTIN, type TIN } from '@salamivg/core'

import {readFileSync} from 'node:fs'

// const file = readFileSync('./flow.time100.hd5.coords.json').toString()
const coordsFile = readFileSync('./mountain-coords.txt').toString()
const cellsFile = readFileSync('./mountain-cells.txt').toString()
const coords: number[] = JSON.parse(coordsFile)
const cells: number[] = JSON.parse(cellsFile)
console.log('coords length', coords.length)

let xMin = 0
let xMax = 0
let yMin = 0
let yMax = 0
let zMin = 0
let zMax = 0
// calculate the minimum x value by manually looping through coords
// necessary because `Math.min(...coords.map(c => c[0]))` hits a max recursion limit 😳
for (let i = 0; i < coords.length; i += 3) {
  xMin = Math.min(xMin, coords[i])
  xMax = Math.max(xMax, coords[i])
  yMin = Math.min(yMin, coords[i+1])
  yMax = Math.max(yMax, coords[i+1])
  zMin = Math.min(zMin, coords[i+2])
  zMax = Math.max(zMax, coords[i+2])
}

console.log({
  minX: xMin,
  maxX: xMax,
  minY: yMin,
  maxY: yMax,
  minZ: zMin,
  maxZ: zMax
})

console.log('processing array', Date.now())
const ts: TIN = []
for (let i = 0; i < cells.length; i += 3) {
  if (cells[i] > coords.length || cells[i+1] > coords.length || cells[i+2] > coords.length) {
    console.warn(`cell ${i} contains coordinates outside bounds`)
    continue
  }

  if (cells[i] === 0 || cells[i+1] === 0 || cells[i+2] === 0) {
    console.warn(`cell ${i} contains a 0`)
    continue
  }

  // cells[i] is the coordinate index for point 1 of the triangle
  // cells[i] refers to the index in the [num, num, num] array, which has been flattened.
  // if cells[i] == 900; in the original array it would correspond to index 900-1=899.
  // in the new array, it corresponds to (900-1)*3
  // cells[i] used to contain 3 values
  /**
   * ORIGINAL
   * cells[i] = [900, 734, 870]
   * coords[900] = [3.4, 5.6, 7.8]
   * coords[734] = [3.4, 5.6, 7.8]
   * coords[870] = [3.4, 5.6, 7.8]
   * 
   * NEW
   * cells[i] = 900
   * cells[i+1] = 734
   * cells[i+2] = 870
   * coords[900*3] = 3.4
   * coords[900*3+1] = 5.6
   * coords[900*3+2] = 7.8
   * coords[734*3] = 3.4
   * coords[734*3+1] = 5.6
   * coords[734*3+2] = 7.8
   * coords[870*3] = 3.4
   * coords[870*3+1] = 5.6
   * coords[870*3+2] = 7.8
   * 
   * 
   */

  const v1 = vec3(coords[(cells[i+0]-1)*3+0], coords[(cells[i+0]-1)*3+1], coords[(cells[i+0]-1)*3+2])
  const v2 = vec3(coords[(cells[i+1]-1)*3+0], coords[(cells[i+1]-1)*3+1], coords[(cells[i+1]-1)*3+2])
  const v3 = vec3(coords[(cells[i+2]-1)*3+0], coords[(cells[i+2]-1)*3+1], coords[(cells[i+2]-1)*3+2])
  ts.push([v1, v2, v3])
}
console.log('finish processing array', Date.now())


const config = {
  width: 800,
  height: 800,
  viewBox: `${xMin} ${yMin} ${xMax} ${yMax}`,
  scale: 1,
  loopCount: 1,
}
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

let seed = randomSeed()

renderSvg(config, (svg) => {
  const rng = createRng(seed)
  svg.filenameMetadata = { seed: String(seed) }
  svg.numericPrecision = 3
  svg.setBackground('#fff')
  svg.setAttributes({'stroke-linecap': 'round' })
  

  // TIN only
  // for (const t of ts) {
  //   svg.polygon(p => {
  //     p.points = t.map(t => vec2(t.x, t.y))
  //     p.fill = null
  //     p.stroke = '#000'
  //     p.strokeWidth = 10
  //   })
  // }

  const start = process.hrtime.bigint()
  const thresholdCount = 100

  const spectrum = ColorSequence.fromColors([hsl(340, 0.8, 0.3), hsl(220, 0.8, 0.5)])
  const connected = contoursFromTIN({
    thresholdCount,
    zMin,
    zMax,
    tin: ts,
    nearnessThreshold: 10,
  })

  const end = process.hrtime.bigint()
  const diff = (end - start).toString()
  const diffFormat = diff.slice(0, -9) + '.' + diff.slice(-9)
  console.log(`TIN size: ${ts.length}, Threshold count: ${thresholdCount}, Time: ${diffFormat}s`);

  for (const [threshold, segmentsList] of connected.entries()) {
    for (const segments of segmentsList) {
      const contour = Path.fromPoints(segments, false, 'absolute')
      contour.fill = null
      contour.stroke = spectrum.at(map(zMin, zMax, 0, 1, threshold))
      contour.strokeWidth = 50
      svg.path(contour)
    }
  }

  return () => { seed = randomSeed(); }
})

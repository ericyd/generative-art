import { renderSvg, map, vec2, randomSeed, createRng, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle, Svg, ColorHsl, grid, Path, Vector3, Vector2, array, vec3, contoursFromTIN, type TIN } from '@salamivg/core'

import {readFileSync} from 'node:fs'

// const file = readFileSync('./flow.time100.hd5.coords.json').toString()
const coordsFile = readFileSync('./tin.time9.varCanyon.coords.txt').toString().replace(/\t/g, ',').replace(/\n/g, '],\n[')
const cellsFile = readFileSync('./tin.time9.varCanyon.cells.txt').toString().replace(/\t/g, ',').replace(/\n/g, '],\n[')
const coords: [number, number, number][] = JSON.parse('[[' + coordsFile + ']]')
const cells: [number, number, number][] = JSON.parse('[[' + cellsFile + ']]')
console.log('coords length', coords.length)

let xMin = 0
let xMax = 0
let yMin = 0
let yMax = 0
let zMin = 0
let zMax = 0
// calculate the minimum x value by manually looping through coords
// necessary because `Math.min(...coords.map(c => c[0]))` hits a max recursion limit 😳
for (let i = 0; i < coords.length; i += 1) {
  xMin = Math.min(xMin, coords[i][0])
  xMax = Math.max(xMax, coords[i][0])
  yMin = Math.min(yMin, coords[i][1])
  yMax = Math.max(yMax, coords[i][1])
  zMin = Math.min(zMin, coords[i][2])
  zMax = Math.max(zMax, coords[i][2])
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
for (let i = 0; i < cells.length; i += 1) {
  if (cells[i][0] > coords.length || cells[i][1] > coords.length || cells[i][2] > coords.length) {
    console.warn(`cell ${i} contains coordinates outside bounds`)
    continue
  }

  if (cells[i][0] === 0 || cells[i][1] === 0 || cells[i][2] === 0) {
    console.warn(`cell ${i} contains a 0`)
    continue
  }

  const [v1Index, v2Index, v3Index] = cells[i]

  const v1 = vec3(...coords[v1Index-1])
  const v2 = vec3(...coords[v2Index-1])
  const v3 = vec3(...coords[v3Index-1])
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

let seed = randomSeed()

renderSvg(config, (svg) => {
  const rng = createRng(seed)
  svg.filenameMetadata = { seed: String(seed) }
  svg.numericPrecision = 3
  svg.setBackground('#fff')
  svg.setAttributes({'stroke-linecap': 'round' })

  const start = process.hrtime.bigint()
  const thresholdCount = 30
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

  for (const [_threshold, segmentsList] of connected.entries()) {
    for (const segments of segmentsList) {
      const contour = Path.fromPoints(segments, false, 'absolute')
      contour.fill = null
      contour.stroke = '#000'
      contour.strokeWidth = 50
      svg.path(contour)
    }
  }

  return () => { seed = randomSeed(); }
})

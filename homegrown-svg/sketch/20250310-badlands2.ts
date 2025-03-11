import { renderSvg, map, vec2, randomSeed, createRng, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle, Svg, ColorHsl, grid, Path, Vector3, Vector2, array, vec3 } from '@salamivg/core'

import {readFileSync} from 'node:fs'

type IntersectingLine = [Vector2, Vector2]
type Triangle3 = [Vector3, Vector3, Vector3]

// const file = readFileSync('./flow.time100.hd5.coords.json').toString()
const coordsFile = readFileSync('./tin.time9.varCanyon.coords.txt').toString().replace(/\t/g, ',').replace(/\n/g, '],\n[')
const cellsFile = readFileSync('./tin.time9.varCanyon.cells.txt').toString().replace(/\t/g, ',').replace(/\n/g, '],\n[')
const coords: [number, number, number][] = JSON.parse('[[' + coordsFile + ']]')
const cells: [number, number, number][] = JSON.parse('[[' + cellsFile + ']]')
console.log('coords length', coords.length)


// const xMin = Math.min(...coords.map(c => c[0]))
// const xMax = Math.max(...coords.map(c => c[0]))
// const yMin = Math.min(...coords.map(c => c[1]))
// const yMax = Math.max(...coords.map(c => c[1]))
// const zMin = Math.min(...coords.map(c => c[2]))
// const zMax = Math.max(...coords.map(c => c[2]))
let xMin = 0
let xMax = 0
let yMin = 0
let yMax = 0
let zMin = 0
let zMax = 0
// calculate the minimum x value by manually looping through coords
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

const ts: Triangle3[] = []
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

  const connected = getContourPaths({
    contourCount: 50,
    zMin,
    zMax,
    triangles: ts,
    segmentNearnessThreshold: 10,
    logProgress: true
  })

  console.log('connected length', connected.length)
  for (let i = 0; i < connected.length; i++) {
    const p = connected[i]
    const contour = Path.fromPoints(p, false, 'absolute')
    contour.fill = null
    contour.stroke = '#000'
    contour.strokeWidth = 300
    svg.path(contour)
  }

  return () => { seed = randomSeed(); }
})

function getContourPaths({
  contourCount = 10,
  zMin = -1,
  zMax = 1,
  triangles,
  segmentNearnessThreshold = 1,
  logProgress = false
}: {
  contourCount?: number;
  zMin?: number;
  zMax?: number;
  triangles: Triangle3[],
  segmentNearnessThreshold?: number
  logProgress?: boolean
}) {
  if (logProgress) console.log(triangles.length, 'triangles')

  const thresholds = array(contourCount).map((i) => map(0, contourCount - 1, zMin, zMax, i))
  if (logProgress) console.log('calculating contours', thresholds.length)
  
  // this MUST be flatMapped to ensure we get an unordered list of contour segments for the given threshold
  // each segments represents an intersection of a single triangle.
  const segments = thresholds.flatMap(threshold => calcContour(threshold, triangles))
  if (logProgress) console.log('segments length', segments.length)
  
  return connectContourSegments(segments, segmentNearnessThreshold)
}

function contourLine(vertices: Triangle3, threshold: number): Contour | null {
  const below = vertices.filter(v => v.z < threshold);
  const above = vertices.filter(v => v.z >= threshold);

  if (above.length === 0 || below.length === 0) {
    return null;
  }

  const minority = below.length < above.length ? below : above;
  const majority = below.length > above.length ? below : above;

  // @ts-expect-error the array is initialized empty,
  // but visual inspection tells us it will contain IntersectingLine by the time it is returned.
  const contourPoints: IntersectingLine = [];
  for (const [vMin, vMax] of [[minority[0], majority[0]], [minority[0], majority[1]]]) {
    const howFar = (threshold - vMax.z) / (vMin.z - vMax.z);
    const crossingPoint = vec2(
      howFar * vMin.x + (1.0 - howFar) * vMax.x,
      howFar * vMin.y + (1.0 - howFar) * vMax.y
    );
    contourPoints.push(crossingPoint);
  }
  return { line: contourPoints, threshold };
}

function calcTriangles(elevationFn: (x: number, y: number) => number, grid: Grid): Triangle3[] {
  // when x is even, create triangle from points [(x,y), (x+1,y), (x+1,y+1)]
  // when x is odd, create triangle from points [(x,y), (x,y+1), (x+1,y+1)]
  const triangles: Triangle3[] = []
  for (let row = 0; row < grid.rowCount - 1; row++) {
    for (let col = 0; col < grid.columnCount - 1; col++) {
      const x0 = col * grid.yStep;
      const x1 = (col + 1) * grid.yStep;
      const y0 = row * grid.xStep;
      const y1 = (row + 1) * grid.xStep;
      if (col % 2 === 0) {
        const v1 = new Vector3(x0, y0, elevationFn(x0, y0))
        const v2 = new Vector3(x1, y0, elevationFn(x1, y0))
        const v3 = new Vector3(x1, y1, elevationFn(x1, y1))
        triangles.push([v1, v2, v3])
      } else {
        const v1 = new Vector3(x0, y0, elevationFn(x0, y0))
        const v2 = new Vector3(x0, y1, elevationFn(x0, y1))
        const v3 = new Vector3(x1, y1, elevationFn(x1, y1))
        triangles.push([v1, v2, v3])
      }
    }
  }

  return triangles
}

type Contour = {
  line: IntersectingLine,
  threshold: number,
}

function calcContour(threshold: number, triangles: Triangle3[]): Contour[] {
  return triangles.map(ts => contourLine(ts, threshold)).filter(Boolean) as Contour[];
}

// TODO: not sure threshold should always be `1`...
function isNear(p1: Vector2, p2: Vector2, threshold = 1): boolean {
  return p1.distanceTo(p2) < threshold;
}

function connectContourSegments(contours: Contour[], threshold?: number): Vector2[][] {
  let contourLines: Vector2[][] = [];

  // partition the contour segments by threshold.
  // justification: we only want to connect segments which belong to the same threshold,
  // (i.e. avoid connecting segments which are spatially close but do not belong to the same threshold).
  // Also, this MASSIVELY speeds up processing because it dramatically reduces the number of segments to search
  // through when looking for connecting segments.
  const contourMap = new Map<number, IntersectingLine[]>()
  for (let i = 0; i < contours.length; i++) {
    if (contourMap.has(contours[i].threshold)) {
      contourMap.get(contours[i].threshold)?.push(contours[i].line)
    } else {
      contourMap.set(contours[i].threshold, [contours[i].line])
    }
  }

  console.log('contour map key length', Array.from(contourMap.keys()).length)

  for (const segments of contourMap.values()) {
    // contourLines.push(...segments)

    outerLoop: while (segments.length > 0) {
      // grab arbitrary segment
      const line: Vector2[] | undefined = segments.pop()
      if (!line) break outerLoop;
  
      innerLoop: while (true) {
        // find segments that join at the head of the line.
        // if we find a point that matches, we must take the other point from the segment, so
        // we continue to build a line moving forwards with no duplicate points.
        const firstMatchingSegmentIndex = segments.findIndex((segment) => 
          isNear(line[0], segment[0], threshold) ||
          isNear(line[0], segment[1], threshold) ||
          isNear(line[line.length - 1], segment[0], threshold) ||
          isNear(line[line.length - 1], segment[1], threshold)
        );
  
        // when no matching segment exists, the line is complete.
        if (firstMatchingSegmentIndex === -1) {
          if (line.length > 5) {
            contourLines.push(line);
          }
          break innerLoop;
        }
  
        const match = segments[firstMatchingSegmentIndex];
  
        // Note: in all "cases" below, the phrase "connects to" means the points are functionally equivalent,
        // and therefore do not need to be duplicated in the resulting contour.
        // case: match[0] connects to line[0]; unshift match[1]
        if (isNear(match[0], line[0], threshold)) {
          line.unshift(match[1])
        }
        // case: match[1] connects to line[0]; unshift match[0]
        else if (isNear(match[1], line[0], threshold)) {
          line.unshift(match[0])
        }
        // case: match[0] connects to line[-1]; push match[1]
        else if (isNear(match[0], line[line.length - 1], threshold)) {
          line.push(match[1])
        }
        // case: match[1] connects to line[-1]; push match[0]
        else if (isNear(match[1], line[line.length - 1], threshold)) {
          line.push(match[0])
        }
  
        // removing the matching segment from the list to prevent duplicate connections
        segments.splice(firstMatchingSegmentIndex, 1);
      }
    }
  }


  return contourLines;
}

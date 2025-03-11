import { renderSvg, map, vec2, randomSeed, createRng, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle, Svg, ColorHsl, grid, Path, Vector3, Vector2, array } from '@salamivg/core'

type IntersectingLine = [Vector2, Vector2]

class Triangle3D {
  vertices: Vector3[];

  constructor(vertices: Vector3[]) {
    this.vertices = vertices;
  }

  contourLine(threshold: number): IntersectingLine | null {
    const below = this.vertices.filter(v => v.z < threshold);
    const above = this.vertices.filter(v => v.z >= threshold);

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
    return contourPoints;
  }
}

const config = {
  width: 800,
  height: 800,
  scale: 1,
  loopCount: 1,
  // openEveryFrame: false
}

let seed = randomSeed()
seed = 5823994192365817

renderSvg(config, (svg) => {

  const rng = createRng(seed)
  svg.filenameMetadata = { seed: String(seed) }
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
  const connected = getContourPaths({
    contourCount: 30,
    zMin: -zScale,
    zMax: zScale,
    noiseFn: noise,
    grid: g,
    segmentNearnessThreshold: stepSize * 5,
    logProgress: true
  })

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

function getContourPaths({
  contourCount = 10,
  zMin = -1,
  zMax = 1,
  noiseFn,
  grid,
  segmentNearnessThreshold = 1,
  logProgress = false
}: {
  contourCount?: number;
  zMin?: number;
  zMax?: number;
  noiseFn: (x: number, y: number) => number;
  grid: Grid,
  segmentNearnessThreshold?: number
  logProgress?: boolean
}) {
  if (logProgress) console.log('Triangulating ...');
  const ts = calcTriangles(noiseFn, grid)
  if (logProgress) console.log(ts.length, 'triangles')

  const thresholds = array(contourCount).map((i) => map(0, contourCount - 1, zMin, zMax, i))
  console.log('thresholds', thresholds)
  if (logProgress) console.log('calculating contours')
  
  // this MUST be flatMapped to ensure we get an unordered list of contour segments for the given threshold
  // each segments represents an intersection of a single triangle.
  const segments = thresholds.flatMap(threshold => calcContour(threshold, ts))
  if (logProgress) console.log('segments length', segments.length)
  
  return connectContourSegments(segments, segmentNearnessThreshold)
}

function calcTriangles(elevationFn: (x: number, y: number) => number, grid: Grid): Triangle3D[] {
  // when x is even, create triangle from points [(x,y), (x+1,y), (x+1,y+1)]
  // when x is odd, create triangle from points [(x,y), (x,y+1), (x+1,y+1)]
  const triangles: Triangle3D[] = []
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
        triangles.push(new Triangle3D([v1, v2, v3]))
      } else {
        const v1 = new Vector3(x0, y0, elevationFn(x0, y0))
        const v2 = new Vector3(x0, y1, elevationFn(x0, y1))
        const v3 = new Vector3(x1, y1, elevationFn(x1, y1))
        triangles.push(new Triangle3D([v1, v2, v3]))
      }
    }
  }

  return triangles
}

function calcContour(threshold: number, triangles: Triangle3D[]): IntersectingLine[] {
  return triangles.map(t => t.contourLine(threshold)).filter(Boolean) as IntersectingLine[];
}

// TODO: not sure threshold should always be `1`...
function isNear(p1: Vector2, p2: Vector2, threshold = 1): boolean {
  return p1.distanceTo(p2) < threshold;
}

function isSomewhatNear(p1: Vector2, p2: Vector2): boolean {
  if (!p1 || !p2) return false;
  return p1.distanceTo(p2) < 20.0;
}

// Written in a very imperative way to try to optimize performance
// turns out "algo1" is better anyway! Faster and also connects more segments.
function connectContourSegments(segments: IntersectingLine[], threshold?: number): Vector2[][] {
  let contourLines: Vector2[][] = [];

  outerLoop: while (segments.length > 0) {
    // grab arbitrary
    const line: Vector2[] | undefined = segments.pop()
    if (!line) break outerLoop;

    innerLoop: while (true) {
      // find segments that join at the head of the line.
      // if we find a point that matches, we must take the other point from the segment, so
      // we continue to build a line moving forwards with no duplicate points.
      let headMatchIndex: number | undefined = undefined;
      let headMatch0 = false;
      let tailMatchIndex: number | undefined = undefined;
      let tailMatch0 = false;
      for (let i = 0; i < segments.length; i++) {
        if (headMatchIndex === undefined) {
          if (isNear(line[0], segments[i][0], threshold)) {
            headMatchIndex = i;
            headMatch0 = true;
          } else if (isNear(line[0], segments[i][1], threshold)) {
            headMatchIndex = i;
            headMatch0 = false;
          }
        }

        if (tailMatchIndex === undefined) {
          if (isNear(line[line.length - 1], segments[i][0], threshold)) {
            tailMatchIndex = i;
            tailMatch0 = true;
          } else if (isNear(line[line.length - 1], segments[i][1], threshold)) {
            tailMatchIndex = i;
            tailMatch0 = false;
          }
        }

        if (headMatchIndex !== undefined && tailMatchIndex !== undefined) {
          break;
        }
      }

      // when no matching segment exists, the line is complete.
      if (headMatchIndex === undefined && tailMatchIndex === undefined) {
        if (line.length > 0) {
          contourLines.push(line);
        }
        break innerLoop;
      }

      if (headMatchIndex !== undefined) {
        if (headMatch0) {
          line.unshift(segments[headMatchIndex][1])
        } else {
          line.unshift(segments[headMatchIndex][0])
        }
      }

      if (tailMatchIndex !== undefined) {
        if (tailMatch0) {
          line.unshift(segments[tailMatchIndex][1])
        } else {
          line.unshift(segments[tailMatchIndex][0])
        }
      }

      // removing the matching segment from the list to prevent duplicate connections
      if (headMatchIndex !== undefined && tailMatchIndex === undefined) {
        segments.splice(headMatchIndex, 1);
      } else if (headMatchIndex === undefined && tailMatchIndex !== undefined) {
        segments.splice(tailMatchIndex, 1);
      } else if (headMatchIndex !== undefined && tailMatchIndex !== undefined) {
        if (headMatchIndex < tailMatchIndex) {
          segments.splice(headMatchIndex, 1);
          segments.splice(tailMatchIndex, 1);
        } else {
          segments.splice(tailMatchIndex, 1);
          segments.splice(headMatchIndex, 1);
        }
      }
    }
  }

  return contourLines;
}

import { renderSvg, map, vec2, randomSeed, createRng, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle, Svg, ColorHsl, grid, Path, Vector3, Vector2 } from '@salamivg/core'

class Triangle3D {
  vertices: Vector3[];

  constructor(vertices: Vector3[]) {
    this.vertices = vertices;
  }

  contourLine(threshold: number): Vector2[] | null {
    const below = this.vertices.filter(v => v.z < threshold);
    const above = this.vertices.filter(v => v.z >= threshold);

    if (above.length === 0 || below.length === 0) {
      return null;
    }

    const minority = below.length < above.length ? below : above;
    const majority = below.length > above.length ? below : above;

    const contourPoints: Vector2[] = [];
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

  console.log('Triangulating ...');
  const ts = calcTriangles(noise, g)
  console.log(ts.length, 'triangles')

  const nThresholds = 30
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

function calcContour(threshold: number, triangles: Triangle3D[]): Vector2[][] {
  return triangles.map(t => t.contourLine(threshold)).filter(Boolean) as Vector2[][];
}

// TODO: not sure threshold should always be `1`...
function isNear(p1: Vector2, p2: Vector2, threshold = 1): boolean {
  return p1.distanceTo(p2) < threshold;
}

function isSomewhatNear(p1: Vector2, p2: Vector2): boolean {
  if (!p1 || !p2) return false;
  return p1.distanceTo(p2) < 20.0;
}

function connectContourSegments(segments: Vector2[][], threshold?: number): Vector2[][] {
  const contourLines: Vector2[][] = [];

  outer: while (segments.length > 0) {
    const line = segments.pop()!;
    inner: while (true) {
      let headSegments: [number, Vector2[]][] = [];
      let tailSegments: [number, Vector2[]][] = [];

      for (const [index, seg] of segments.entries()) {
        if (line.at(-1) && (isNear(line.at(-1)!, seg[0], threshold) || isNear(line.at(-1)!, seg[1], threshold))) {
          headSegments.push([index, seg]);
        }
        if (line[0] && (isNear(line[0], seg[0], threshold) || isNear(line[0], seg[1], threshold))) {
          tailSegments.push([index, seg]);
        }
      }

      if (headSegments.length === 0 && tailSegments.length === 0) {
        contourLines.push(line);
        break inner;
      }

      let headSegment = headSegments.pop();
      if (headSegment) {
        const [index, newSeg] = headSegment;
        if (isNear(newSeg[0], line.at(-1)!, threshold)) {
          line.push(newSeg[0]);
          line.push(newSeg[1]);
        } else {
          line.push(newSeg[1]);
          line.push(newSeg[0]);
        }
        segments.splice(index, 1);
      }

      let tailSegment = tailSegments.pop();
      if (tailSegment) {
        const [index, newSeg] = tailSegment;
        if (isNear(newSeg[0], line[0], threshold)) {
          line.unshift(newSeg[0]);
          line.unshift(newSeg[1]);
        } else {
          line.unshift(newSeg[1]);
          line.unshift(newSeg[0]);
        }
        segments.splice(index, 1);
      }
    }
  }

  return contourLines;
}

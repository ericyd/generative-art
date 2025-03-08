// originally converted from https://github.com/ericyd/generative-art/blob/b584b6d05e00e533f4c5bdacf23f92aee48f9ac7/nannou/examples/util/contours.rs
// with the help of qwen2.5-coder:7b
import { Grid, Vector2, vec2, Vector3 } from '@salamivg/core';
// import { Point3, Point, Vector2[] } from './types'; // Assuming these types are defined in a separate file
// import { Fbm, RidgedMulti, Billow } from 'noisejs';

export class Triangle3D {
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

export function calcTriangles(elevationFn: (x: number, y: number) => number, grid: Grid): Triangle3D[] {
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

export function calcContour(threshold: number, triangles: Triangle3D[]): Vector2[][] {
  return triangles.map(t => t.contourLine(threshold)).filter(Boolean) as Vector2[][];
}

// TODO: not sure threshold should always be `1`...
export function isNear(p1: Vector2, p2: Vector2, threshold = 1): boolean {
  return p1.distanceTo(p2) < threshold;
}

export function isSomewhatNear(p1: Vector2, p2: Vector2): boolean {
  if (!p1 || !p2) return false;
  return p1.distanceTo(p2) < 20.0;
}

export function connectContourSegments(segments: Vector2[][], threshold?: number): Vector2[][] {
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

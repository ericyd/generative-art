/**
 * Based on the algorithm used here:
 * http://rectangleworld.com/blog/archives/462
 * and introduced here:
 * http://rectangleworld.com/blog/archives/413
 *
 * Note: since the original algorithm has been removed:
 * It used a linked-list type structure which, yes, is more efficient, but
 * I chose to use a normal array because it is much, much easier to work with.
 * That said, I could potentially implement a custom iterator if I wanted to use a linked list in the future.
 */

import { LineSegment } from '../components/line-segment.js'
import { Path } from '../components/path.js'
import { random } from '../random.js'
import { Vector2 } from '../vector2.js'

/**
 * @class FractalizedLine
 * @classdesc Represents a fractalized line based on a given set of points.
 */
export class FractalizedLine {
  /**
   * @param {Vector2[]} points - The initial set of points.
   * @param {import("../random.js").Rng} [rng] - The random number generator. Defaults to the default Random instance.
   */
  constructor(points, rng = Math.random) {
    this.points = points
    this.rng = rng
  }

  /**
   * @readonly
   * @type {LineSegment[]} segments - The segments formed by connecting consecutive points.
   */
  get segments() {
    const segments = []
    for (let i = 0; i < this.points.length - 1; i++) {
      segments.push(new LineSegment(this.points[i], this.points[i + 1]))
    }
    return segments
  }

  /**
   * @param {boolean} closed
   * @returns {Path}
   */
  path(closed = true) {
    return Path.fromPoints(this.points, closed)
  }

  // TODO: openrndr/src/main/kotlin/shape/SmoothLine.kt
  // get smoothLine() {
  //   return new SmoothLine(this.points);
  // }

  /**
   * Recursively subdivide the points using perpendicular offset.
   * @param {number} subdivisions - The number of times to subdivide.
   * @param {number} [offsetPct=0.50] - The percentage of the offset.
   * @returns {FractalizedLine} - The updated FractalizedLine instance.
   */
  perpendicularSubdivide(subdivisions, offsetPct = 0.5) {
    return this.subdivide(
      subdivisions,
      offsetPct,
      this.perpendicularOffset.bind(this),
    )
  }

  // TODO: openrndr/src/main/kotlin/shape/FractalizedLine.kt
  // gaussianSubdivide(subdivisions, offsetPct = 0.35) {
  //   return this.subdivide(subdivisions, offsetPct, this.gaussianOffset);
  // }

  /**
   * Recursively subdivide the points.
   * @private
   * @param {number} subdivisions - The number of times to subdivide.
   * @param {number} [offsetPct=0.5] - The percentage of the offset.
   * @param {(v1: Vector2, v2: Vector2, n: number) => Vector2} [offsetFn=this.perpendicularOffset] - The offset function.
   * @returns {FractalizedLine} - The updated FractalizedLine instance.
   */
  subdivide(
    subdivisions,
    offsetPct = 0.5,
    offsetFn = this.perpendicularOffset.bind(this),
  ) {
    for (let i = 0; i < subdivisions; i++) {
      const newPoints = []

      for (let j = 0; j < this.points.length - 1; j++) {
        const current = this.points[j]
        const next = this.points[j + 1]
        const mid = offsetFn(current, next, offsetPct)
        newPoints.push(current, mid)
      }
      newPoints.push(this.points[this.points.length - 1])

      this.points = newPoints
    }
    return this
  }

  /**
   * Calculate the perpendicular offset between two points.
   * @private
   * @param {Vector2} start - The starting point.
   * @param {Vector2} end - The ending point.
   * @param {number} offsetPct - The percentage of the offset.
   * @returns {Vector2} - The calculated offset point.
   */
  perpendicularOffset(start, end, offsetPct) {
    const perpendicular =
      Math.atan2(end.y - start.y, end.x - start.x) - Math.PI / 2.0
    const maxDeviation = (start.subtract(end).length() / 2.0) * offsetPct
    const mid = start.add(end).div(2)
    const offset = random(-maxDeviation, maxDeviation, this.rng)
    return new Vector2(
      mid.x + Math.cos(perpendicular) * offset,
      mid.y + Math.sin(perpendicular) * offset,
    )
  }
}

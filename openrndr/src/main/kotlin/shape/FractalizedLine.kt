package shape

import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment
import org.openrndr.shape.ShapeContour
import java.util.*
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Based on the algorithm used here:
 * http://rectangleworld.com/blog/archives/462
 * and introduced here:
 * http://rectangleworld.com/blog/archives/413
 */

class FractalizedLine(var points: List<Vector2>, private val rng: Random = Random.Default) {
  constructor(other: FractalizedLine) : this(other.points, other.rng)

  val segments: List<Segment>
    get() {
      val segments = mutableListOf<Segment>()
      for (i in 0 until points.size - 1) {
        segments.add(Segment(points[i], points[i+ 1]))
      }
      // This is only needed if the path is closed, which this will never be.
      // Could be a possible enhancement in future: combine FractalizedPolygon and FractalizedLine with a `closed` boolean prop
      // segments.add(Segment(points.last(), points.first()))
      return segments
    }

  val shape: ShapeContour
    get() = ShapeContour(segments, true)

  /**
   * Recursively subdivide the points using perpendicular offset
   */
  fun perpendicularSubdivide(subdivisions: Int, offsetPct: Double = 0.50): FractalizedLine =
    subdivide(subdivisions, offsetPct, ::perpendicularOffset)

  /**
   * Recursively subdivide the points using gaussian offset
   * The default offsetPct here is intentionally lower than perpendicular because it gets _wild_ real quick
   */
  fun gaussianSubdivide(subdivisions: Int, offsetPct: Double = 0.35): FractalizedLine =
    subdivide(subdivisions, offsetPct, ::gaussianOffset)

  /**
   * Recursively subdivide the points
   */
  private fun subdivide(subdivisions: Int, offsetPct: Double = 0.5, offsetFn: (Vector2, Vector2, Double) -> Vector2): FractalizedLine {
    for (i in 0 until subdivisions) {
      val newPoints = mutableListOf<Vector2>()

      // Iterate through all points.
      // Skip the last index because we are accessing j+1 to get the "next" point anyway.
      // The last point will be added after the loop
      for (j in 0 until points.size - 1) {
        val current = points[j]
        val next = points[j + 1]
        val mid = offsetFn(current, next, offsetPct)
        newPoints.add(current)
        newPoints.add(mid)
      }
      newPoints.add(points.last())

      points = newPoints
    }
    return this
  }

  private fun perpendicularOffset(start: Vector2, end: Vector2, offsetPct: Double): Vector2 {
    val perpendicular = atan2(end.y - start.y, end.x - start.x) - (PI / 2.0)
    val maxDeviation = (start - end).length / 2.0 * offsetPct
    val mid = (start + end) / 2.0
    val offset = random(-maxDeviation, maxDeviation, rng)
    return mid + Vector2(cos(perpendicular) * offset, sin(perpendicular) * offset)
  }

  private fun gaussianOffset(start: Vector2, end: Vector2, offsetPct: Double): Vector2 =
    Vector2.gaussian(
      // midpoint
      mean = (start + end) / 2.0,
      deviation = Vector2((start - end).length / 2.0) * offsetPct,
      random = rng
    )

  fun clone(): FractalizedLine = FractalizedLine(this)
}

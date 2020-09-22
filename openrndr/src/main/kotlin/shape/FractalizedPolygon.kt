package shape

import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Segment
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Not sure I love the name of this class, but it is based on the method described here
 * https://tylerxhobbs.com/essays/2017/a-generative-approach-to-simulating-watercolor-paints
 *
 * Open questions:
 * 1. Consider other data structures for points?
 * 2. Do we really need radius or center since we aren't used either in the offsetMid function anymore?
 */

class FractalizedPolygon(points: List<Vector2>, val radius: Double = 1.0, val center: Vector2 = Vector2.ZERO, val rand: Random = Random.Default) {
  constructor(nPoints: Int = 8, radius: Double = 1.0, center: Vector2 = Vector2.ZERO, rand: Random = Random.Default) : this(
    MutableList(nPoints) { n ->
      val angle = map(0.0, nPoints.toDouble(), 0.0, 2.0 * PI, n.toDouble())
      Vector2(cos(angle) * radius, sin(angle) * radius) + center
    },
    radius,
    center,
    rand
  )

  constructor(other: FractalizedPolygon) : this(other.points, other.radius, other.center, other.rand)

  constructor(other: ShapeContour, rand: Random = Random.Default) : this(other.segments.map { it.start }, hypot(other.bounds.height, other.bounds.width) / 2.0, other.bounds.center, rand = rand)

  var points = points

  val segments: List<Segment>
    get() {
      var segments = mutableListOf<Segment>()
      for (i in 0 until points.size - 1) {
        segments.add(Segment(points.get(i), points.get(i + 1)))
      }
      segments.add(Segment(points.last(), points.first()))
      return segments
    }

  val shape: ShapeContour
    get() = ShapeContour(segments, true)

  /**
   * Recursively subdivide the points
   */
  fun fractalize(subdivisions: Int, offsetPct: Double = 0.50): FractalizedPolygon {
    for (i in 0 until subdivisions) {
      var newPoints = mutableListOf<Vector2>()

      for (j in 0 until points.size - 1) {
        val current = points.get(j)
        val next = points.get(j + 1)
        val mid = offsetMid(current, next, offsetPct)
        newPoints.add(current)
        newPoints.add(mid)
      }
      newPoints.add(points.last())
      val mid = offsetMid(points.first(), points.last(), offsetPct)
      newPoints.add(mid)

      points = newPoints
    }
    return this
  }

  private fun offsetMid(start: Vector2, end: Vector2, offsetPct: Double): Vector2 =
    Vector2.gaussian(
      // midpoint
      mean = (start + end) / 2.0,
      // length * offsetPct
      // we optionally take radius/20 if greater, because otherwise clusters of points result in weird dark spots
      // deviation = max(Vector2((start - end).length / 2.0) * offsetPct, Vector2(radius / 20.0)),
      deviation = Vector2((start - end).length / 2.0) * offsetPct,
      random = rand
    )

  fun clone(): FractalizedPolygon = FractalizedPolygon(this)
}

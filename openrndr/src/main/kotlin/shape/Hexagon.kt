package shape

import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import java.lang.Math.toDegrees
import java.lang.Math.toRadians
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class Hexagon(val origin: Vector2, val pointRadius: Double, val rotation: Double = 0.0) {
  // This is a little verbose but basically we're just grabbing 6 evenly spaced points around a circle,
  // starting from "rotation" and going to "2pi + rotation".
  // We convert to degrees because they make much more sense for integer ranges
  val points = (toDegrees(rotation).toInt() until toDegrees(2.0 * PI + rotation).toInt() step 60).map { degrees ->
    val radians = toRadians(degrees.toDouble())
    Vector2(cos(radians) * pointRadius, sin(radians) * pointRadius) + origin
  }

  val contour = ShapeContour.fromPoints(points, closed = true)
}

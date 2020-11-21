package shape

import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Leaf(val start: Vector2, val angle: Double, val size: Double, val rng: Random) {
  val convex: ShapeContour
    get() {
      val end = Vector2(cos(angle), sin(angle)) * size + start
      val ctrl1 =
        // midpoint between start and end
        (end + start) * 0.5 +
          // perpendicular of the leaf angle
          Vector2(cos(angle + PI * 0.5), sin(angle + PI * 0.5)) *
          // some random "width" for the leaf
          size * random(0.175, 0.45, rng)
      // same as ctrl1 but going the other direction
      val ctrl2 = (end + start) * 0.5 +
        Vector2(cos(angle - PI * 0.5), sin(angle - PI * 0.5)) *
        size * random(0.175, 0.45, rng)

      return contour {
        moveTo(start)
        curveTo(ctrl1, end)
        curveTo(ctrl2, start)
        close()
      }
    }
}

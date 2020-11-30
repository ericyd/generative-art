package shape

import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Leaf(val start: Vector2, val angle: Double, val size: Double, val symmetrical: Boolean = true, val rng: Random) {
  // Makes a simple leaf shape, which is basically two quadratic bezier curves
  val convex: ShapeContour
    get() {
      val end = Vector2(cos(angle), sin(angle)) * size + start
      val ctrl1Size = random(0.175, 0.45, rng)
      val ctrl2Size = if (symmetrical) ctrl1Size else random(0.175, 0.45, rng)
      val ctrl1 =
        // midpoint between start and end
        (end + start) * 0.5 +
          // perpendicular of the leaf angle
          Vector2(cos(angle + PI * 0.5), sin(angle + PI * 0.5)) *
          // some random "width" for the leaf
          size * ctrl1Size
      // same as ctrl1 but going the other direction
      val ctrl2 = (end + start) * 0.5 +
        Vector2(cos(angle - PI * 0.5), sin(angle - PI * 0.5)) *
        size * ctrl2Size

      return contour {
        moveTo(start)
        curveTo(ctrl1, end)
        curveTo(ctrl2, start)
        close()
      }
    }

  // Not sure what to call this... basically just another leafy shape
  val halfConvex: ShapeContour
    get() {
      val median = Vector2(cos(angle), sin(angle))
      val end = median * size + start
      val ctrl1Size = random(0.375, 0.65, rng)
      val ctrl2Size = if (symmetrical) ctrl1Size else random(0.175, 0.25, rng)
      val mid = (end + start) * 0.5

      // I wish I could say these control points are something other than tweaking params until it "fits"...
      // The way I developed this was by drawing the control points as circles and seeing where they ended up.
      // It is not intuititve.
      // The commented out lines produce a shaper curve near the edges of the leaf, as an alternative
      val ctrl1a = mid +
        Vector2(cos(angle + PI * 0.5), sin(angle + PI * 0.5)) * size * ctrl1Size
      val ctrl1b = mid +
        median * size * ctrl1Size * 0.5
      // Vector2(cos(angle + PI * 1.875), sin(angle + PI * 1.875)) * size * ctrl1Size * 0.5
      val ctrl2a = mid +
        Vector2(cos(angle + PI * 1.5), sin(angle + PI * 1.5)) * size * ctrl2Size
      val ctrl2b = mid +
        median * size * -ctrl2Size * 0.5
      // Vector2(cos(angle + PI * 0.875), sin(angle + PI * 0.875)) * size * ctrl2Size * 0.5

      return contour {
        moveTo(start)
        curveTo(ctrl1a, ctrl1b, end)
        curveTo(ctrl2a, ctrl2b, start)
        close()
      }
    }
}

package util

import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.shape.Rectangle
import kotlin.math.sqrt

// Disclaimer: I don't know if this is useful or not
/**
 * A Double that is expected (but not guaranteed) to be in the range [0.0, 1.0]
 */
typealias PercentageDouble = Double

interface ConcentrationGradient {
  /**
   * Translate a point in Cartesian space into a percentage,
   * based on the definition of the ConcentrationGradient and
   * where the point is in relation to its bounding rectangle.
   * @param boundingRect the bounding rectangle for `point`
   * @param point the point to assess
   * @return a percentage between 0.0 and 1.0 (inclusive) representing the concentration of the gradient at the given point.
   */
  fun assess(boundingRect: Rectangle, point: Vector2, clamp: Boolean = false): PercentageDouble

  /**
   * Convert the point to a "unit point", where both x and y are in range [0.0, 1.0]
   */
  fun normalizePoint(boundingRect: Rectangle, point: Vector2): Vector2 =
    Vector2(
      (point.x - boundingRect.x) / boundingRect.width,
      (point.y - boundingRect.y) / boundingRect.height
    )
}

/**
 * Creates a 2D gradient f(x,y) where values are interpolated over a circle defined in relation to a unit square.
 * Unit square: (corner at (0,0), width = 1, height = 1)
 * @param center center of the radial gradient
 * @param minRadius the distance from the center at which the gradient f(x,y) = 0.0
 * @param maxRadius the distance from the center at which the gradient f(x,y) = 1.0
 * @param reverse reverses the gradient so center has concentration 1.0 and radius has concentration 0.0
 */
class RadialConcentrationGradient(
  private val center: Vector2 = Vector2.ZERO,
  private val minRadius: Double = 0.0,
  private val maxRadius: Double = sqrt(2.0),
  private val reverse: Boolean = false
) : ConcentrationGradient {
  override fun assess(boundingRect: Rectangle, point: Vector2, clamp: Boolean): PercentageDouble {
    val normalizedPoint = normalizePoint(boundingRect, point)
    // still figuring out if this is the right math, so keeping the original which doesn't use minRadius

    var assessed = (normalizedPoint.distanceTo(center) - minRadius) / (maxRadius - minRadius)
    if (clamp) {
      assessed = clamp(assessed, 0.0, 1.0)
    }
    // debugging...
    // println("===============================================")
    // println("point: $point")
    // println("boundingRect: $boundingRect")
    // println("normalizedPoint: $normalizedPoint")
    // println("center: $center")
    // println("normalizedPoint.distanceTo(center): ${normalizedPoint.distanceTo(center)}")
    // println(normalizedPoint.distanceTo(center) - minRadius)
    return if (reverse) {
      // 1.0 - normalizedPoint.distanceTo(center) / maxRadius
      1.0 - assessed
    } else {
      // normalizedPoint.distanceTo(center) / maxRadius
      assessed
    }
  }

  companion object {
    val default = RadialConcentrationGradient()
  }
}

/**
 * Creates a 2D gradient f(x,y) where values at the corners of the unit square determine the concentration in space.
 * Calculates gradient using bilinear interpolation of four points, assumed to be on the unit square
 * @param upperLeft f(0,0)
 * @param upperRight f(1,0)
 * @param lowerLeft f(0,1)
 * @param lowerRight f(1,1)
 */
class BilinearConcentrationGradient(
  private val upperLeft: Double = 0.0,
  private val upperRight: Double = 0.0,
  private val lowerLeft: Double = 0.0,
  private val lowerRight: Double = 0.0
) : ConcentrationGradient {
  override fun assess(boundingRect: Rectangle, point: Vector2, clamp: Boolean): PercentageDouble {
    val normalizedPoint = normalizePoint(boundingRect, point)
    return bilinearInterp(upperLeft, upperRight, lowerLeft, lowerRight, normalizedPoint)
  }

  companion object {
    /**
     * gradient goes from high (top left) to low (bottom right)
     */
    val default = BilinearConcentrationGradient(1.0, 0.5, 0.5, 0.0)
    /**
     * possibly confusingly named... gradient goes from high (bottom) to low (top)
     */
    val fadeUp = BilinearConcentrationGradient(0.0, 0.0, 1.0, 1.0)
    /**
     * possibly confusingly named... gradient goes from high (top) to low (bottom)
     */
    val fadeDown = BilinearConcentrationGradient(1.0, 1.0, 0.0, 0.0)
  }
}

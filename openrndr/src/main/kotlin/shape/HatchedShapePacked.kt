// This is a hatched shape with cross hatches coming from a circle packing algorithm
package shape

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.shape.Composition
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import org.openrndr.shape.difference
import org.openrndr.shape.drawComposition
import org.openrndr.shape.intersection
import util.BilinearConcentrationGradient
import util.CirclePack
import util.ConcentrationGradient
import util.packCirclesOnGradient
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Class to create a list of (cross) hatch segments for a given ShapeContour using circle packing
 */
class HatchedShapePacked(
  val shape: ShapeContour,
  private val primaryAngleRange: ClosedFloatingPointRange<Double> = 0.0..PI,
  private val secondaryAngleRange: ClosedFloatingPointRange<Double> = (PI * 0.25)..(PI * 0.5),
  private val includeCrossHatch: Boolean = true,
  private val maxFailedAttempts: Int = Short.MAX_VALUE.toInt(),
  val rng: Random = Random.Default
) {
  // lol I should probably have fewer params... BUT I LIKE THEM ALL SO MUCH!
  fun hatchedShape(
    radiusRange: ClosedFloatingPointRange<Double> = 0.5..5.0,
    hatchLength: Double = 10.0,
    strokeWeight: Double = 0.2,
    strokeColor: ColorRGBa = ColorRGBa.BLACK.opacify(0.5),
    primaryAngle: Double? = null,
    secondaryAngle: Double? = null,
    intersectionContours: List<ShapeContour>? = null,
    differenceContours: List<ShapeContour>? = null,
    gradient: ConcentrationGradient = BilinearConcentrationGradient.fadeUp,
    circlePack: CirclePack? = null
  ): Pair<ShapeContour, Composition> {
    val circles = circlePack ?: packCircles(radiusRange, shape.bounds, gradient)
    val composition = drawComposition {
      this.strokeWeight = strokeWeight
      fill = null
      stroke = strokeColor
      circles.forEach {
        val angle1 = normalizeAngle(primaryAngle) ?: random(primaryAngleRange.start, primaryAngleRange.endInclusive, rng)
        val angle2 = normalizeAngle(secondaryAngle) ?: angle1 + random(secondaryAngleRange.start, secondaryAngleRange.endInclusive, rng)
        val angles = if (includeCrossHatch) listOf(angle1, angle2) else listOf(angle1)
        angles.forEach { angle ->
          val hatch = contour {
            moveTo(it.center + Vector2(cos(angle) * hatchLength * 0.5, sin(angle) * hatchLength * 0.5))
            lineTo(it.center + Vector2(cos(angle + PI) * hatchLength * 0.5, sin(angle + PI) * hatchLength * 0.5))
          }
          var tempShape = intersection(hatch, shape)
          intersectionContours?.forEach {
            tempShape = intersection(tempShape, it)
          }
          differenceContours?.forEach {
            tempShape = difference(tempShape, it)
          }
          shape(tempShape)
        }
      }
    }
    return Pair(shape, composition)
  }

  /**
   * Generate list of circles via circle packing algorithm.
   */
  private fun packCircles(radiusRange: ClosedFloatingPointRange<Double>, boundingRect: Rectangle, gradient: ConcentrationGradient): CirclePack =
    packCirclesOnGradient(radiusRange, boundingRect, gradient, maxFailedAttempts, rng)

  private fun normalizeAngle(angle: Double?): Double? {
    if (angle == null) return angle
    var result = angle
    while (result < 0.0) {
      result += 2.0 * PI
    }
    return result
  }
}

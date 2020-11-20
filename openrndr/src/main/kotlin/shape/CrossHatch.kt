package shape

import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import org.openrndr.shape.drawComposition
import org.openrndr.shape.intersection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

// This works!
//
// But we always gotta be interating, so...
//
// Ideas for V2:
// Use circle packing to determine cross hatch center
// Areas of darker shading will have higher density of circle packs
// If the circle center is outside the bounds, throw it away
// From the center of the circle, draw a line of the desired length
// Check the endpoints of the line. If an endpoint is in the acceptable bounds.
//   if yes:
//     accept line
//   if no:
//     cut it in half between the point and the center of the line.
//     Check if the point is in acceptable bounds
//     If yes:
//       place the endpoint halfway between current position and the original endpoint.
//       Repeat algorithm, cutting in half each time it is outside bounds.
//       Repeat X times and this will get very close to the boundary
//     If no:
//       Cut in half again and repeat algorithm

/**
 * Class to create a list of cross hatch segments for a given ShapeContour
 */
class CrossHatch(val shapes: List<ShapeContour>, private val spacing: Int? = null, private val primaryAngle: Double? = null, val rng: Random = Random.Default) {
  val QUARTER_PI = PI * 0.25
  val THREE_QUARTER_PI = PI * 0.75
  val FIVE_QUARTER_PI = PI * 1.25
  val SEVEN_QUARTER_PI = PI * 1.75

  // Pattern for hatch marks is substantially more complex
  // Comments are in line but the essence is that we need to
  // draw a bunch of hash marks at two different angles,
  // then find the intersection of the marks and the leaf shape.
  val hatches: List<Pair<ShapeContour, List<Segment>>>
    get() {
      return shapes.map(::generateHatchesForShape)
    }

  private fun generateHatchesForShape(shape: ShapeContour): Pair<ShapeContour, List<Segment>> {
    // Spacing of the hatch marks
    val space = spacing ?: random(4.0, 10.0, rng).toInt()

    // Generate hatch mark angles.
    // IMO strictly perpendicular hatching angles don't look as nice
    val angle1 = normalizeAngle(primaryAngle) ?: random(0.0, PI, rng)
    val angle2 = angle1 + random(PI * 0.25, PI * 0.5, rng)

    // determine a max length for the hatch marks
    // (they will be trimmed to size with the CompoundBuilder)
    val length = hypot(shape.bounds.width, shape.bounds.height) * 1.5

    // Get a list of the start points for the hatch marks.
    // Since we are creating them oversized and trimming them with CompoundBuilder,
    // we can just start all of the marks along the border of the bounding rectangle of the shape
    val sideStarts = generateLeftRightStarts(shape, space)
    val topStarts = generateTopBottomStarts(shape, space)
    val startPositionAnglePairs = combineStartsWithAngles(angle1, angle2, sideStarts, topStarts)

    // create hash marks for both angles
    // I suspect there is an easier way to combine these in the Compound Builder
    // rather than drawing them separately and combining,
    // but I don't know what it is off the top of my head
    val hatches = startPositionAnglePairs.flatMap { (angle, start) -> generateHatches(start, angle, length, shape) }

    return Pair(shape, hatches)
  }

  // we should only draw a hatch for a start point if the angle is appropriate for it.
  // That is, angles between [PI/4, -PI/4] or [3*PI/4, 5*PI/4] should be drawn from the sides, and
  // angles between [PI/4, 3*PI/4] or [5*PI/4, 7*PI/4] should be drawn from the top/bottom
  //
  // This function is pretty bad but it works so :shrug:
  private fun combineStartsWithAngles(angle1: Double, angle2: Double, sideStarts: List<Vector2>, topStarts: List<Vector2>): List<Pair<Double, Vector2>> {
    val angle1Pairs = if (angle1 < QUARTER_PI || angle1 >= SEVEN_QUARTER_PI || (angle1 > THREE_QUARTER_PI && angle1 <= FIVE_QUARTER_PI)) {
      //  assign to side starts
      sideStarts.map { Pair(angle1, it) }
    } else {
      //  assign to top starts
      topStarts.map { Pair(angle1, it) }
    }

    val angle2Pairs = if (angle2 < QUARTER_PI || angle2 >= SEVEN_QUARTER_PI || (angle2 > THREE_QUARTER_PI && angle2 <= FIVE_QUARTER_PI)) {
      //  assign to side starts
      sideStarts.map { Pair(angle2, it) }
    } else {
      //  assign to top starts
      topStarts.map { Pair(angle2, it) }
    }

    return angle1Pairs + angle2Pairs
  }

  private fun generateHatches(start: Vector2, angle: Double, length: Double, shapeContour: ShapeContour): List<Segment> {
    val end = Vector2(start.x + cos(angle) * length, start.y + sin(angle) * length)

    // Would be simpler to do this
    //   val hatch = LineSegment(start, end)
    // but apparently compoundBuilder only works with closed contours/shapes
    val hatch = contour {
      moveTo(start)
      lineTo(end)
      close()
    }
    val composition = drawComposition {
      shape(
        intersection(
          // These are both ShapeContours so calling clockwise shouldn't be necessary according to the advertised
          // functionality of CompositionDrawer, but it is the only way I can get it to work
          hatch.clockwise,
          shapeContour.clockwise
        )
      )
    }

    // Get the first segment from each shape
    // This assumes that the first segment will be along the long edge of the hatch,
    // which is definitely not guaranteed,
    // but it seems reasonably stable so we're going with it
    return composition
      .findShapes()
      .map {
        it.shape.contours.flatMap { it.segments }.first()
      }
  }

  // Generate start points along left and right borders of the ShapeContour's bounding box
  private fun generateLeftRightStarts(shape: ShapeContour, spacing: Int): List<Vector2> =
    // ((shape.bounds.y - shape.bounds.height * 1.5).toInt() until (shape.bounds.y + shape.bounds.height * 2.5).toInt() step spacing).map { startY ->
    //   Vector2(shape.bounds.x, startY.toDouble())
    // }
    // (shape.bounds.y.toInt() until (shape.bounds.y + shape.bounds.height).toInt() step spacing).flatMap { startY ->
    //
    // This is the best one, though still not 100% perfect
    ((shape.bounds.y - shape.bounds.height * 1.5).toInt() until (shape.bounds.y + shape.bounds.height * 2.5).toInt() step spacing).flatMap { startY ->
      listOf(
        Vector2(shape.bounds.x, startY.toDouble()),
        Vector2(shape.bounds.x + shape.bounds.width, startY.toDouble())
      )
    }

  // Generate start points along top and bottom of the ShapeContour's bounding box
  private fun generateTopBottomStarts(shape: ShapeContour, spacing: Int): List<Vector2> =
    // ((shape.bounds.x - shape.bounds.width * 1.5).toInt() until (shape.bounds.x + shape.bounds.width * 2.5).toInt() step spacing).map { startX ->
    //   Vector2(startX.toDouble(), shape.bounds.y)
    // }
    // (shape.bounds.x.toInt() until (shape.bounds.x + shape.bounds.width).toInt() step spacing).flatMap { startY ->
    //
    // This is the best one, though still not 100% perfect
    ((shape.bounds.x - shape.bounds.width * 1.5).toInt() until (shape.bounds.x + shape.bounds.width * 2.5).toInt() step spacing).flatMap { startX ->
      listOf(
        Vector2(startX.toDouble(), shape.bounds.y),
        Vector2(startX.toDouble(), shape.bounds.y + shape.bounds.height)
      )
    }

  private fun normalizeAngle(angle: Double?): Double? {
    if (angle == null) return angle
    var result = angle
    while (result < 0.0) {
      result += 2.0 * PI
    }
    return result
  }
}

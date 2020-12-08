@file:Suppress("NAME_SHADOWING") // https://stackoverflow.com/a/53111863/3991555
package util

import force.MovingBody
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import javax.swing.Spring.height
import kotlin.random.Random


typealias CirclePack = List<Circle>

/**
 * Check for intersection of two circles
 * Similar to native "contains" method, but checks against other circles instead of other point
 */
fun Circle.intersects(other: Circle): Boolean =
  this.center.distanceTo(other.center) < (this.radius + other.radius)

/**
 * Generate list of circles via circle packing algorithm.
 * 1. Choose random position within bounds of shape
 * 2. Check if any other circle contains this circle
 *    Yes -> increment failed attempts counter and continue
 *    No -> draw circle
 */
fun packCirclesOnGradient(
  radiusRange: ClosedFloatingPointRange<Double>,
  boundingRect: Rectangle,
  gradient: ConcentrationGradient,
  maxFailedAttempts: Int = Short.MAX_VALUE.toInt(),
  rng: Random = Random.Default,
): CirclePack {
  val circles = mutableListOf<Circle>()
  var failedAttempts = 0
  while (failedAttempts < maxFailedAttempts) {
    val position = Vector2(
      random(boundingRect.x, boundingRect.x + boundingRect.width, rng),
      random(boundingRect.y, boundingRect.y + boundingRect.height, rng)
    )
    // endInclusive and start are "reversed" here, because a gradient's lowest concentration maps to 0.0,
    // and that actually correlates to where we want the circles to be **most** spaced out.
    // That means we need low concentration to map to high radius, hence the reverse.
    val radius = map(
      0.0, 1.0,
      radiusRange.endInclusive, radiusRange.start,
      gradient.assess(boundingRect, position)
    )
    val circle = Circle(position, radius)

    if (circles.any { it.intersects(circle) }) {
      failedAttempts++
      continue
    }

    // this is better for some circle packing but it makes this take **forever** and I'm impatient
    // failedAttempts = 0
    circles.add(circle)
  }
  return circles
}


// Based on
// http://www.codeplastic.com/2017/09/09/controlled-circle-packing-with-processing/
// algorithm
// [1] do while any circles have velocity:
//   [2] for all circles, apply separation forces
//   [3] for all circles, zero the circle's velocity if it isn't intersecting any others
// [4] return circles
fun packCirclesControlled(
  nCircles: Int = 0,
  center: Vector2 = Vector2.ZERO,
  initialRadius: Double = 1.0,
  circles: List<MovingBody>? = null
): List<MovingBody> {
  val circles = circles ?: List(nCircles) { MovingBody(center, radius = initialRadius) }

  // [1]
  do {
    // [2]
    circles.forEach { primary ->
    //  TODO: calculate separation forces
    }

    // [3]
    circles.forEach { primary ->
      if (circles.any { other -> other != primary && other.intersects(primary) }) {
        primary.velocity = Vector2.ZERO
      }
    }
  } while (circles.any { it.velocity != Vector2.ZERO })

  // [4]
  return circles
}

// Enhancement: check if circle is outside window bounds and bounce it back
//
// if (circle_i.position.x - circle_i.radius / 2 < 0 || circle_i.position.x + circle_i.radius / 2 > width) {
//   circle_i.velocity.x *= -1
//   circle_i.update()
// }
// if (circle_i.position.y - circle_i.radius / 2 < 0 || circle_i.position.y + circle_i.radius / 2 > height) {
//   circle_i.velocity.y *= -1
//   circle_i.update()
// }

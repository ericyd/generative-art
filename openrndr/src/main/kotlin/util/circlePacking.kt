@file:Suppress("NAME_SHADOWING") // https://stackoverflow.com/a/53111863/3991555
package util

import force.MovingBody
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.math.sqrt
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
  clamp: Boolean = false
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
      gradient.assess(boundingRect, position, clamp)
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

/**
 * Generate list of circles via circle packing algorithm.
 * 1. Choose random position within bounds of shape
 * 2. Check if any other circle contains this circle
 *    Yes -> increment failed attempts counter and continue
 *    No -> draw circle
 */
fun packCirclesInRectangle(
  radiusRange: ClosedFloatingPointRange<Double>,
  boundingRect: Rectangle,
  maxFailedAttempts: Int = Short.MAX_VALUE.toInt(),
  rng: Random = Random.Default
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
    val radius = random(radiusRange.endInclusive, radiusRange.start, rng)
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

/**
 * Creates list of <MovingBody>s that can be fed into `packCirclesControlled`
 * @param nBodies number of bodies to generate
 * @param center where to place the generated bodies
 * @param initialRadius how large to generate the bodies
 */
fun generateMovingBodies(nBodies: Int, center: Vector2 = Vector2.ZERO, initialRadius: Double = 1.0): List<MovingBody> =
  List(nBodies) { MovingBody(center, radius = initialRadius) }

/**
 * Includes a list of packed MovingBodies, and a Boolean to indicate if they are done packing.
 * When the second parameter is true, the bodies will not move any more
 */
data class PackCompleteResult(val bodies: List<MovingBody>, val isComplete: Boolean = false)

/**
 * Based on
 * http://www.codeplastic.com/2017/09/09/controlled-circle-packing-with-processing/
 * algorithm
 * [1] do while any bodies have velocity:
 *   [2] for all bodies, apply separation forces
 *   [3] for all bodies, zero the circle's velocity if it isn't intersecting any others
 *   [4] adjust size of bodies based on the `sizeFn` definition
 * [5] return bodies
 *
 * @param bodies provide a list of MovingBodies to pack
 * @param incremental if true, returns only a single iteration of the "separating" process
 * @param rng randomizer for when bodies are on top of each other and need a nudge
 * @param sizeFn function that is called each iteration to dynamically adjust the size of the bodies
 */
fun packCirclesControlled(
  bodies: List<MovingBody>,
  incremental: Boolean = false,
  rng: Random = Random.Default,
  sizeFn: ((MovingBody) -> Unit) = { _: MovingBody -> }
): PackCompleteResult {
  var packComplete = false
  // [1]
  do {
    // [2]
    bodies.forEach { primary ->

      // only apply separation forces to circles that intersect
      val intersectingCircles = bodies.filter { it != primary && it.intersects(primary) }
      for (other in intersectingCircles) {
        val dist = other.position.distanceTo(primary.position)
        // If the distance is 0, then they are on top of each other and just need a random nudge
        val force = if (dist > 0.0) {
          // wow. This is BY FAR the biggest slowdown in this algorithm.
          // dividing by the distance makes it really nice and smooth but goddamn it slows it down like a m****f****
          //  Original: (primary.position - other.position).normalized / dist
          //  Also tried: (primary.position - other.position).normalized
          //  Final version was chosen for a mix of speed and accuracy
          (primary.position - other.position).normalized / sqrt(dist)
        } else {
          Vector2.gaussian(random = rng)
        }

        // do not update the `primary` until we've assessed all other circles
        primary.applyForce(force)

        // applying force immediately to secondary seems to speed up the "settling time" quite a bit
        other.applyForce(force * -1.0).update()
      }

      // scaling by number of intersecting circles prevents wacky values
      primary.applyFriction(intersectingCircles.size.toDouble()).update()
    }

    // [3]
    bodies.forEach { primary ->
      if (bodies.none { other -> other != primary && other.intersects(primary) }) {
        primary.velocity = Vector2.ZERO
      }
      // [4]
      sizeFn(primary)
    }
    packComplete = bodies.all { it.velocity == Vector2.ZERO }
  } while (!incremental && !packComplete)

  // [5]
  return PackCompleteResult(bodies, packComplete)
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

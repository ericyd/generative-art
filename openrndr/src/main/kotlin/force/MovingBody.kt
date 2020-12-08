package force

import org.openrndr.math.Vector2

/**
 * An even-more-general-purpose moving body.
 * Inspired by: http://www.codeplastic.com/2017/09/09/controlled-circle-packing-with-processing/
 */
class MovingBody(
  var position: Vector2,
  var velocity: Vector2 = Vector2.ZERO,
  var acceleration: Vector2 = Vector2.ZERO,
  // a body can have a circular shape I guess?
  // not really convinced this makes sense from a "generic" standpoint,
  // but I think defaulting to zero (point body) makes this OK
  var radius: Double = 0.0
) {
  fun applyForce(force: Vector2): MovingBody {
    acceleration += force
    return this
  }

  fun update(): MovingBody {
    velocity += acceleration
    position += velocity
    acceleration = Vector2.ZERO
    return this
  }

  fun stop(): MovingBody {
    velocity = Vector2.ZERO
    acceleration = Vector2.ZERO
    return this
  }

  fun intersects(other: MovingBody): Boolean =
    position.distanceTo(other.position) < (radius + other.radius)
}

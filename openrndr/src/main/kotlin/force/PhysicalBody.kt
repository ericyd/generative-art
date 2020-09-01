package force

import org.openrndr.math.Vector2

/**
 * General purpose physical body in motion.
 * I bet $1,000 that I will rename this class in the future.
 *
 * @param coords the starting point of the body
 * @param mass the mass of the body
 * @param speed the speed with which it moves towards it's target
 * @param target the location in space which the body aims towards
 */
class PhysicalBody(coords: Vector2, val mass: Double, val speed: Double = 0.0, val target: Vector2 = Vector2.ZERO) {
  var coords = coords

  constructor(x: Int, y: Int, mass: Double, speed: Double = 0.0, target: Vector2 = Vector2.ZERO) :
    this(Vector2(x.toDouble(), y.toDouble()), mass, speed, target)

  fun move(system: GravitySystem): Vector2 {
    val extrinsicForce = system.forceRaw(coords, mass)
    // meh, just gonna call this force even though it's not
    val intrinsicForce = (target - coords).normalized * speed
    val movement = (extrinsicForce + intrinsicForce).normalized

    coords += movement
    return coords
  }

  fun spiral(system: GravitySystem, scale: Double = 1.0): Vector2 {
    val perpendicular = coords + coords.perpendicular() * scale
    val extrinsicForce = system.forceRaw(perpendicular, mass)

    // meh, just gonna call this force even though it's not
    val intrinsicForce = perpendicular.normalized * speed
    val movement = (extrinsicForce + intrinsicForce).normalized

    coords += movement
    return coords
  }

  fun spiral2(system: GravitySystem, scale: Double, center: Vector2 = Vector2.ZERO): Vector2 {
    val pos = coords - center
    val extrinsicForce = system.spiralRaw(pos, mass, scale = scale)
    val intrinsicForce = (extrinsicForce - pos).normalized.perpendicular() * speed
    val movement = (extrinsicForce + intrinsicForce).normalized

    coords += movement
    return coords
  }

  fun orbit(system: GravitySystem, scale: Double, center: Vector2 = Vector2.ZERO): Vector2 {
    val pos = coords - center
    val extrinsicForce = system.orbitRaw(pos, mass, scale = scale)
    val intrinsicForce = (extrinsicForce - pos).normalized.perpendicular() * speed
    val movement = (extrinsicForce + intrinsicForce).normalized

    coords += movement
    return coords
  }
}

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
class PhysicalBody(coords: Vector2, val mass: Double, val speed: Double, val target: Vector2) {
  var coords = coords

  constructor(x: Int, y: Int, mass: Double, speed: Double, target: Vector2) :
    this(Vector2(x.toDouble(), y.toDouble()), mass, speed, target)

  fun move(system: GravitySystem): Vector2 {
    val extrinsicForce = system.forceRaw(coords, mass)
    // mixing speed and force...?
    // should I multiply velocity by mass to get momentum?
    // Is momentum ~= force if each calculation is per 1s?
    val intrinsicVelocity = unitVector(target - coords) * speed
    val movement = unitVector(extrinsicForce + intrinsicVelocity)

    coords += movement
    return coords
  }
}

package force

import org.openrndr.math.Vector2
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * GravityBodies are a point in space with a mass that exert gravitational forces on other points.
 * @param x x coordinate of the body
 * @param y y coordinate of the body
 * @param mass a unitless "mass" of the body
 */
open class GravityBody(val x: Double, val y: Double, val mass: Double) {
  constructor(origin: Vector2, mass: Double) : this(origin.x, origin.y, mass)

  /**
   * Calculate the distance from this body to another point
   * This corresponds to the "radius" in most Gravitational force equations
   */
  protected fun distance(x2: Double, y2: Double): Double =
    hypot(x2 - x, y2 - y)

  /**
   * Ccalculate the angle from the GravityBody (origin) to the given point
   */
  private fun angle(x2: Double, y2: Double): Double =
    atan2(y2 - y, x2 - x)

  /**
   * Calculate the gravitational force on the body.
   * This is NOT separated by x/y component
   */
  open fun force(g: Double, x2: Double, y2: Double) =
    -g * mass / distance(x2, y2).pow(2.0)

  // Based on
  // https://en.wikipedia.org/wiki/Newton%27s_law_of_universal_gravitation
  // However, not sure if the radius (self.distance) should be raised to the
  // 2nd or 3rd power. In the Three body problem, it is raised to the third power.
  // Should the power be equal to the number of bodies in the problem?
  // Gut says "yes" but experimentally it doesn't improve the look so I'm going to skip it.
  //
  // Note that `g` is the "gravitational constant" and therefore doesn't totally make sense
  // as a parameter to this method, but typically GravityBodies are used within a GravitySystem
  // which takes ownership of the gravitational constant and passes as needed.
  // Also I think I'm missing a sign somewhere... something about attraction and negative forces.
  // Not sure, but this works.
  /**
   * Calculate the x-component of the gravitational force that
   * this body exerts on point mass at (x2,y2)
   */
  fun forceX(g: Double, x2: Double, y2: Double): Double =
    force(g, x2, y2) * cos(angle(x2, y2))

  // If discontinuities arise in the resulting field, try `cos` instead of `sin`.
  // I used cos in the original Rust implementation, but I believe that was because
  // I was using atan() instead of atan2()
  /**
   * Calculate the y-component of the gravitational force that
   * this body exerts on point mass at (x2,y2)
   */
  fun forceY(g: Double, x2: Double, y2: Double): Double =
    force(g, x2, y2) * sin(angle(x2, y2))
}

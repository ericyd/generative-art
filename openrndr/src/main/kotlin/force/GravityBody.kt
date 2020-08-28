package force

import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * GravityBodies are a point in space with a mass that exert gravitational forces on other points.
 * @param x x coordinate of the body
 * @param y y coordinate of the body
 * @param mass a unitless "mass" of the body
 */
open class GravityBody(val x: Double, val y: Double, val mass: Double, val rand: Random = Random.Default, val polarityOption: YPolarity? = null) {
  constructor(origin: Vector2, mass: Double, rand: Random) : this(origin.x, origin.y, mass, rand)
  var origin = Vector2(x, y)
  val polarity = polarityOption ?: if (random(-1.0, 1.0, rand) < 0.0) YPolarity.CCW_POSITIVE_Y else YPolarity.CW_NEGATIVE_Y
  val perpendicular = origin.perpendicular(polarity)

  fun equals(other: GravityBody): Boolean =
    other?.origin == origin && other?.mass == mass

  /**
   * Calculate the distance from this body to another point
   * This corresponds to the "radius" in most Gravitational force equations
   */
  protected fun distance(x2: Double, y2: Double): Double =
    hypot(x2 - origin.x, y2 - origin.y)

  /**
   * Ccalculate the angle from the GravityBody (origin) to the given point
   */
  private fun angle(x2: Double, y2: Double): Double =
    atan2(y2 - origin.y, x2 - origin.x)

  private fun angle(other: Vector2): Double =
    angle(other.x, other.y)

  /**
   * Calculate the gravitational force on the body.
   * This is NOT separated by x/y component
   */
  open fun force(g: Double, x2: Double, y2: Double, m2: Double = 1.0) =
    -g * mass * m2 / distance(x2, y2).pow(2.0)

  open fun force(g: Double, other: Vector2, m2: Double = 1.0) =
    force(g, other.x, other.y, m2)

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
  fun forceX(g: Double, x2: Double, y2: Double, m2: Double = 1.0): Double =
    force(g, x2, y2, m2) * cos(angle(x2, y2))

  // If discontinuities arise in the resulting field, try `cos` instead of `sin`.
  // I used cos in the original Rust implementation, but I believe that was because
  // I was using atan() instead of atan2()
  /**
   * Calculate the y-component of the gravitational force that
   * this body exerts on point mass at (x2,y2)
   */
  fun forceY(g: Double, x2: Double, y2: Double, m2: Double = 1.0): Double =
    force(g, x2, y2, m2) * sin(angle(x2, y2))

  fun spiralX(g: Double, other: Vector2, m2: Double = 1.0, scale: Double = 1.0): Double {
    val other = other + other.perpendicular(polarity) * scale
    return force(g, other, m2) * cos(angle(other))
  }

  fun spiralY(g: Double, other: Vector2, m2: Double = 1.0, scale: Double = 1.0): Double {
    val other = other + other.perpendicular(polarity) * scale
    return force(g, other, m2) * sin(angle(other))
  }

  fun orbitX(g: Double, other: Vector2, m2: Double = 1.0, scale: Double = 1.0): Double {
    val other = other.perpendicular(polarity) * scale
    return force(g, other, m2) * cos(angle(other))
  }

  fun orbitY(g: Double, other: Vector2, m2: Double = 1.0, scale: Double = 1.0): Double {
    val other = other.perpendicular(polarity) * scale
    return force(g, other, m2) * sin(angle(other))
  }
}

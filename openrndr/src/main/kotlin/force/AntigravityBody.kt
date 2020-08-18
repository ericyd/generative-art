package force

/**
 * GravityBodies are a point in space with a mass that exert gravitational forces on other points.
 * @param x x coordinate of the body
 * @param y y coordinate of the body
 * @param mass a unitless "mass" of the body
 */
class AntigravityBody(x: Double, y: Double, mass: Double) : GravityBody(x, y, mass) {
  /**
   * Calculate the gravitational force on the body.
   * This is NOT separated by x/y component.
   * Antigravity uses positive `g` instead of negative `g`
   */
  override fun force(g: Double, x2: Double, y2: Double, m2: Double) = super.force(-g, x2, y2, m2)
}

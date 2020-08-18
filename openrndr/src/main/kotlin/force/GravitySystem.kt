package force

import org.openrndr.math.Vector2

class GravitySystem(val g: Double, val bodies: List<GravityBody>) {
  fun forceX(x2: Double, y2: Double, m2: Double, effectiveBodies: List<GravityBody>): Double =
    effectiveBodies.fold(
      0.0,
      { total: Double, body: GravityBody ->
        total + body.forceX(g, x2, y2, m2)
      }
    )

  fun forceY(x2: Double, y2: Double, m2: Double, effectiveBodies: List<GravityBody>): Double =
    effectiveBodies.fold(
      0.0,
      { total: Double, body: GravityBody ->
        total + body.forceY(g, x2, y2, m2)
      }
    )

  fun force(other: Vector2, m2: Double = 1.0, overrideBodies: List<GravityBody>? = null): Vector2 =
    unitVector(forceRaw(other, m2, overrideBodies))

  fun force(x2: Double, y2: Double, m2: Double = 1.0, overrideBodies: List<GravityBody>? = null): Vector2 =
    force(Vector2(x2, y2), m2, overrideBodies)

  fun forceRaw(other: Vector2, m2: Double = 1.0, overrideBodies: List<GravityBody>? = null): Vector2 {
    val effectiveBodies = overrideBodies ?: bodies
    val fX = forceX(other.x, other.y, m2, effectiveBodies)
    val fY = forceY(other.x, other.y, m2, effectiveBodies)
    return Vector2(fX, fY)
  }

  fun nextTick() {
    for (body in bodies) {
      val others = bodies.filter { it != body }
      val current = body.origin
      val next = current + force(current, body.mass, others)
      body.origin = next
    }
  }
}

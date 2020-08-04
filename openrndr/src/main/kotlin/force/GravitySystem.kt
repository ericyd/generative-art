package force

import org.openrndr.math.Vector2
import kotlin.math.hypot

class GravitySystem(val g: Double, val bodies: List<GravityBody>) {
  fun forceX(x2: Double, y2: Double): Double =
    bodies.fold(
      0.0,
      { total: Double, body: GravityBody ->
        total + body.forceX(g, x2, y2)
      }
    )

  fun forceY(x2: Double, y2: Double): Double =
    bodies.fold(
      0.0,
      { total: Double, body: GravityBody ->
        total + body.forceY(g, x2, y2)
      }
    )

  fun force(x2: Double, y2: Double): Vector2 {
    val fX = forceX(x2, y2)
    val fY = forceY(x2, y2)
    val dist = hypot(fX, fY) // normalize!
    return Vector2(fX / dist, fY / dist)
  }

  fun force(other: Vector2): Vector2 = force(other.x, other.y)
}

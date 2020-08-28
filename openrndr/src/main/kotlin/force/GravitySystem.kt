package force

import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import kotlin.random.Random

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

  fun spiralX(other: Vector2, m2: Double, effectiveBodies: List<GravityBody>, rand: Random, scale: Double): Double =
    effectiveBodies.fold(
      0.0,
      { total: Double, body: GravityBody ->
        val pos = if (random(-1.0, 1.0, random = rand) < 0.0) other.perpendicular(YPolarity.CCW_POSITIVE_Y) else other.perpendicular(YPolarity.CW_NEGATIVE_Y)
        total + body.forceX(g, pos.x, pos.y, m2)
      }
    )

  fun spiralY(other: Vector2, m2: Double, effectiveBodies: List<GravityBody>, rand: Random, scale: Double): Double =
    effectiveBodies.fold(
      0.0,
      { total: Double, body: GravityBody ->
        val pos = if (random(-1.0, 1.0, random = rand) < 0.0) other.perpendicular(YPolarity.CCW_POSITIVE_Y) else other.perpendicular(YPolarity.CW_NEGATIVE_Y)
        total + body.forceY(g, pos.x, pos.y, m2)
      }
    )

  fun force(other: Vector2, m2: Double = 1.0, overrideBodies: List<GravityBody>? = null): Vector2 =
    (forceRaw(other, m2, overrideBodies)).normalized

  fun force(x2: Double, y2: Double, m2: Double = 1.0, overrideBodies: List<GravityBody>? = null): Vector2 =
    force(Vector2(x2, y2), m2, overrideBodies)

  fun forceRaw(other: Vector2, m2: Double = 1.0, overrideBodies: List<GravityBody>? = null): Vector2 {
    val effectiveBodies = overrideBodies ?: bodies
    val fX = forceX(other.x, other.y, m2, effectiveBodies)
    val fY = forceY(other.x, other.y, m2, effectiveBodies)
    return Vector2(fX, fY)
  }

  fun spiralRaw(other: Vector2, m2: Double = 1.0, overrideBodies: List<GravityBody>? = null, rand: Random = Random.Default, scale: Double = 1.0): Vector2 {
    val effectiveBodies = overrideBodies ?: bodies
    val fX = spiralX(other, m2, effectiveBodies, rand, scale)
    val fY = spiralY(other, m2, effectiveBodies, rand, scale)
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

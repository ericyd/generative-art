package shape

import org.openrndr.math.Vector2
import org.openrndr.math.map
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun ellipse(center: Vector2, width: Double, height: Double, resolution: Int): List<Vector2> =
  List(resolution) {
    val angle = map(0.0, resolution.toDouble(), 0.0, 2.0 * PI, it.toDouble())
    Vector2(
      width / 2.0 * cos(angle) + center.x,
      height / 2.0 * sin(angle) + center.y
    )
  }

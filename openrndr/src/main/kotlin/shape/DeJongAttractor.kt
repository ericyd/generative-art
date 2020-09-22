package shape

import org.openrndr.math.Vector2
import kotlin.math.cos
import kotlin.math.sin

class DeJongAttractor(points: List<Vector2>, params: Map<String, Double>) : Attractor2D(points, params) {
  override fun nextPoint(point: Vector2): Vector2 =
    Vector2(
      sin(params["a"]!! * point.y) - cos(params["b"]!! * point.x),
      sin(params["c"]!! * point.x) - cos(params["d"]!! * point.y)
    )
}

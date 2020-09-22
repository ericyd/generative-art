package shape

import org.openrndr.math.Vector2

abstract class Attractor2D(initialPoints: List<Vector2>, val params: Map<String, Double>) {
  var lines = initialPoints.map {
    mutableListOf(nextPoint(it))
  }

  val points: List<Vector2>
    get() {
      return lines.flatten()
    }

  abstract fun nextPoint(point: Vector2): Vector2

  open fun addNext() {
    lines.forEach { l ->
      l.add(nextPoint(l.last()))
    }
  }
}

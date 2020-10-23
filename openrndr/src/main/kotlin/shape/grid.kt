package shape

import org.openrndr.math.Vector2

fun <T : Any> grid(xMin: Int, xMax: Int, yMin: Int, yMax: Int, stepSize: Int = 1, fn: (Int, Int) -> T): List<T> =
  (xMin until xMax step stepSize).flatMap { x ->
    (yMin until yMax step stepSize).map { y ->
      fn(x, y)
    }
  }

@JvmName("gridDouble")
fun <T : Any> grid(xMin: Int, xMax: Int, yMin: Int, yMax: Int, stepSize: Int = 1, fn: (Double, Double) -> T): List<T> =
  (xMin until xMax step stepSize).flatMap { x ->
    (yMin until yMax step stepSize).map { y ->
      fn(x.toDouble(), y.toDouble())
    }
  }

@JvmName("gridVector2")
fun <T : Any> grid(xMin: Int, xMax: Int, yMin: Int, yMax: Int, stepSize: Int = 1, fn: (Vector2) -> T): List<T> =
  (xMin until xMax step stepSize).flatMap { x ->
    (yMin until yMax step stepSize).map { y ->
      fn(Vector2(x.toDouble(), y.toDouble()))
    }
  }

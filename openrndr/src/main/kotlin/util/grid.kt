package util

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

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

@JvmName("gridDouble")
fun <T : Any> grid(xMin: Int, xMax: Int, stepSizeX: Int = 1, yMin: Int, yMax: Int, stepSizeY: Int = 1, fn: (Double, Double) -> T): List<T> =
  (xMin until xMax step stepSizeX).flatMap { x ->
    (yMin until yMax step stepSizeY).map { y ->
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

@JvmName("gridRectVector2")
fun <T : Any> grid(rect: Rectangle, stepSize: Int = 1, fn: (Vector2) -> T): List<T> =
  (rect.x.toInt() until (rect.x + rect.width).toInt() step stepSize).flatMap { x ->
    (rect.y.toInt() until (rect.y + rect.height).toInt() step stepSize).map { y ->
      fn(Vector2(x.toDouble(), y.toDouble()))
    }
  }

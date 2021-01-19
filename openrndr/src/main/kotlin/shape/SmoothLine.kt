package shape

import org.openrndr.math.Vector2

class SmoothLine(val raw: List<Vector2>) {
  val size = raw.size

  fun movingWindow(windowSize: Int = 5): List<Vector2> =
    raw.mapIndexed { index, point ->
      val min = if (index < windowSize) 0 else index - windowSize
      val max = if (index + windowSize > size) size else index + windowSize
      val magnitude = (max.toDouble() - min.toDouble())
      raw.subList(min, max)
      val xMean = raw.subList(min, max).fold(0.0) { acc, p -> acc + p.x } / magnitude
      val yMean = raw.subList(min, max).fold(0.0) { acc, p -> acc + p.y } / magnitude
      Vector2(xMean, yMean)
    }
}

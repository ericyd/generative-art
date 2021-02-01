// Resources
// https://en.wikipedia.org/wiki/Smoothing
// https://en.wikipedia.org/wiki/Moving_average
package shape

import org.openrndr.math.Vector2

class SmoothLine(val raw: List<Vector2>) {
  val size = raw.size

  fun movingAverage(windowSize: Int = 5): List<Vector2> =
    raw.mapIndexed { index, point ->
      // without these premature returns, we end up shrinking the line quite substantially
      // because the ends contract towards the middle
      if (index < windowSize || index + windowSize > size - 1) {
        return@mapIndexed point
      }
      val min = if (index < windowSize) 0 else index - windowSize
      val max = if (index > size - windowSize) size else index + windowSize
      val subList = raw.subList(min, max)
      val magnitude = subList.size
      val xMean = subList.fold(0.0) { acc, p -> acc + p.x } / magnitude
      val yMean = subList.fold(0.0) { acc, p -> acc + p.y } / magnitude
      Vector2(xMean, yMean)
    }
}

package util

import org.openrndr.shape.Segment
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Returns the angular difference between two segments.
 * Accounts for the case where segment orientations are similar in angular direction
 * but very different in atan2 value
 */
fun angularDifference(s1: Segment, s2: Segment): Double {
  val diff = orientation(s1) - orientation(s2)
  // when crossing the π/-π threshold, the difference will be large even though the angular difference is small.
  // We can adjust for this special case by adjusting the diff by 2π
  return when {
    abs(diff) > PI && diff > 0.0 -> diff - 2.0 * PI
    abs(diff) > PI && diff < 0.0 -> diff + 2.0 * PI
    else -> diff
  }
}

/**
 * @return angle in radians in range [-π, π]
 */
fun orientation(s: Segment): Double = atan2(s.end.y - s.start.y, s.end.x - s.start.x)

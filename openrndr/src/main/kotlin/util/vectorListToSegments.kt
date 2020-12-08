package util

import org.openrndr.math.Vector2
import org.openrndr.shape.Segment

/**
 * Returns a list of segments from the vectors,
 * where adjacent vectors create segments
 * vectors (in):     A   B   C   D   E
 * segements (out):  | A | B | C | D |
 *
 * YES, this returns a list with n-1 values
 */
fun vectorListToSegments(list: List<Vector2>): List<Segment> {
  val newList = mutableListOf<Segment>()
  list.forEachIndexed { index, vec ->
    if (index < list.lastIndex) {
      newList.add(Segment(vec, list[index + 1]))
    }
  }
  return newList
}

package color

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.random
import org.openrndr.math.map
import kotlin.random.Random

/**
 * A container for a palette of colors that is unevenly spaced.
 *
 * method:
 * Generate n blocks with random width between 0 and 1.
 * Then scale each block based on the difference between the total set of blocks and the total desired width
 *
 * This does work, but feels VERY inelegant. But, is that a problem?
 * Also, the method to access the colors feels kinda weird. The start/width combo is odd. Consider how to improve
 */
class Palette(val colors: List<ColorRGBa>, val min: Double, val max: Double, val rand: Random = Random.Default) {
  // not sure why this is marked as same signature...
  // constructor(colors: List<ColorHSLa>) : this(colors.map { it.toRGBa() })

  val trueMin = 0.0001

  val randomQueue: List<Double> = (0 until colors.size).foldIndexed(listOf()) { idx, list, n ->
    list + listOf(random(trueMin, 1.0, rand) + list.getOrElse(idx - 1) { 0.0 })
  }

  val queueLen = randomQueue.last()

  val rightEdges = randomQueue.map { map(trueMin, queueLen, min, max, it) }

  val allEdges = listOf(0.0) + rightEdges
  val leftEdges = listOf(0.0) + rightEdges.dropLast(1)

  val widths: List<Double> = leftEdges.foldIndexed(listOf()) { idx, list, edge ->
    val next = allEdges[idx + 1]
    list + listOf(next - edge)
  }

  fun colorAt(x: Double): ColorRGBa {
    val colorIndex = rightEdges.indexOfFirst { x < it }
    return colors[colorIndex]
  }

  fun forEach(f: (color: ColorRGBa, xStart: Double, colorWidth: Double) -> Unit) {
    (0 until colors.size).forEach { f(colors[it], leftEdges[it], widths[it]) }
  }
}

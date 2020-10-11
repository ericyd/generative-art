/**
 * Goal:
 * Henon Phase attractor
 * http://www.complexification.net/gallery/machines/henonPhaseDeep/
 */
package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 750
    height = 750
  }

  program {
    // In future, consider implementing own screenshot mechanism that could run after each draw loop
    // screenshot: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-extensions/src/main/kotlin/org/openrndr/extensions/Screenshots.kt
    // timestamp: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-core/src/main/kotlin/org/openrndr/utils/NamedTimestamp.kt
    extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 2.0
    }
    backgroundColor = ColorRGBa.WHITE

    // This is really the number of "starting points",
    // but they expose themself as lines over time
    val nLines = 5000

    // Choose a seed at random, or override with a well-liked value
    var seed = random(1.0, Long.MAX_VALUE.toDouble()).toLong()
    // nice seeds
    // seed = 6945021760961207296
    // seed = 1929376643247894528
    seed = 3512302340293065728

    // Instantiate Random class with our seed
    val rand = Random(seed)

    // define the bounds of the window (may need to get more complex for non-square windows)
    // This seems rather microscopic but it's really the most interesting part
    var bounds = 01.0

    // a is our "Phase constant"
    val a = random(0.0, PI * 2.0, rand)
    // val a = random(-1.0, 1.0, rand)
    val b = random(0.0, PI * 2.0, rand)
    val c = random(0.0, PI * 2.0, rand)
    val d = random(0.0, PI * 2.0, rand)
    // val a = 1.7831

    // For debugging/recreation
    println(
      """
      seed: $seed
      a: $a
      b: $b
      """.trimIndent()
    )

    // Henon Phase
    // Calculate next point iteratively
    fun nextPoint(point: Vector2): Vector2 =
      // custom - really cool with seed = 1929376643247894528
      // Vector2(
      //   point.x * cos(c / d) - (point.y - (point.x * point.x)) * sin(a  / b),
      //   point.x * sin(c / d) + (point.y - (point.x * point.x)) * cos(a /  b)
      // )

      // custom 2
      Vector2(
        point.x * sin(c / d) - (point.y - (point.x * point.x)) * cos(a / b),
        point.x * cos(c / d) + (point.y - (point.x * point.x)) * sin(a / b)
      )

    // original
    // Vector2(
    //   point.x * cos(a) - (point.y - (point.x * point.x)) * sin(a),
    //   point.x * sin(a) + (point.y - (point.x * point.x)) * cos(a)
    // )

    // Randomly generate initial points. The placement is quite unimportant
    var points = List(nLines) {
      Vector2(random(-bounds, bounds, rand), random(-bounds, bounds, rand))
      // Vector2(random(-1.0, 1.0, rand), random(-1.0, 1.0, rand))
    }

    // Start lines at the "next point" to avoid artifacts from the random placement of the first points
    var lines: List<MutableList<Vector2>> = points.map {
      mutableListOf(nextPoint(it))
    }
    // bounds = (lines.flatten().maxBy { it.x }!!.x + lines.flatten().maxBy { it.y }!!.y) / 2.0

    extend {
      // On each loop, add a new point to each line
      lines.forEach { l ->
        l.add(nextPoint(l.last()))
      }

      // Flatten the lines and map to our window boundaries
      points = lines.flatten().map {
        Vector2(
          map(-bounds / 9.0, bounds / 9.0, 0.0, width.toDouble(), it.x),
          map(-bounds / 9.0, bounds / 9.0, 0.0, height.toDouble(), it.y)
        )
      }

      // Draw the mapped points
      // could also use circles here
      drawer.fill = ColorRGBa.BLACK.opacify(0.15)
      // drawer.fill = ColorRGBa(0.70, 0.60, 0.20, 0.05)
      points.chunked(500) { drawer.points(it) }
    }
  }
}

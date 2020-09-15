/**
 * Goal:
 * De Jong attractor
 * http://www.complexification.net/gallery/machines/peterdejong/
 *
 * 2D attractor instead of 3D, no need for weird projections
 */
package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
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

    val nLines = 5000

    var seed = random(1.0, Long.MAX_VALUE.toDouble()).toLong()
    // nice seeds
    // seed = 6616143053206225920
    // seed = 3721948038489384960
    // seed = 9006867297533176832
    // seed = 1380776900078604288
    // seed = 8258304699529367552
    // seed = 6752889625800710144
    val rand = Random(seed)
    val bounds = 2.0

    // really nice, but lifted straight from http://www.complexification.net/gallery/machines/peterdejong/
    // val a = 1.4191403
    // val b = -2.2841523
    // val c = 2.4275403
    // val d = -2.17716

    // Allowing these to be a bit larger is also interesting, but I like the smaller range generally
    val paramRange = 2.5
    val a = random(-paramRange, paramRange, rand)
    val b = random(-paramRange, paramRange, rand)
    val c = random(-paramRange, paramRange, rand)
    val d = random(-paramRange, paramRange, rand)

    println("""
      seed: $seed
      a: $a
      b: $b
      c: $c
      d: $d
    """.trimIndent())

    // De Jong
    fun nextPoint(point: Vector2): Vector2 =
      Vector2(
        sin(a * point.y) - cos(b * point.x),
        sin(c * point.x) - cos(d * point.y)
      )

    var points = List(nLines) {
      Vector2(random(-bounds, bounds, rand), random(-bounds, bounds, rand))
    }

    // Start lines at the "next point" to avoid artifacts from the random placement of the first points
    var lines: List<MutableList<Vector2>> = points.map {
      mutableListOf(nextPoint(it))
    }

    extend {
      lines.forEach { l ->
        l.add(nextPoint(l.last()))
      }
      points = lines.flatten().map {
        // DeJong always returns between -2 and 2 since it is the difference of two basic trig funcs
        Vector2(
          map(-bounds, bounds, 0.0, width.toDouble(), it.x),
          map(-bounds, bounds, 0.0, height.toDouble(), it.y)
        )
      }

      drawer.fill = ColorRGBa.BLACK.opacify(0.35)
      // drawer.fill = ColorRGBa(0.70, 0.60, 0.20, 0.05)
      points.chunked(500) { drawer.points(it) }
    }
  }
}

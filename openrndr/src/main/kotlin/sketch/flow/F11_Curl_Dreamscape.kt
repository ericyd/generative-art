/**
 * More custom noise explorations
 */
package sketch.flow

import noise.perlinCurl
import org.openrndr.application
import org.openrndr.color.ColorHSLa
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import java.lang.Math.pow
import kotlin.math.hypot
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 1100
  }

  program {
    // In future, consider implementing own screenshot mechanism that could run after each draw loop
    // screenshot: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-extensions/src/main/kotlin/org/openrndr/extensions/Screenshots.kt
    // timestamp: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-core/src/main/kotlin/org/openrndr/utils/NamedTimestamp.kt
    extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 2.0
    }

    var seed = random(1.0, Long.MAX_VALUE.toDouble()).toLong() // know your seed 😛
    seed = 5417199235470797824
    val rand = Random(seed)
    println("seed = $seed")

    backgroundColor = ColorRGBa.WHITE

    val stepSize = 9
    val jitter = stepSize * 0.7
    val lineLength = 500
    val opacity = 0.25
    val center = Vector2(width / 2.0, height / 2.0)
    val halfDiagonal = hypot(width / 2.0, height / 2.0)
    val noiseScaleBounds = Pair(1000.0, 70.0)
    val noiseScaleEffect = Pair(0.0625, 1.0)
    val bounds = width / 2

    val contours: List<ShapeContour> = ((0 - bounds) until (width + bounds) step stepSize).flatMap { x ->
      ((0 - bounds) until (height + bounds) step stepSize).map { y ->
        contour {
          moveTo(
            x + random(-jitter, jitter, rand),
            y + random(-jitter, jitter, rand)
          )

          List(lineLength) {
            // curl noise - a few variations shown below
            val noiseScaleRatio = map(
              0.0, pow(halfDiagonal, 2.0),
              noiseScaleEffect.first, noiseScaleEffect.second,
              cursor.squaredDistanceTo(center)
            )
            // layer two curl noises together, creating a sort of "major" and "minor" flow pattern
            val res = perlinCurl(seed.toInt(), cursor / noiseScaleBounds.first) +
              perlinCurl(seed.toInt(), cursor / noiseScaleBounds.second) * noiseScaleRatio

            lineTo(cursor + res.normalized)
          }
        }
      }
    }

    /**
     * val should be in [-1.0, 1.0] range
     */
    fun mapColor(value: Double): ColorRGBa {
      // create three "bands" of color
      return if (value < 1.0 / -3.0) {
        val hue = map(-1.0, 1.0 / -3.0, 5.0, 50.0, value)
        ColorHSLa(hue, 0.75, 0.5, opacity).toRGBa()
      } else if (value < 1.0 / 3.0) {
        val hue = map(1.0 / -3.0, 1.0 / 3.0, 200.0, 270.0, value)
        ColorHSLa(hue, 0.7, 0.8, opacity).toRGBa()
      } else {
        val hue = map(1.0 / 3.0, 1.0, 300.0, 340.0, value)
        ColorHSLa(hue, 0.4, 0.7, opacity).toRGBa()
      }
    }

    println("aww yeah, about to render...")
    extend {
      drawer.fill = null
      drawer.stroke = null // overwritten below
      drawer.strokeWeight = 1.0
      drawer.lineCap = LineCap.ROUND

      // simple B&W
      // drawer.stroke = ColorRGBa.BLACK.opacify(0.2)
      // contours.chunked(500).forEach { drawer.contours(it) }

      // "dreamscape"
      contours.forEach {
        drawer.strokeWeight = map(
          0.0, pow(halfDiagonal, 2.0),
          1.0, 2.0,
          it.segments.first().start.squaredDistanceTo(center)
        )
        drawer.stroke = mapColor(simplex(seed.toInt(), it.segments.first().start / noiseScaleBounds.first))
        drawer.contour(it)
      }
    }
  }
}

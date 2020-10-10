/**
 * You cannot appreciate Perlin noise, or Simplex noise,
 * or probably any noise, until you try to write your own noise algorithm.
 *
 * Generative art for me is such an interesting mixture of
 * actual art - a love of aesthetic beauty -
 * and a love of programming and problem solving.
 *
 * My strength is in programming, but my background is not in any type of visual coding,
 * so algorithms like gradient noise are quite challenging for me.
 */
package sketch.flow

import noise.mapToRadians
import noise.memoize
import noise.yanceyNoiseV1
import org.openrndr.application
import org.openrndr.color.ColorHSLa
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.math.cos
import kotlin.math.sin
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
    // seed = 3375967866848408576
    // seed = 2673564062524443648
    // seed = 929405885139496960
    // seed = 3566189830629765120
    // seed = 3369173695223600128
    // seed = 6191782011463045120 with normalizerIshThingy = true
    val rand = Random(seed)
    println("seed = $seed")

    backgroundColor = ColorRGBa.WHITE

    val stepSize = 10
    val jitter = (width / stepSize) * 0.1
    val lineLength = 400
    val opacity = 0.25

    val noiseScale = listOf(width, height).max()!!.toDouble()
    val bounds = width / 2

    // Memoizing our random function ensures that we get smooth gradients.
    // The idea is, for each unique point in our "grid", we should always return the same value.
    // There are other ways of achieving this, but memoizing a random function is a pretty easy way.
    val memo = memoize<Vector2, Vector2> { v -> Vector2(random(-1.0, 1.0, rand), random(-1.0, 1.0, rand)).normalized }

    val results = mutableListOf<Double>()

    val contours: List<ShapeContour> = ((0 - bounds) until (width + bounds) step stepSize).flatMap { x ->
      ((0 - bounds) until (height + bounds) step stepSize).map { y ->
        contour {
          moveTo(
            x + random(-jitter, jitter, rand),
            y + random(-jitter, jitter, rand)
          )

          List(lineLength) {
            // 2D value noise
            // Note: must use memoizedValueRandom, e.g. val memo = memoizedValueRandom(rand, Vector2.ZERO)
            // val angle = mapToRadians(0.0, 1.0, valueNoise2D(cursor / noiseScale, memo))

            // 2D ... gradient noise? Maybe
            val res = yanceyNoiseV1(cursor / noiseScale, memo, true)
            results.add(res)
            val angle = mapToRadians(-1.0, 1.0, res)

            lineTo(cursor + Vector2(cos(angle), sin(angle)))
          }
        }
      }
    }

    // Optionally... draw bubbles!
    // Note to self: love the idea, hate the implementation
    // val bubbles: List<List<Vector2>> = contours.map {
    //   it.segments
    //     .map { it.start }
    //     .filterIndexed { index, list -> index % 35 == 0 }
    // }

    // helps me know how to map it :shrug:
    println("max: ${results.max()}")
    println("min: ${results.min()}")

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
        drawer.stroke = mapColor(yanceyNoiseV1(it.bounds.center / noiseScale, memo))
        drawer.contour(it)
      }

      // bubbles.forEach {
      //   val bubbleNoise = yanceyNoiseV1(it.first() / noiseScale, memo, true)
      //   drawer.fill = mapColor(bubbleNoise)
      //   for (bubble in it) {
      //     drawer.circle(bubble, map(-1.0, 1.0, 3.0, 22.0, yanceyNoiseV1( bubble / noiseScale, memo, true)))
      //   }
      //   // drawer.circles(it, map(-1.0, 1.0, 5.0, 12.0, bubbleNoise))
      // }
    }
  }
}

/**
 * More custom noise explorations
 */
package sketch.flow

import noise.mapToRadians
import noise.memoize
import noise.yanceyNoiseGenerator
import org.openrndr.application
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
    val rand = Random(seed)
    println("seed = $seed")

    backgroundColor = ColorRGBa.WHITE

    val stepSize = 10
    val jitter = (width / stepSize) * 0.1
    val lineLength = 400
    val opacity = 0.25
    val center = Vector2(width / 2.0, height / 2.0)
    val noiseScale = 200.0
    val bounds = width / 2

    // Memoizing our random function ensures that we get smooth gradients.
    // The idea is, for each unique point in our "grid", we should always return the same value.
    // There are other ways of achieving this, but memoizing a random function is a pretty easy way.
    // val memo = memoize<Vector2, Double>(::weirdTrigShit)
    val memo = memoize<Vector2, Double> { v -> random(0.0, 1.0, rand) }
    // val memo = memoize<Vector2, Vector2> { v ->
    //   Vector2(random(-1.0, 1.0, rand), random(-1.0, 1.0, rand)).normalized
    // }
    val yanceyNoise = yanceyNoiseGenerator(memo)

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
            // val res = yanceyNoiseV2((cursor - center) / noiseScale, memo)
            val res = yanceyNoise(cursor / noiseScale)
            // val res = yanceyNoiseV3(cursor / noiseScale, memo, true)
            results.add(res)
            val angle = mapToRadians(-1.0, 1.0, res)

            lineTo(cursor + Vector2(cos(angle), sin(angle)))
          }
        }
      }
    }

    // helps me know how to map it :shrug:
    println("max: ${results.max()}")
    println("min: ${results.min()}")

    extend {
      drawer.fill = null
      drawer.stroke = null // overwritten below
      drawer.strokeWeight = 1.0
      drawer.lineCap = LineCap.ROUND

      // simple B&W
      drawer.stroke = ColorRGBa.BLACK.opacify(0.2)
      contours.chunked(500).forEach { drawer.contours(it) }
    }
  }
}

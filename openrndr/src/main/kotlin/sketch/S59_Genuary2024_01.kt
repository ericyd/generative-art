package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.color.palettes.ColorSequence
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.math.*
import org.openrndr.shape.*
import util.rotatePoint
import util.timestamp
import kotlin.math.*
import kotlin.random.Random

fun main() = application {
  configure {
    width = 800
    height = 800
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    seed = 1485667459 // good seed
    println("seed = $seed")

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      contentScale = 3.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
    }

    val bg = ColorRGBa.BLACK
    backgroundColor = bg
    val colors = listOf(
      "23214A",
      "94EF86",
      "F3ED76",
      "E15F33",
      "BE59E7",
      "E56DB1",
      "FF9B4E",
      "A2F2EC",
      "9C99E5",
      "005D7E",
    )

    /**
     * Core algorithm
     * 1. generate `n` base points around center, in sunflower pattern
     * 2. for each point, determine a "rotationDrift" amount based on some 1-D noise function
     * 3. for `angle` in 0 until `rotationDrift`
     *    a. add `m` points, distributed by Vector2.gaussian, with `m` and `deviation` increasing with `angle`
     *    b. color of point is... something, tbd.
     */
    extend {
      val rng = Random(seed)
      drawer.stroke = null

      val center = Vector2(width * 0.5, height * 0.5)
      val spectrum = ColorSequence(colors.shuffled(rng).mapIndexed { index, hex ->
        Pair(map(0.0, colors.size - 1.0, 0.0, 1.0, index.toDouble()), ColorRGBa.fromHex(hex))
      })
      val PHI = 0.5 + 0.5.pow(0.5) * 0.5
      val n = 50
      val basePoints = List(n) {
        val angle = it * (1.0 + PHI)
        val radius = map(0.0, n.toDouble(), 0.0, center.length, it.toDouble())
        center + Vector2(cos(angle), sin(angle)) * radius
      }

      for (point in basePoints) {
        val spectrumStart = random(0.0, 0.6, rng)
        val spectrumEnd = random(spectrumStart, spectrumStart + 0.4, rng)
        val rotationDrift = map(-1.0, 1.0, 0.0, PHI * PI, simplex(seed, point.distanceTo(center) / center.length * 1.3))
        var angle = 0.0
        while (angle < rotationDrift) {
          angle += 0.03
          val nPoints = map(0.0, rotationDrift, 1.0, 100.0, angle).toInt()
          val deviation = Vector2.ONE * map(0.0, rotationDrift, 0.5, 20.0, angle)
          List(nPoints) {
            val pointCenter = Vector2.gaussian(rotatePoint(point, angle, center), deviation, rng)
            val opacity = map(0.0, rotationDrift, random(0.5, 0.9, rng), random(0.2, 0.5, rng), angle)
            drawer.stroke = ColorRGBa.WHITE.opacify(opacity)
            drawer.strokeWeight = 0.075
            drawer.fill = spectrum.index(map(0.0, rotationDrift, spectrumStart, spectrumEnd, angle)).opacify(opacity)
            val circleRadius = map(0.0, rotationDrift, random(0.5, 1.3, rng), random(0.01, 0.22, rng), angle)
            drawer.circle(pointCenter, circleRadius)
          }
        }
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
      }
    }
  }
}

/**
 * More custom noise explorations
 */
package sketch.flow

import extensions.CustomScreenshots
import noise.perlinCurl
import noise.yanceyNoiseGenerator
import org.openrndr.application
import org.openrndr.color.ColorHSLa
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.math.mix
import org.openrndr.shape.contour
import util.timestamp
import java.lang.Math.pow
import kotlin.math.hypot
import kotlin.random.Random

fun main() = application {
  configure {
    width = 800
    height = 1000
  }

  program {
    var seed = random(1.0, Long.MAX_VALUE.toDouble()).toLong() // know your seed 😛
    // seed = 55217631486688256
    // seed = 8896268477267021824
    // seed = 5105143190369064960
    // seed = 6059827915172121600
    // seed = 2072743207044059136
    // seed = 6393143113906295808
    // seed = 554982528967554048
    // seed = 9106824235015506944
    // seed = 4837378247439633408
    // seed = 6065253853972814848
    seed = 843898394290568192
    println("seed = $seed")

    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    val screenshots = extend(CustomScreenshots()) {
      quitAfterScreenshot = false
      scale = 3.0
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      captureEveryFrame = true
    }

    backgroundColor = ColorRGBa.WHITE

    val stepSize = 5
    val jitter = stepSize * 0.7
    val lineLength = 500
    val opacity = 0.05
    val center = Vector2(width / 2.0, height / 2.0)
    val halfDiagonal = hypot(width / 2.0, height / 2.0)
    val noiseScales = listOf(1000.0, 100.0, 35.0)
    val bounds = width / 2

    fun mixNoise(cursor: Vector2, yanceyNoise: (Vector2) -> Double): Vector2 {
      val (major, minor, micro) = noiseScales

      // major ratio varies by a simplex noise map
      val majorRatio = map(
        -1.0, 1.0,
        0.4, 0.90,
        // using "yancey noise" here is totally unnecessary... Perlin or Simplex would be just fine
        yanceyNoise(cursor / mix(major, minor, 0.25))
      )

      // minor ratio varies by distance from center
      val minorRatio = map(
        0.0, pow(halfDiagonal, 2.0),
        0.0625, 0.75,
        cursor.squaredDistanceTo(center)
      )

      // micro ratio varies by a different simplex noise map
      val microRatio = map(
        -1.0, 1.0,
        0.00125, 0.25,
        pow(simplex(seed.toInt(), cursor / mix(major, minor, 0.45)), 2.0)
      )
      // layer two curl noises together, creating a sort of "major" and "minor" flow pattern
      val res = perlinCurl(seed.toInt(), cursor / major) * majorRatio +
        perlinCurl(seed.toInt(), cursor / minor) * minorRatio +
        perlinCurl(seed.toInt(), cursor / micro) * microRatio

      return res.normalized
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

    extend {
      val rand = Random(seed)
      val yanceyNoise = yanceyNoiseGenerator { v -> random(-1.0, 1.0, rand) }

      drawer.fill = null
      drawer.stroke = null // overwritten below
      drawer.strokeWeight = 1.0
      drawer.lineCap = LineCap.ROUND

      // simple B&W
      drawer.stroke = ColorRGBa.BLACK.opacify(opacity)

      for (x in (0 - bounds) until (width + bounds) step stepSize) {
        for (y in (0 - bounds) until (height + bounds) step stepSize) {
          val c = contour {
            moveTo(
              x + random(-jitter, jitter, rand),
              y + random(-jitter, jitter, rand)
            )

            List(lineLength) {
              lineTo(cursor + mixNoise(cursor, yanceyNoise))
            }
          }
          drawer.contour(c)
        }
      }

      // // "dreamscape"
      // contours.forEach {
      //   drawer.strokeWeight = map(
      //     0.0, pow(halfDiagonal, 2.0),
      //     1.0, 2.0,
      //     it.segments.first().start.squaredDistanceTo(center)
      //   )
      //   drawer.stroke = mapColor(simplex(seed.toInt(), it.segments.first().start / noiseScaleBounds.first))
      //   drawer.contour(it)
      // }
    }
  }
}

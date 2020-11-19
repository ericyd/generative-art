/**
 * Algorithm in a nutshell:
 * 1. TODO: Write algorithm in a nutshell
 */
package sketch

import extensions.CustomScreenshots
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.shadestyles.linearGradient
import kotlin.random.Random

fun main() = application {
  configure {
    width = 750
    height = 950
  }

  program {
    val progRef = this
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    // seed = 1088846048
    seed = 690157030
    val screenshots = extend(CustomScreenshots()) {
      quitAfterScreenshot = false
      scale = 3.0
      folder = "screenshots/${progRef.name.ifBlank { progRef.window.title.ifBlank { "my-amazing-drawing" } }}/"
      captureEveryFrame = true
      // name = "screenshots/S9_trial/S9_DisintegratingColumn-${progRef.namedTimestamp("png", "screenshots")}-seed-$seed.png"
    }

    println(
      """
        seed = $seed
      """.trimIndent()
    )

    val bg = ColorRGBa.WHITE
    backgroundColor = bg
    val bgGradient = linearGradient(ColorRGBa(0.99, 0.99, 0.99, 1.0), ColorRGBa(0.02, 0.02, 0.02, 1.0), exponent = 2.5)

    val yMin = (height * 0.1).toInt()
    val yMax = (height * 0.9).toInt()
    val yRange = (yMax - yMin).toDouble()
    val yStep = 4

    val xMin = (width * 0.25).toInt()
    val xMax = (width * 0.75).toInt()
    val xStep = 1

    extend {
      // get that rng
      val rng = Random(seed.toLong())

      drawer.isolated {
        this.shadeStyle = bgGradient
        this.stroke = null
        this.rectangle(0.0, 0.0, width.toDouble(), height.toDouble())
      }

      for (yInt in yMin until yMax step yStep) {
        val y = yInt.toDouble()
        // each "row" has distinct noise function params
        val offset = random(0.0, 200.0, rng)
        val noiseScale = random(120.0, 275.0, rng)

        for (xInt in xMin until xMax step xStep) {
          val x = xInt.toDouble()

          // dots should become more sparse as we go up in y scale
          val probabilityOfNone = y / yRange * 1.1 - 0.2
          if (random(0.0, 1.0, rng) > probabilityOfNone) {

            // fetch a shade from a noise function
            val shade = simplex(seed, x / noiseScale + offset, y / noiseScale + offset, y / yRange)
            val color = ColorRGBa(shade, shade, shade, 1.0)
            drawer.stroke = color
            drawer.fill = null
            drawer.circle(x, y, 2.5)
          }
        }
      }

      // set seed for next iteration
      seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
      // screenshots.name = "screenshots/S9_trial/S9_DisintegratingColumn-${progRef.namedTimestamp("png", "screenshots")}-seed-$seed.png"
    }
  }
}

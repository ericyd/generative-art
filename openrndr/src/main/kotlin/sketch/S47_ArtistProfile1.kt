package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.color.palettes.ColorSequence
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.*
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.translate
import org.openrndr.shape.Rectangle
import util.timestamp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.cos
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 400
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      contentScale = 6.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
    }

    val bg = ColorRGBa.BLACK
    backgroundColor = bg
    var colors = listOf(
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

    extend {
      val rng = Random(seed)
      val spectrum = ColorSequence(colors.mapIndexed { index, hex ->
        Pair(map(0.0, colors.size - 1.0, 0.0, 1.0, index.toDouble()), ColorRGBa.fromHex(hex))
      })
      val strokeWeight = width * 0.003
      
      val tileHeight = 10.0
      val baseTileWidth = 55.0
      
      val majorOffsetX = random(width * 0.1, width * 0.9, rng)
      val minorWaveAmplitude = random(0.1, 0.25, rng)
      val minorScaleFactor = random(1.1, 5.0, rng)
      val lineHeight = height * 0.75
      fun sine(rawX: Double): Double {
//        hm, this ends breaking things if this is too large, because the angles get too extreme
//        for the contouring algorithm to work correctly.
//        But I want more waves... Might need to re-think the contouring algorithm
        val x = rawX / width.toDouble() * PI * 1.2
        val majorWave = sin(x + majorOffsetX)
        val minorWave = minorWaveAmplitude * sin(x * minorScaleFactor + majorOffsetX)
        val scaleFactor = (2.0 + minorWaveAmplitude * 2.0)
        return ((majorWave + minorWave) / scaleFactor + 0.5) * lineHeight
      }

      val stepSize = 8
      drawer.strokeWeight = stepSize / 4.0
      drawer.lineCap = LineCap.ROUND

      for (y in -height until height * 2 step tileHeight.toInt()) {
        var x = -baseTileWidth
        while (x < width.toDouble()) {
          println("(x, y): ($x, $y)")
          // calculate the derivative at this point
          // d/dx sin(x) == cos(x), but our sine is more complex so need to approximate it
          val epsilon = 1.0
          val ddx = sine(x + epsilon * 0.5) - sine(x - epsilon * 0.5)
          val angle = map(-0.9999, 0.99999, -90.0, 90.0, ddx)
          val radians = map(-90.0, 90.0, -PI / 2.0, PI / 2.0, angle) // is there a utility for this?
          val tileWidth = map(0.0, 90.0, baseTileWidth * 0.2, baseTileWidth * 0.999, abs(angle))
//          val effectiveHeight = sin(radians) * tileWidth
          val effectiveWidth = cos(radians) * tileWidth
          x += effectiveWidth

          val rect = Rectangle(0.0, 0.0, tileWidth, tileHeight)
          val spectrumShift = (y.toDouble() / height) * width * 0.65
          val fill = spectrum.index(map(0.0, width * 1.65, 0.0, 1.0, x + spectrumShift))
          drawer.fill = fill
          drawer.stroke = bg.opacify(0.5)
          val effectiveY = sine(x) + y.toDouble()
          val probabilityCutoff = clamp(
            map(height * 0.20, height * 0.90, 1.0, 0.0, effectiveY),
            0.0,
            1.0
          )
          if (random(0.0, 1.0, rng) < probabilityCutoff) {
            drawer.contour(
              rect.toRounded(tileHeight).contour
                .transform(Matrix44.rotateZ(angle))
                .transform(Matrix44.translate(x, effectiveY, 0.0))
            )
          }
        }
      }


      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
        // shuffle colors after first iteration, to preserve "favorite" spectrum on first iteration
        colors = colors.shuffled(rng)
      }
    }
  }
}

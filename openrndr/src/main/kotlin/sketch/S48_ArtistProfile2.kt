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
import kotlin.math.*
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

    extend {
      val rng = Random(seed)
      
      val tileHeight = 6.0
      val baseTileWidth = 39.0

      fun createSine(rng: Random): (x: Double) -> Double {
        val majorOffsetX = random(width * 0.1, width * 0.9, rng)
        val minorWaveAmplitude = random(0.1, 0.25, rng)
        val minorScaleFactor = random(1.1, 5.0, rng)
        val lineHeight = width * 0.33 // height * 0.75
        return { rawX: Double ->
  //        hm, this ends breaking things if this is too large, because the angles get too extreme
  //        for the contouring algorithm to work correctly.
  //        But I want more waves... Might need to re-think the contouring algorithm
          val x = rawX / width.toDouble() * PI * 1.2
          val majorWave = sin(x + majorOffsetX)
          val minorWave = minorWaveAmplitude * sin(x * minorScaleFactor + majorOffsetX)
          val scaleFactor = (2.0 + minorWaveAmplitude * 2.0)
          ((majorWave + minorWave) / scaleFactor + 0.5) * lineHeight
        }
      }

      drawer.strokeWeight = 1.0
      drawer.lineCap = LineCap.ROUND

//      val boundingRects = listOf(
//        random(width * -0.4, width * -0.2, rng),
//        random(width * -0.1, width * 0.3, rng),
//        random(width * 0.2, width * 0.6, rng),
//      ).shuffled(rng).map { x ->
//        Rectangle(
//          x,
//          y = random(height * -1.0, height * -0.25, rng),
//          width = random(width * 0.65, width * 0.85, rng),
//          height = random(height * 1.25, height * 2.25, rng)
//        )
//      }
//      for (bounds in boundingRects) {

//      interesting... but imperfect
      for (i in 0 until 2) {
//         start with a "background" layer
        val bounds = if (i == 0) {
          Rectangle(
            x = width * -0.5,
            y = height * -1.0,
            width = width * 2.0,
            height = height * 2.0,
          )
        } else {
          Rectangle(
            x = random(width * -0.25, width * 0.35, rng),
            y = random(height * -0.55, height * 0.35, rng),
            width = random(width * 0.65, width * 1.1, rng),
            height = random(height * 0.65, height * 1.1, rng),
          )
        }
        val sine = createSine(rng)
        val shade = map(0.0, 1.0, 0.75, 1.5, i.toDouble()) // random(0.75, 1.25, rng)
        val spectrum = ColorSequence(colors.shuffled(rng).mapIndexed { index, hex ->
          Pair(map(0.0, colors.size - 1.0, 0.0, 1.0, index.toDouble()), ColorRGBa.fromHex(hex).shade(shade))
        })

        for (y in bounds.y.toInt() until (bounds.y + bounds.height).toInt() step tileHeight.toInt()) {
          var x = bounds.x
          while (x < bounds.x + bounds.width) {
            // calculate the derivative at this point
            // d/dx sin(x) == cos(x), but our sine is more complex so need to approximate it
            val epsilon = 1.0
            val ddx = sine(x + epsilon * 0.5) - sine(x - epsilon * 0.5)
            val angle = map(-0.9999, 0.99999, -90.0, 90.0, ddx)
            val radians = map(-90.0, 90.0, -PI / 2.0, PI / 2.0, angle) // is there a utility for this?
            val tileWidth = map(0.0, 90.0, baseTileWidth * 0.15, baseTileWidth * 0.999, abs(angle))
            val effectiveWidth = cos(radians) * tileWidth
            val point = Vector2(x, sine(x) + y.toDouble())
            x += max(effectiveWidth, 1.0)

            val probabilityCutoff = if (bounds.contains(point)) {
              clamp(
                map(
                  0.0,
                  hypot(bounds.width, bounds.height) * 0.15,
                  0.0,
                  1.0,
                  bounds.contour.nearest(point).position.distanceTo(point)
                ),
                0.0,
                1.0
              )
            } else {
              0.0
            }
            if (random(0.0, 1.0, rng) < probabilityCutoff) {
              val rect = Rectangle(tileWidth * -0.5, tileHeight * -0.5, tileWidth, tileHeight)
              val spectrumShift = (y.toDouble() / height) * width * 0.65
              val fill = spectrum.index(map(0.0, width * 1.65, 0.0, 1.0, x + spectrumShift))
              drawer.fill = fill
              drawer.stroke = bg.opacify(0.5)
              drawer.contour(
                rect.toRounded(tileHeight).contour
                  .transform(Matrix44.rotateZ(angle))
                  .transform(Matrix44.translate(x, point.y, 0.0))
              )
            }
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

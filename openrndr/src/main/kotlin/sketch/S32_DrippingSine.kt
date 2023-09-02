package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.random
import org.openrndr.extra.color.palettes.ColorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Rectangle
import org.openrndr.shape.contour
import util.timestamp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 600
    height = 800
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 5.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }

    val bleed = 0.4
    val bounds = Rectangle(width * -bleed, height * -bleed, width * (1.0 + bleed), height * (1.0 + bleed))

    val spectrum = ColorSequence(
      listOf(
        0.1 to ColorRGBa.fromHex("0B0C0F"),
        0.3 to ColorRGBa.fromHex("0D0E0F"),
        0.5 to ColorRGBa.fromHex("080908"),
        0.6 to
          ColorRGBa.fromHex("A8D2CA"),
        0.7 to
          ColorRGBa.fromHex("E7E090"),
        0.9 to
          ColorRGBa.fromHex("CB7979"),
        1.0 to
          ColorRGBa.fromHex("A392C1"),
      )
    )

    fun color(y: Double, rng: Random): ColorRGBa {
      val colorMin = map(bounds.y, bounds.y + bounds.height, 1.0, -0.3, y)
      val colorMax = colorMin + 0.2
      var colorIndex = random(colorMin, colorMax, rng)
      val deviation = if (colorIndex < 0.5) abs(colorIndex) else 1.0 - colorIndex
      colorIndex = gaussian(colorIndex, deviation, rng)
      val base = spectrum.index(colorIndex)
      return base.shade(random(0.5, 1.1, rng))
    }

    backgroundColor = ColorRGBa.fromHex("FCFCFD")

    extend {
      val rng = Random(seed)

      for (y in (bounds.height + bounds.y).toInt() downTo bounds.y.toInt() step 8) {
        List(width / 3) {
          val x = random(bounds.x, bounds.x + bounds.width, rng)
          val y = y + random(-0.5, 0.5, rng)
          val length = random(100.0, 500.0, rng).toInt()

          val amplitude = random(2.5, 30.0, rng)
          val freq = random(amplitude, amplitude * 2.0, rng)
          val offset = random(-PI, PI, rng)

          val fn = { y: Double ->
            sin((y + offset - bounds.y) / freq) * amplitude + x
          }

          drawer.fill = null
          drawer.stroke = color(y, rng)
          drawer.contour(
            contour {
              moveTo(x, y)
              List(length) {
                lineTo(Vector2(fn(cursor.y), cursor.y + 1.0))
              }
            }
          )
        }
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
      }
    }
  }
}

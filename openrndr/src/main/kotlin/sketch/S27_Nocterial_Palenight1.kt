/**
 * A logo for the fantastic Nocterial Palenight VSCode theme!
 * https://marketplace.visualstudio.com/items?itemName=AlexDauenhauer.nocterial-palenight
 */
package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.contour
import util.timestamp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 1000
  }

  val bg = ColorRGBa.fromHex("292d3e")

  val colors = listOf(
    ColorRGBa.fromHex("39ADB5"),
    ColorRGBa.fromHex("56f747"),
    ColorRGBa.fromHex("6796E6"),
    ColorRGBa.fromHex("82AAFF"),
    ColorRGBa.fromHex("89DDFF"),
    ColorRGBa.fromHex("A6ACCD"),
    ColorRGBa.fromHex("B267E6"),
    ColorRGBa.fromHex("C3E88D"),
    ColorRGBa.fromHex("C792EA"),
    ColorRGBa.fromHex("CD9731"),
    ColorRGBa.fromHex("E53935"),
    ColorRGBa.fromHex("F07178"),
    ColorRGBa.fromHex("F44747"),
    ColorRGBa.fromHex("F78C6C"),
    ColorRGBa.fromHex("F76D47"),
    ColorRGBa.fromHex("FF5370"),
    ColorRGBa.fromHex("FFCB6B"),
  )

  program {
    backgroundColor = bg
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 1.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }

    val center = Vector2(width * 0.5, height * 0.5)
    val maxRadius = hypot(center.x, center.y) * 0.5

    val rng = Random(seed)

    extend {
      drawer.lineCap = LineCap.ROUND
      drawer.strokeWeight = maxRadius * 0.1

      for (ring in 1..6) {
        val radius = map(1.0, 6.0, maxRadius * 0.2, maxRadius, ring.toDouble())

        val originalStart = random(0.0, 2.0 * PI, rng)
        var startAngle = originalStart
        var start = Vector2(cos(startAngle), sin(startAngle)) * radius + center
        var endAngle = startAngle + random(0.0, PI, rng)

        val gap = map(1.0, 6.0, PI * 0.25, PI * 0.05, ring.toDouble())

        while (endAngle - originalStart < 2.0 * PI - gap) {
          val line = contour {
            moveTo(start)
            List(100) {
              val angle = map(0.0, 100.0, startAngle, endAngle, it.toDouble())
              lineTo(Vector2(cos(angle), sin(angle)) * radius + center)
            }
          }
          drawer.stroke = colors[random(0.0, colors.size.toDouble(), rng).toInt()]
          drawer.contour(line)

          startAngle = endAngle + gap
          start = Vector2(cos(startAngle), sin(startAngle)) * radius + center
          endAngle = startAngle + random(0.0, PI, rng)
        }

        // close up any large gaps
        if (originalStart + 2.0 * PI - startAngle > gap * 2.0) {
          endAngle = originalStart + 2.0 * PI - gap
          val line = contour {
            moveTo(start)
            List(100) {
              val angle = map(0.0, 100.0, startAngle, endAngle, it.toDouble())
              lineTo(Vector2(cos(angle), sin(angle)) * radius + center)
            }
          }
          drawer.stroke = colors[random(0.0, colors.size.toDouble(), rng).toInt()]
          drawer.contour(line)
        }
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

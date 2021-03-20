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
import org.openrndr.shape.LineSegment
import util.timestamp
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
      scale = 3.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }

    val center = Vector2(width * 0.5, height * 0.5)
    val factor = 0.7
    val size = width * factor

    val rng = Random(seed)

    extend {
      drawer.lineCap = LineCap.ROUND
      val rows = 10
      drawer.strokeWeight = (size / rows.toDouble()) * 0.7

      for (row in 0 until rows) {
        val y = center.y - (height * factor * 0.5) + (height * factor / (rows.toDouble() - 1.0)) * row
        var xStart = center.x - width * factor * 0.5
        var xEnd = xStart + random(0.0, size * 0.5, rng)
        val xAbsoluteEnd = width * factor * 0.5 + center.x
        val gap = size * 0.15

        while (xEnd + gap < xAbsoluteEnd) {
          val start = Vector2(xStart, y)
          val end = Vector2(xEnd, y)
          val line = LineSegment(start, end)

          drawer.stroke = colors[random(0.0, colors.size.toDouble(), rng).toInt()]
          drawer.lineSegment(line)

          xStart = xEnd + gap
          xEnd = xStart + random(0.0, size * 0.5, rng)
        }

        // draw in last line
        val start = Vector2(xStart, y)
        val end = Vector2(xAbsoluteEnd, y)
        val line = LineSegment(start, end)

        drawer.stroke = colors[random(0.0, colors.size.toDouble(), rng).toInt()]
        drawer.lineSegment(line)
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

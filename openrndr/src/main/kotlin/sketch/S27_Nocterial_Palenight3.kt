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
import org.openrndr.shape.LineSegment
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

    val rng = Random(seed)
    val stripes = 15
    val hyp = hypot(width.toDouble(), height.toDouble())
    val factor = hyp / stripes.toDouble()
    val angle = PI * 0.25

    extend {
      drawer.lineCap = LineCap.ROUND
      drawer.strokeWeight = factor * 0.85

      for (stripe in 0 until stripes) {
        var start = Vector2(cos(angle), sin(angle)) * hyp * 0.5
        start = start.perpendicular() * map(0.0, stripes.toDouble() - 1.0, -1.0, 1.0, stripe.toDouble())
        // fill the screen
        val absoluteEnd = Vector2(cos(angle), sin(angle)) * hyp * 2.0
        // or, leave gaps at the end
        // val absoluteEnd = Vector2(cos(angle), sin(angle)) * hyp * random(0.85, 1.25, rng)
        var end = start + Vector2(cos(angle), sin(angle)) * random(0.0, hyp * 0.5)
        val gap = hyp * 0.07

        while (end.x + gap < absoluteEnd.x && end.y + gap < absoluteEnd.y) {
          val line = LineSegment(start, end)
          drawer.stroke = colors[random(0.0, colors.size.toDouble(), rng).toInt()]
          drawer.lineSegment(line)

          start = end + Vector2(cos(angle), sin(angle)) * gap
          end = start + Vector2(cos(angle), sin(angle)) * random(0.0, hyp * 0.5)
        }
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

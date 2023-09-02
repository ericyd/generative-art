package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.shape.contour
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.random
import org.openrndr.extra.color.palettes.ColorSequence
import org.openrndr.math.*
import org.openrndr.math.transforms.scale
import util.timestamp
import kotlin.math.PI
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
      scale = 6.0
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
      val tileWidth = 55.0
      
      val majorOffsetX = random(width * 0.1, width * 0.9, rng)
      val minorWaveAmplitude = random(0.1, 0.25, rng)
      val minorScaleFactor = random(1.1, 5.0, rng)
      val lineHeight = height * 0.75
      fun sine(x: Double): Double {
        val majorWave = sin(x + majorOffsetX)
        val minorWave = minorWaveAmplitude * sin(x * minorScaleFactor + majorOffsetX)
        val scaleFactor = (2.0 + minorWaveAmplitude * 2.0)
        return ((majorWave + minorWave) / scaleFactor + 0.5) * lineHeight
      }

      val stepSize = 8
      drawer.strokeWeight = stepSize / 4.0
      drawer.lineCap = LineCap.ROUND
      val x = { i: Int -> map(0.0, width.toDouble(), 0.0, PI, i.toDouble()) }

      for (i in 0..width step tileWidth.toInt()) {
        drawer.stroke = spectrum.index(random(0.0, 1.0, rng))
//        drawer.lineSegment(Vector2(i.toDouble(), 0.0), Vector2(i.toDouble(), sine(x(i))))
        // calculate the derivative at this point
        // d/dx sin(x) == cos(x), but our sine is more complex so need to approximate it
        val ddx = sin(x(i)) - sin(x(i-1))
        
//        val rect = Rectangle()
//        
//        val point0 = Vector2(cos(angle), sin(angle)) * outerRadius + center
//        val probabilityCutoff = clamp(map(height * 0.55, height * 1.25, 1.0, 0.0, point0.x+point0.y), 0.0, 1.0)
//        if (true /*TODO: random(0.0, 1.02, rng) > probabilityCutoff*/) {
//          // this is pretty ugly/imperative, basically just draw 4 versions of the same contour, but the first 3 are offset/opacified and the final one is not
//          for (i in 0..4) {
//            val offset = if (i == 3) { Vector2.ZERO } else { Vector2.gaussian(Vector2.ZERO, Vector2(6.0), rng) }
//            val fill = spectrum.index(map(-PI, PI, 0.0, 1.0, angle))
//            val c = contour {
//              moveTo(point0)
//
//              val point1 = Vector2(cos(angle + tileAngularWidth), sin(angle + tileAngularWidth)) * outerRadius + center + offset
//              arcTo(
//                crx = outerRadius,
//                cry = outerRadius,
//                angle = Math.toDegrees(tileAngularWidth),
//                largeArcFlag = false,
//                sweepFlag = true,
//                end = point1
//              )
//
//              val point2 = Vector2(cos(angle + tileAngularWidth), sin(angle + tileAngularWidth)) * innerRadius + center + offset
//              lineTo(point2)
//
//              val point3 = Vector2(cos(angle), sin(angle)) * innerRadius + center + offset
//              arcTo(
//                crx = innerRadius,
//                cry = innerRadius,
//                angle = Math.toDegrees(tileAngularWidth),
//                largeArcFlag = false,
//                sweepFlag = false,
//                end = point3
//              )
//
//              close()
//            }
//            drawer.fill = if (i == 3) { fill } else { fill.opacify(random(0.1, 0.4, rng)) }
//            drawer.stroke = bg.opacify(0.8)
//            drawer.strokeWeight = if (i == 3) { strokeWeight } else { 0.0 } 
//            drawer.contour(c)
//          }
//        }
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

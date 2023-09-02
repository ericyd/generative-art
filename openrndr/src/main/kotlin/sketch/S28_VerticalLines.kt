/**
 * Create new works here, then move to parent package when complete
 */
package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.hsl
import org.openrndr.draw.isolated
import org.openrndr.extensions.Screenshots
//import org.openrndr.extra.filterextension.extend
import org.openrndr.extra.fx.dither.ADither
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.math.map
import shape.FractalizedLine
import util.timestamp
import kotlin.math.abs
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 675
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    // seed = 221531080
    seed = 1407025804
    println("seed = $seed")

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 4.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }

//    lost in ORX 4.3.0 update
//    extend(ADither())

    val scale = 3
    backgroundColor = ColorRGBa.WHITE

    extend {
      val rng = Random(seed)
      val noiseScale = random(width * 0.1, width * 0.3, rng)

      val colorVariation = 0.075
      val yVariation = 3.50

      var topLine = FractalizedLine(listOf(Vector2(0.0, height * 1.0), Vector2(width.toDouble(), height * 0.75)), rng).perpendicularSubdivide(1, 0.2)
      while (topLine.points.size < width * scale) {
        topLine = topLine.perpendicularSubdivide(1, 0.2)
      }

      var bottomLine = FractalizedLine(listOf(Vector2(width * -1.0, 0.0), Vector2(width * 2.0, 0.0)), rng).perpendicularSubdivide(1, 0.1)
      while (bottomLine.points.size < width * scale * 2) {
        bottomLine = bottomLine.perpendicularSubdivide(1, 0.1)
      }

      for (xIndex in 0 until width * scale) {
        val x = xIndex.toDouble() / scale

        // first band
        var yTarget = topLine.points[xIndex].y + random(-yVariation, yVariation, rng)
        var y = 0.0
        var start = Vector2(x, y)
        var yAdd = 0.0
        var end = start + Vector2(0.0, yAdd)
        var lum = 0.0
        while (end.y < yTarget) {
          drawer.isolated {
            stroke = hsl(0.0, 0.0, lum).toRGBa()
            strokeWeight = 1.0 / scale
            lineSegment(start, end)
          }
          start = end
          yAdd = map(-1.0, 1.0, yTarget * 0.1, yTarget * 0.3, simplex(seed, start / noiseScale * yVariation)) + random(-yVariation * yVariation, yVariation * yVariation, rng)
          end = start + Vector2(0.0, yAdd)
          lum = map(-1.0, 1.0, -0.1, 0.2, simplex(seed, end / noiseScale)) + random(-colorVariation, colorVariation, rng)
        }

        end = Vector2(x, yTarget)
        drawer.isolated {
          stroke = hsl(0.0, 0.0, lum).toRGBa()
          strokeWeight = 1.0 / scale
          lineSegment(start, end)
        }

        // second band
        start = end
        y = abs(bottomLine.points[xIndex].y) + random(-yVariation, yVariation, rng)
        end = start + Vector2(0.0, y)
        lum = map(-1.0, 1.0, 0.3, 0.7, simplex(seed, end / noiseScale)) + random(-colorVariation, colorVariation, rng)
        drawer.isolated {
          stroke = hsl(0.0, 0.0, lum).toRGBa()
          strokeWeight = 1.0 / scale
          lineSegment(start, end)
        }

        // third band
        start = end
        end = start + Vector2(0.0, height * 0.5)
        lum = map(-1.0, 1.0, -0.1, 0.2, simplex(seed, end / noiseScale)) + random(-colorVariation, colorVariation, rng)
        drawer.isolated {
          stroke = hsl(0.0, 0.0, lum).toRGBa()
          strokeWeight = 1.0 / scale
          lineSegment(start, end)
        }
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

/**
 * Algorithm in a nutshell:
 * 1. Read the comments
 *
 * Inspired by
 * https://sighack.com/post/cohen-sutherland-line-clipping-algorithm
 * Though I ended up using OpenRNDR built-ins rather than making my own clipping algorithm
 */
package sketch

import extensions.CustomScreenshots
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import shape.FractalizedLine
import shape.HatchedShape
import shape.Leaf
import util.timestamp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 750
    height = 750
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()

    println(
      """
        seed = $seed
      """.trimIndent()
    )

    val screenshots = extend(CustomScreenshots()) {
      quitAfterScreenshot = false
      scale = 3.0
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      captureEveryFrame = true
    }

    val bg = ColorRGBa.WHITE
    backgroundColor = bg

    extend {
      // get that rng
      val rng = Random(seed.toLong())

      // Base leaf params
      val nLeavesPerSegment = random(5.0, 12.0, rng).toInt()
      val leafRingAngularDeviation = PI * random(0.0, 0.7, rng)
      val meanLeafSize = width * random(0.04, 0.15, rng)
      val leafOffset = random(50.0, 100.0, rng)

      // create fractal subdivided path along which to place leaves
      val path = FractalizedLine(
        listOf(Vector2(0.0, random(height * 0.2, height * 0.8, rng)), Vector2(width.toDouble(), random(height * 0.2, height * 0.8, rng))),
        rng = rng
      ).perpendicularSubdivide(6).segments

      // Draw leaves on path with some variance
      val leaves = path.flatMap { segment ->
        val segmentAngle = atan2(segment.end.y - segment.start.y, segment.end.x - segment.start.x)
        val leafAngle = segmentAngle + random(-leafRingAngularDeviation, leafRingAngularDeviation, rng)

        List(nLeavesPerSegment) {
          val offset = random(-leafOffset, leafOffset, rng)
          val start = segment.start +
            Vector2(cos(leafAngle * PI), sin(leafAngle * PI)) * offset
          val leafSize = random(meanLeafSize * 0.8, meanLeafSize * 1.2, rng)
          Leaf(start, leafAngle, leafSize, rng = rng).convex
        }
      }

      // All the cross hatching logic has been moved into this class
      val hatchedLeaves = leaves.map { HatchedShape(it, includeCrossHatch = true, rng = rng).hatchedShape }

      hatchedLeaves.forEach { (leaf, hatches) ->
        drawer.isolated {
          fill = bg
          strokeWeight = 0.5
          stroke = ColorRGBa.BLACK
          contour(leaf)
        }

        drawer.isolated {
          fill = null
          strokeWeight = 0.2
          stroke = ColorRGBa.BLACK.opacify(0.5)
          segments(hatches)
        }
      }

      // set seed for next iteration
      seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
      screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }
  }
}

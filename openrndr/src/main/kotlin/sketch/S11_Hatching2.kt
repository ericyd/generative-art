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
import extensions.timestamp
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import shape.CrossHatch
import shape.FractalizedLine
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

    class Leaf(val start: Vector2, val angle: Double, val size: Double, val rng: Random) {
      val contour: ShapeContour
        get() {
          val end = Vector2(cos(angle), sin(angle)) * size + start
          val ctrl1 =
            // midpoint between start and end
            (end + start) * 0.5 +
              // perpendicular of the leaf angle
              Vector2(cos(angle + PI * 0.5), sin(angle + PI * 0.5)) *
              // some random "width" for the leaf
              size * random(0.175, 0.45, rng)
          // same as ctrl1 but going the other direction
          val ctrl2 = (end + start) * 0.5 +
            Vector2(cos(angle - PI * 0.5), sin(angle - PI * 0.5)) *
            size * random(0.175, 0.45, rng)

          return contour {
            moveTo(start)
            curveTo(ctrl1, end)
            curveTo(ctrl2, start)
            close()
          }
        }
    }

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
          Leaf(start, leafAngle, leafSize, rng).contour
        }
      }

      // All the cross hatching logic has been moved into this class
      val hatchedLeaves = CrossHatch(leaves, rng = rng).hatches

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

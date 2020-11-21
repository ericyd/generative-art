/**
 * Algorithm in a nutshell:
 * 1. Read the comments
 */
package sketch

import extensions.CustomScreenshots
import extensions.timestamp
import frames.circularFrame
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.contour
import org.openrndr.shape.difference
import org.openrndr.shape.drawComposition
import shape.HatchedShape
import shape.Leaf
import kotlin.math.PI
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
      val nLeavesPerSegment = random(10.0, 20.0, rng).toInt()
      val leafRingAngularDeviation = PI * random(0.0, 0.1, rng)
      val meanLeafSize = width * random(0.1, 0.15, rng)
      val maxLeafOffset = random(250.0, 350.0, rng)

      // create fractal subdivided path along which to place leaves
      val path = (0 until (height * 0.9).toInt() step 25).map { y ->
        Vector2(width * 0.5, y.toDouble())
      }.reversed()

      // Draw leaves on path with some variance
      val leaves = path.mapIndexed { index, position ->
        val baseAngle = PI * -0.5
        val leafOffset = map(0.0, path.size - 1.0, maxLeafOffset * 0.35, maxLeafOffset, index.toDouble())

        List(nLeavesPerSegment) {
          val leafAngle = baseAngle + random(-leafRingAngularDeviation, leafRingAngularDeviation, rng)
          val offset = random(-leafOffset, leafOffset, rng)
          val start = Vector2(position.x + offset, position.y)
          val leafSize = random(meanLeafSize * 0.6, meanLeafSize * 1.4, rng)
          Leaf(start, leafAngle, leafSize, rng).convex
        }
      }.flatten().sortedByDescending { it.clockwise.segments.first().start.y }

      // All the cross hatching logic has been moved into this class
      val leavesSize = leaves.size
      val hatchedLeaves = leaves.mapIndexed { index, leaf ->
        val angle = map(0.0, leavesSize.toDouble(), -PI * 0.25, -PI * 0.75, index.toDouble())
        val spacing = if (index > leavesSize / 4) {
          map(leavesSize / 4.0, leavesSize.toDouble(), 2.0, 15.0, index.toDouble()).toInt()
        } else {
          2
        }
        HatchedShape(leaf, primaryAngle = angle, spacing = spacing, includeCrossHatch = false, rng = rng).hatchedShape
      }

      // there's a circle that kind of adds some boundary around the leaves
      val circle = Circle(Vector2(width * 0.5, height * 0.5), height * 0.3)

      hatchedLeaves.forEach { (leaf, hatches) ->
        // once we get past half way up, draw the circle
        // this gives a nice overlapping effect, making it look like the leaves
        // are coming out of the circle.
        // The circularFrame is just a cheap way of cutting off the leaves that are outside the circle
        if (leaf.segments.first().start.y > circle.center.y) {
          circularFrame((circle.radius * 2.0).toInt(), (circle.radius * 2.0).toInt(), drawer, centerOverride = circle.center)
          drawer.isolated {
            fill = null
            stroke = ColorRGBa.BLACK
            strokeWeight = 1.0
            circle(circle)
          }
        }

        // leaf
        drawer.isolated {
          fill = bg
          strokeWeight = 0.5
          stroke = ColorRGBa.BLACK
          contour(leaf)
        }

        // hatches
        drawer.isolated {
          fill = null
          strokeWeight = 0.2
          stroke = ColorRGBa.BLACK.opacify(0.5)
          segments(hatches)
        }
      }

      // Draw some lines along the bottom for some nice texture
      drawer.isolated {
        fill = null
        strokeWeight = 0.5

        for (x in 0 until width step 3) {
          val totalLength = random(height * 0.1, height * 0.5, rng)
          val nSegments = random(8.0, 14.0, rng).toInt()
          var start = Vector2(x.toDouble(), height.toDouble())
          for (seg in 0 until nSegments) {
            if (start.y < height - totalLength) {
              break
            }
            val end = Vector2(x.toDouble(), start.y - random(0.1, totalLength / 2.0, rng))

            // This is such a verbose way to declare a line segment and get the difference with the circle,
            // but ultimately it's still a lot less work than writing an intersection algorithm myself, so...
            val line = contour {
              moveTo(start)
              lineTo(end)
              close()
            }
            val comp = drawComposition { shape(difference(line, circle.shape)) }
              .findShapes()
              .flatMap { s -> s.shape.contours.map { c -> c.segments.first() } }

            stroke = ColorRGBa.BLACK.opacify(
              map(height.toDouble(), height - totalLength, 0.99, 0.15, end.y)
            )
            drawer.segments(comp)
            val gap = random(2.0, 10.0, rng)
            start = Vector2(end.x, end.y - gap)
          }
        }
      }

      // set seed for next iteration
      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

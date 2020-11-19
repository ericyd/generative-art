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
import org.openrndr.math.map
import org.openrndr.shape.compound
import org.openrndr.shape.contour
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
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

    val leavesOrigin = Vector2(width * 0.1, height * 0.9)
    val nLeaves = 70
    val nLeafRings = nLeaves / 6
    val leafRingAngularMean = -PI * 0.25
    val leafRingAngularDeviation = PI * 0.2
    val maxLeafRingRadius = width * 0.75
    val meanLeafSize = maxLeafRingRadius / nLeafRings * 1.75

    extend {
      // get that rng
      val rng = Random(seed.toLong())

      // Pattern for selecting origin of leaves is
      // - calculate some rings
      // - for each ring, select some random positions on the arc within a given angular range
      val leaves = (1 until nLeafRings + 1).flatMap { ring ->
        val radius = ring.toDouble() / (nLeafRings - 1.0) * maxLeafRingRadius
        val nLeavesPerRing = map(1.0, nLeafRings + 1.0, 3.0, 10.0, ring.toDouble()).toInt()
        val leafRingAngularMin = random(0.0, leafRingAngularDeviation, rng) + leafRingAngularMean
        val leafRingAngularMax = random(0.0, -leafRingAngularDeviation, rng) + leafRingAngularMean
        val jitter = (leafRingAngularMax - leafRingAngularMin) * 0.05

        List(nLeavesPerRing) { n ->
          // leaf parameters
          val placementAngle = map(0.0, nLeavesPerRing - 1.0, leafRingAngularMin, leafRingAngularMax, n.toDouble()) +
            random(-jitter, jitter, rng)
          val leafAngle = random(leafRingAngularMin, leafRingAngularMax, rng)
          val leafSize = random(meanLeafSize * 0.8, meanLeafSize * 1.2, rng)

          // contour control points
          val start = Vector2(cos(placementAngle), sin(placementAngle)) * radius + leavesOrigin
          val end = Vector2(cos(leafAngle), sin(leafAngle)) * leafSize + start
          val ctrl1 =
            // midpoint between start and end
            (end + start) * 0.5 +
              // perpendicular of the leaf angle
              Vector2(cos(leafAngle + PI * 0.5), sin(leafAngle + PI * 0.5)) *
              // some random "width" for the leaf
              leafSize * random(0.175, 0.45, rng)
          // same as ctrl1 but going the other direction
          val ctrl2 = (end + start) * 0.5 +
            Vector2(cos(leafAngle - PI * 0.5), sin(leafAngle - PI * 0.5)) *
            leafSize * random(0.175, 0.45, rng)

          contour {
            moveTo(start)
            curveTo(ctrl1, end)
            curveTo(ctrl2, start)
            close()
          }
        }
      }

      // Pattern for hatch marks is substantially more complex
      // Comments are in line but the essence is that we need to
      // draw a bunch of hash marks at two different angles,
      // then find the intersection of the marks and the leaf shape.
      val hatchedLeaves = leaves.map { leaf ->
        // Spacing of the hatch marks
        val spacing = random(4.0, 10.0, rng).toInt()

        // Generate hatch mark angles.
        // IMO strictly perpendicular hatching angles don't look as nice
        val hatchingAngle1 = random(0.0, PI, rng)
        val hatchingAngle2 = hatchingAngle1 + random(PI * 0.25, PI * 0.5, rng)

        // determine a max length for the hatch marks
        // (they will be trimmed to size with the CompoundBuilder)
        val length = hypot(leaf.bounds.width, leaf.bounds.height)

        // Get a list of the start points for the hatch marks.
        // Since we are creating them oversized and trimming them with CompoundBuilder,
        // we can just start all of the marks along the border of the bounding rectangle of the shape
        val startYs = (leaf.bounds.y.toInt() until leaf.bounds.y.toInt() + leaf.bounds.height.toInt() step spacing).map { startY ->
          Vector2(leaf.bounds.x, startY.toDouble())
        }
        val startXs = (leaf.bounds.x.toInt() until leaf.bounds.x.toInt() + leaf.bounds.width.toInt() step spacing).map { startX ->
          Vector2(startX.toDouble(), leaf.bounds.y)
        }

        // create hash marks for both angles
        // I suspect there is an easier way to combine these in the Compound Builder
        // rather than drawing them separately and combining,
        // but I don't know what it is off the top of my head
        val hatches = listOf(hatchingAngle1, hatchingAngle2).flatMap { angle ->
          (startXs + startYs).flatMap { start ->
            val end = Vector2(start.x + cos(angle) * length, start.y + sin(angle) * length)

            // Would be simpler to do this
            //   val hatch = LineSegment(start, end)
            // but apparently compoundBuilder only works with closed contours/shapes
            val hatch = contour {
              moveTo(start)
              lineTo(end)
              close()
            }
            compound {
              intersection {
                // calling clockwise on both contours isn't really the advertised functionality of compoundBuilder
                // but it is the only way I could get it to work
                shape(hatch.clockwise)
                shape(leaf.clockwise)
              }
            }
          }
        }

        Pair(leaf, hatches)
      }

      hatchedLeaves.forEach { (leaf, hatches) ->
        drawer.isolated {
          fill = bg
          strokeWeight = 1.0
          stroke = ColorRGBa.BLACK
          contour(leaf)
        }

        drawer.isolated {
          fill = null
          strokeWeight = 0.25
          shapes(hatches)
        }
      }

      // set seed for next iteration
      seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
      screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }
  }
}

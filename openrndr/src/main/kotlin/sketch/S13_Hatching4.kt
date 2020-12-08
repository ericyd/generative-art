/**
 * Algorithm in a nutshell:
 * 1. Read the comments
 */
package sketch

import extensions.CustomScreenshots
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Triangle
import org.openrndr.shape.contour
import shape.FractalizedLine
import shape.HatchedShapePacked
import shape.Leaf
import util.timestamp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.random.Random

fun main() = application {
  configure {
    width = 750
    height = 750
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

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
      val nLeavesPerSegment = random(1.0, 2.0, rng).toInt()
      val leafAngularDeviation = PI * random(0.0, 0.1, rng)
      val meanLeafSize = width * random(0.3, 0.55, rng)
      val maxLeafOffset = random(250.0, 350.0, rng)

      // create path along which to place leaves
      val path = FractalizedLine(
        listOf(Vector2(0.0, height * 0.95), Vector2(width.toDouble(), height * 0.4)),
        rng
      ).perpendicularSubdivide(4, 0.30).points

      // Draw leaves on path with some variance
      val leaves = path.mapIndexed { index, position ->
        val baseAngle = PI * -0.25
        val leafOffset = map(0.0, path.size - 1.0, maxLeafOffset * 0.35, maxLeafOffset, index.toDouble())

        List(nLeavesPerSegment) {
          val leafAngle = baseAngle + random(-leafAngularDeviation, leafAngularDeviation, rng)
          val offset = random(-leafOffset, leafOffset, rng)
          val start = Vector2(position.x + offset, position.y)
          val leafSize = random(meanLeafSize * 0.9, meanLeafSize * 1.1, rng)
          Leaf(start, leafAngle, leafSize, rng = rng).halfConvex
        }
      }.flatten().sortedByDescending { it.clockwise.segments.first().start.y }

      // cuz... geometry?
      val triangle = Triangle(
        Vector2(width * -0.05, height * 0.95),
        Vector2(width * 0.95, height * 0.95),
        Vector2(width * 0.45, height * 0.05),
      )

      val (_, bgHatches) = HatchedShapePacked(
        Rectangle(-20.0, -20.0, width + 20.0, height + 20.0).contour,
        includeCrossHatch = false,
        primaryAngleRange = (PI * 0.1)..(PI * 0.3),
        rng = rng,
      ).hatchedShape(
        hatchLength = 30.0,
        strokeWeight = 0.1,
        strokeColor = ColorRGBa(0.5, 0.5, 0.5, 0.5),
        differenceContours = listOf(triangle.contour),
      )

      // Apply cross hatching to each leaf shape.
      // Spacing and angle of cross hatch varies with spatial origin of leaf
      val angle = atan2(path.last().y - path.first().y, path.last().x - path.first().x)
      val hatchedLeaves = leaves.mapIndexed { index, leaf ->
        HatchedShapePacked(leaf, primaryAngleRange = (angle * 0.8)..(angle * 1.2), includeCrossHatch = true, rng = rng)
          .hatchedShape(intersectionContours = listOf(triangle.contour))
      }

      // Background hatches with overlaying triangle
      drawer.composition(bgHatches)

      hatchedLeaves.forEach { (leaf, hatches) ->
        // leaf
        drawer.isolated {
          fill = bg
          strokeWeight = 0.5
          stroke = ColorRGBa.BLACK
          contour(leaf)
        }

        // hatches
        drawer.composition(hatches)
      }

      drawer.fill = null
      drawer.contour(triangle.contour)

      // set seed for next iteration
      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

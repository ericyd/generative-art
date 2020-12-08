/**
 * Using a gradient to define the hatching concentration
 */
package sketch

import extensions.CustomScreenshots
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Ellipse
import org.openrndr.shape.Rectangle
import org.openrndr.shape.contour
import shape.HatchedShapePacked
import util.BilinearConcentrationGradient
import util.RadialConcentrationGradient
import util.timestamp
import kotlin.random.Random

fun main() = application {
  configure {
    width = 950
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

      // start our "ellipse train" somewhere near the left edge
      // Create new ellipses every time, and stop when we get to the endEdge
      val startEdge = width * 0.05
      val endEdge = width * 0.95
      var lastEllipse = Ellipse(0.0, random(height * 0.2, height * 0.8, rng), startEdge, 1.0)
      val ellipses = mutableListOf<Ellipse>()
      do {
        val xRadius = random(width * 0.02, width * 0.1, rng)
        val ellipse = Ellipse(
          lastEllipse.center.x + lastEllipse.xRadius + xRadius * 0.5,
          random(height * 0.2, height * 0.8, rng),
          xRadius,
          xRadius * 0.35
        )
        lastEllipse = ellipse
        ellipses.add(ellipse)
      } while (lastEllipse.center.x + lastEllipse.xRadius < endEdge)

      // Ellipses are placed on top of rectangles,
      // to give the illusion of a cylinder
      val radiusRange = 0.1..9.0
      val radialGradient = RadialConcentrationGradient(Vector2(01.12, -0.10))
      val columnGradient = BilinearConcentrationGradient(1.0, 0.0, 1.0, 0.3)
      val hatchLength = 20.0
      val weight = 0.1
      val strokeColor = ColorRGBa.BLACK.opacify(0.2)
      val hatchedShapes = ellipses.map { outline ->
        val hatchedEllipse = HatchedShapePacked(outline.contour, includeCrossHatch = true, rng = rng)
          .hatchedShape(
            radiusRange = radiusRange,
            gradient = radialGradient,
            hatchLength = hatchLength,
            strokeWeight = weight,
            strokeColor = strokeColor
          )

        val rect = Rectangle(outline.center.x - outline.xRadius, outline.center.y, outline.xRadius * 2.0, height.toDouble())

        val hatchedRect = HatchedShapePacked(rect.contour, includeCrossHatch = true, rng = rng)
          .hatchedShape(
            radiusRange = radiusRange,
            gradient = columnGradient,
            differenceContours = listOf(outline.contour),
            hatchLength = hatchLength,
            strokeWeight = weight,
            strokeColor = strokeColor
          )

        Pair(hatchedEllipse, hatchedRect)
      }

      hatchedShapes.forEach { (hatchedEllipse, hatchedRect) ->
        drawer.isolated {
          fill = bg
          strokeWeight = 0.5
          stroke = ColorRGBa(0.3, 0.3, 0.3, 1.0)
          contour(hatchedRect.first)
          contour(hatchedEllipse.first)
        }

        drawer.composition(hatchedRect.second)
        drawer.composition(hatchedEllipse.second)
      }

      // set seed for next iteration
      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

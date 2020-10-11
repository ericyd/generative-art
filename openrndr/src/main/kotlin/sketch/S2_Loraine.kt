/**
 * Inspired by Google Earth
 * https://earthview.withgoogle.com/loraine-united-states-1727
 * https://www.gstatic.com/prettyearth/assets/full/1727.jpg
 *
 * My attempt will start by creating a grid of 8 ~evenly spaced rectangles.
 * Each rectangle will have a different noise function and have a flow field.
 * In addition, the noise function will somehow map to an elevation function that will
 * provide a background gradient that corresponds to the noise function.
 * The flow field and background will be unique to each rectangle and will not spill out
 * (except for places where it is intentional to do so)
 */
package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// TODO: Would be really cool if rectangles were irregular and packed together!
fun generateRectangles(nRectangles: Int, dimensions: Vector2, bounds: Vector2): List<Rectangle> {
  val rectWidth = bounds.x / dimensions.x
  val rectHeight = bounds.y / dimensions.y
  return List(nRectangles) { n ->
    val y = n % dimensions.y
    val x = (n - y) / dimensions.y
    val center = Vector2(rectWidth * x, rectHeight * y)
    Rectangle(center, rectWidth, rectHeight)
  }
}

fun fillRectangles(rectangles: List<Rectangle>): List<ShapeContour> =
  rectangles.mapIndexed { index, rect ->
    val stepSize = 10

    // Create some pseudo-random values to keep things interesting
    val seed = rect.width.toInt() % (index + 1) * 1020 + 20
    val baseNoise = simplex(seed, rect.corner.x + seed / (index + 1), rect.corner.y + seed * index)
    val scale = (baseNoise + 1.0) * map(-1.0, 1.0, 50.0, 150.0, baseNoise)

    // Start points for contours: grid inside rectangle bounds
    (rect.corner.x.toInt() until (rect.corner.x + rect.width).toInt() step stepSize).flatMap { x ->
      (rect.corner.y.toInt() until (rect.corner.y + rect.height).toInt() step stepSize).map { y ->

        // Draw contour based on noise function
        val c = contour {
          moveTo(x.toDouble(), y.toDouble())
          for (i in 0 until 200) {
            val noise = map(-1.0, 1.0, 0.0, 2.0 * PI, simplex(seed, cursor / scale))
            lineTo(cursor + Vector2(cos(noise), sin(noise)))
          }
        }

        // Only take points in the contour that are contained within the rectangle
        ShapeContour(c.segments.takeWhile { rect.contains(it.end) }, false)
      }
    }
  }.flatten()

fun main() = application {
  configure {
    width = 1000
    height = 700
  }

  program {
    // In future, consider implementing own screenshot mechanism that could run after each draw loop
    // screenshot: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-extensions/src/main/kotlin/org/openrndr/extensions/Screenshots.kt
    // timestamp: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-core/src/main/kotlin/org/openrndr/utils/NamedTimestamp.kt
    extend(Screenshots()) {
      quitAfterScreenshot = true
      scale = 2.0
    }
    backgroundColor = ColorRGBa.WHITE

    val nRectangles = 8
    val dimensions = Vector2(4.0, 2.0)
    val rectangles = generateRectangles(nRectangles, dimensions, Vector2(width.toDouble(), height.toDouble()))
    val contours = fillRectangles(rectangles)

    extend {
      drawer.stroke = ColorRGBa.BLACK
      drawer.strokeWeight = 2.0
      rectangles.forEach {
        drawer.rectangle(it)
      }

      drawer.stroke = ColorRGBa(0.1, 0.1, 0.1, 1.0)
      drawer.strokeWeight = 0.5
      drawer.contours(contours)
    }
  }
}

package sketch.flow

import force.AntigravityBody
import force.GravityBody
import force.GravitySystem
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 1000
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
    val lineLength = 500
    val seed = 14950199490419014
    val rand = Random(seed)

    // these attract things
    val gravityBodies = listOf(
      GravityBody(width * 1.25, -height * 0.15, 300.0), // upper right
      GravityBody(-width * 0.75, height / 4.0, 500.0), // middle left
      GravityBody(width * 1.75, height * 2.0 / 3.0, 300.0), // middle right
      GravityBody(width / 6.0, height * 1.25, 450.0), // bottom center-left
      GravityBody(width + 300.0, height + 300.0, 500.0) // bottom right
    )

    // these repel things
    val antigravityBodies = List(15) {
      AntigravityBody(random(0.0, width.toDouble(), rand), random(0.0, height.toDouble(), rand), 0.75)
    }

    // this calculates net forces on a point mass
    val system = GravitySystem(10.0, gravityBodies + antigravityBodies)

    // draw lines emanating from each gravitational body.
    // Around each body, drawn several layers of contours
    val contours: List<ShapeContour> = antigravityBodies.flatMap { body ->
      // create different radii for contour groups
      (10 until 200 step 25).flatMap { radius ->
        val nMax = 20
        // draw nMax lines around each circumference
        List(nMax) { n ->
          val angle = map(0.0, nMax.toDouble(), 0.0, 2.0 * PI, n.toDouble())
          contour {
            moveTo(Vector2(body.x + cos(angle) * radius, body.y + sin(angle) * radius))
            List(lineLength) {
              // `cursor` points to the end point of the previous command - AMAZING!!
              val force = system.force(cursor)
              lineTo(cursor + force)
            }
          }
        }
      }
    }

    extend {
      drawer.stroke = ColorRGBa(0.0, 0.0, 0.0, 0.65)
      drawer.strokeWeight = 0.65
      contours.forEach { drawer.contour(it) }
    }
  }
}

/**
 * A "web" like gravity system
 * 2020-08-05
 * Seed: 349383817327
 */
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
    val seed = random(1.0, 1000000000000.0).toLong() // know your seed 😛
    println("seed: $seed")
    val rand = Random(seed)
    val center = Vector2(width / 2.0, height / 2.0)

    // these attract things
    val nRings = 3
    val nBodies = 8
    val halfWidth = width * 0.5
    val baseRadius = halfWidth
    val gravityBodies = List(nRings) { ring ->
      List(nBodies) { n ->
        val angle = map(0.0, nBodies.toDouble(), 0.0, 2.0 * PI, n.toDouble()) + (PI / (ring + 2.46))
        val radius = map(0.0, nRings.toDouble(), baseRadius * 0.15, baseRadius * 1.1, ring.toDouble())
        GravityBody(center.x + cos(angle) * radius, center.y + sin(angle) * radius, random(1.0, 2.0, rand))
      }
    }.flatten()

    // these repel things
    val antigravityBodies = List(nRings) { ring ->
      List(nBodies) { n ->
        val angle = map(0.0, nBodies.toDouble(), 0.0 + PI / 8.0, 2.0 * PI + PI / 8.0, n.toDouble()) + (PI / (ring + 2.96))
        val radius = map(0.0, nRings.toDouble(), baseRadius * 0.3, baseRadius * 1.25, ring.toDouble())
        AntigravityBody(center.x + cos(angle) * radius, center.y + sin(angle) * radius, random(1.5, 2.50, rand))
      }
    }.flatten()

    // this calculates net forces on a point mass
    val system = GravitySystem(1.0, gravityBodies + antigravityBodies)

    // draw lines emanating from each gravitational body.
    // Around each body, drawn several layers of contours
    val nContours = 105
    val maxRadius = 70.00
    val contours: List<ShapeContour> = antigravityBodies.flatMap { body ->
      List(nContours) { n ->
        val cRadius = random(1.0, maxRadius, rand)
        val angle = map(0.0, nContours.toDouble(), 0.0, 2.0 * PI, n.toDouble())
        contour {
          moveTo(body.x + cos(angle) * cRadius, body.y + sin(angle) * cRadius)
          List(lineLength) {
            lineTo(cursor + system.force(cursor))
          }
        }
      }
    }

    extend {
      drawer.stroke = ColorRGBa(0.0, 0.0, 0.0, 0.45)
      drawer.strokeWeight = 0.55
      contours.forEach { drawer.contour(it) }
    }
  }
}

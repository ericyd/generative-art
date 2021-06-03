/**
 * A gentle gravity field, with some noise.
 *
 * The idea here is that each flow field is a particle that is seeking a target.
 * The particle has a certain amount of speed with which it pursues the target,
 * but it is also affected by the gravity field around it.
 *
 * The color of the particle is defined by a custom palette
 */
package sketch.flow

import color.Palette
import force.AntigravityBody
import force.GravityBody
import force.GravitySystem
import force.PhysicalBody
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.hsl
import org.openrndr.draw.LineCap
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import util.timestamp
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 1000
  }

  program {
    backgroundColor = ColorRGBa.WHITE
    val seed = random(1.0, 1000000000000.0).toLong() // know your seed 😛
    println("seed: $seed")
    val rand = Random(seed)

    // In future, consider implementing own screenshot mechanism that could run after each draw loop
    // screenshot: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-extensions/src/main/kotlin/org/openrndr/extensions/Screenshots.kt
    // timestamp: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-core/src/main/kotlin/org/openrndr/utils/NamedTimestamp.kt
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 4.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
    }

    val lineLength = 1500
    val padding = 300

    val colors = listOf(
      hsl(261.0, 0.45, 0.43), // purple
      hsl(238.0, 0.67, 0.36), // dark blue
      hsl(194.0, 0.70, 0.85), // light blue
      hsl(10.0, 0.40, 0.15), // dark brown
      hsl(255.0, 0.46, 0.86), // light purple
      hsl(173.0, 0.66, 0.975), // smokey white
      hsl(29.0, 0.93, 0.83) // orange/salmon
    )

    // val palette = Palette(colors.map { it.toRGBa() }, 0.0, hypot(width.toDouble(), height.toDouble()), rand)
    val palette = Palette(colors.map { it.toRGBa() }, 0.0, width + padding * 2.0, rand)

    // these attract things
    val yOffset = 100.0
    val gravityBodies: List<GravityBody> = listOf(
      GravityBody(width * 0.1, height * 1.1, 100000.0),
      GravityBody(width * -0.95, height * 0.15, 70000.0),
      GravityBody(width * 1.2, height * 0.20, 150000.0)
    )

    // these repel things
    val primaryAntigravityBodies = listOf(
      AntigravityBody(width * -0.05, height + yOffset * 0.5, 100000.0),
      AntigravityBody(width * 0.85, -yOffset * 0.5, 150000.0)
    )

    // this calculates net forces on a point mass
    val system = GravitySystem(1.0, gravityBodies + primaryAntigravityBodies)

    val stepSize = 20
    val target = Vector2(width * 1.1, height * 1.1)
    val contours: List<ShapeContour> = (-padding until width + padding step stepSize).flatMap { x ->
      (-padding until height + padding step stepSize).map { y ->
        val body = PhysicalBody(Vector2(random(x * 0.95, x * 1.05, rand), random(y * 0.95, y * 1.05, rand)), random(50.0, 150.0, rand), random(15.0, 25.0, rand), target)
        contour {
          moveTo(body.coords)
          List(lineLength) {
            lineTo(body.move(system))
          }
        }
      }
    }

    extend {
      drawer.strokeWeight = 0.55
      drawer.lineCap = LineCap.ROUND
      contours.forEach {
        val start = it.segments.first().start
        drawer.stroke = palette.colorAt(start.x).opacify(0.885)
        drawer.contour(it)
      }
      drawer.circle(gravityBodies[0].origin, 10.0)
    }
  }
}

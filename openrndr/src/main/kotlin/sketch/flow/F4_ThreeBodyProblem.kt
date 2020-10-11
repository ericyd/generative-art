/**
 * A naive solution to the three body problem
 */
package sketch.flow

import force.GravityBody
import force.GravitySystem
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
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
    val lineLength = 1500
    val seed = random(1.0, 1000000000000.0).toLong() // know your seed 😛
    println("seed: $seed")
    val rand = Random(seed)
    val center = Vector2(width / 2.0, height / 2.0)

    // these attract things
    val bodies = listOf(
      GravityBody(width * 0.33, height * 0.33, 15.0),
      GravityBody(width * 0.66, height * 0.33, 10.0),
      GravityBody(width * 0.50, height * 0.66, 20.0)
    )

    // this calculates net forces on a point mass
    val system = GravitySystem(1.0, bodies)

    // draw lines emanating from each gravitational body.
    // Around each body, drawn several layers of contours
    val iterations = 1000
    val circles: List<Circle> = List(iterations) { it }.flatMap {
      system.nextTick()
      bodies.map { body -> Circle(body.origin, 2.0) }
    }

    extend {
      drawer.stroke = ColorRGBa(0.0, 0.0, 0.0, 0.85)
      drawer.strokeWeight = 0.75
      drawer.fill = ColorRGBa.BLACK
      circles.forEach { drawer.circle(it) }
    }
  }
}

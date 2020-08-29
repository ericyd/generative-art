/**
 * Identical to F5_Orbit, but in Black & White
 */
package sketch.flow

import force.GravityBody
import force.GravitySystem
import force.PhysicalBody
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

fun main() = application {
  configure {
    width = 1000
    height = 1400
  }

  program {
    // In future, consider implementing own screenshot mechanism that could run after each draw loop
    // screenshot: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-extensions/src/main/kotlin/org/openrndr/extensions/Screenshots.kt
    // timestamp: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-core/src/main/kotlin/org/openrndr/utils/NamedTimestamp.kt
    extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 2.0
    }

    // if rendering with circles, the screenshot makes the result much brighter.
    // You'll need to fiddle with these to get a good "final shot",
    // Also note that circles need to be at least 1.5 px to really be visible
    val strokeWeight = 0.95
    val opacity = 0.185
    val nLines = 600
    var settings = Settings(
      // random(1.0, Long.MAX_VALUE.toDouble()).toLong() // know your seed 😛
      8238401536276667392,
      lineLength = 3000,
      bodyCount = 10,
      scaleOrNull = 1.6
    )

    println(settings)
    val center = Vector2(width / 2.0, height / 2.0)

    val bg = ColorRGBa.BLACK
    val fg = ColorRGBa.WHITE.opacify(opacity)
    // val bg = ColorRGBa.WHITE
    // val fg = ColorRGBa.BLACK.opacify(opacity)
    backgroundColor = bg

    val gravityBodies: List<GravityBody> = List(settings.nBodies) {
      GravityBody(
        x = random(width * -0.75, width * 1.75, settings.rand),
        y = random(height * -0.75, height * 1.75, settings.rand),
        mass = random(900.0, 1000.0, settings.rand),
        rand = settings.rand
      )
    }

    // this calculates net forces on a point mass
    val system = GravitySystem(2.0, gravityBodies)

    val maxRadius = hypot(width / 2.0, height / 2.0)
    val nAngles = 7
    val contours: List<ShapeContour> = (0 until nAngles).flatMap { index ->
      val angle = map(0.0, nAngles.toDouble(), 0.0, 2.0 * PI, index.toDouble())

      // create a "swarm" of contours originating from one spot at the given angle
      (0 until nLines).map {
        val randomizedAngle = random(angle * 0.4, angle * 1.6, settings.rand)
        val randomizedRadius = random(maxRadius * 0.8, maxRadius * 1.2, settings.rand)
        val body = PhysicalBody(
          center + Vector2(cos(randomizedAngle) * randomizedRadius, sin(randomizedAngle) * randomizedRadius),
          mass = random(205.0, 315.0, settings.rand),
          speed = 0.0
        )
        contour {
          moveTo(body.coords)
          List(settings.lineLength) {
            lineTo(body.orbit(system, settings.scale, center))
          }
        }
      }
    }

    // val points = contours.flatMap { it.segments.map { it.start } }
    //   // take every third point
    //   .mapIndexed { index, pt -> Pair(index, pt) }
    //   .filter { (index, _) -> index % 4 == 0 }
    //   .map { (_, pt) -> pt }

    extend {
      drawer.fill = null
      drawer.strokeWeight = strokeWeight
      drawer.lineCap = LineCap.ROUND
      drawer.stroke = fg
      // drawer.contours doesn't handle huge lists very well
      contours.chunked(500).forEach { drawer.contours(it) }

      // Use this for points, use above for lines
      // drawer.stroke = fg
      // drawer.fill = fg
      // drawer.circles(points, strokeWeight)
      // drawer.points(points)
    }
  }
}

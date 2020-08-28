/**
 * A gentle gravity field, with some noise.
 *
 * This gravity field is slightly different in that instead of acting on
 * the current point in the line, it acts on the point perpendicular to the
 * point. That creates some interesting spirals, and also some challenges with getting things
 * on screen, but nothing a few magic numbers can't solve
 */
package sketch.flow

import force.GravityBody
import force.GravitySystem
import force.PhysicalBody
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.hsla
import org.openrndr.draw.LineCap
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.contour
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class Settings(val seed: Long, val scale: Double, val lineLength: Int, var bodyCount: Int? = null) {
  val rand = Random(seed)
  val nBodies = bodyCount ?: random(4.0, 8.0, rand).toInt()
}

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
      quitAfterScreenshot = false
      scale = 2.0
    }
    backgroundColor = ColorRGBa.BLACK
    val settings = Settings(
      seed = random(1.0, Long.MAX_VALUE.toDouble()).toLong(), // know your seed 😛
      lineLength = 1500,
      scale = 3.0,
      bodyCount = null
    )

    // Some collections of "settings" (probably should make a data class for this in future
    // val settings = Settings(
    //   seed = 596848407871,
    //   scale = 1.0,
    //   bodyCount = 4,
    //   lineLength = 1500
    // )

    println("seed: ${settings.seed}")
    val center = Vector2(width / 2.0, height / 2.0)

    // These are really beautiful but take a while for "prototyping"
    // val strokeWeight = 0.75
    // val opacity = 0.15
    // val nLines = 1700

    // Good for quick design checks
    val strokeWeight = 0.75
    val opacity = 0.85
    val nLines = 200

    val colors = listOf(
      hsla(261.0, 0.45, 0.43, opacity), // purple
      hsla(212.0, 0.67, 0.30, opacity), // dark blue
      hsla(194.0, 0.70, 0.85, opacity), // light blue
      hsla(10.0, 0.40, 0.15, opacity), // dark brown
      hsla(255.0, 0.46, 0.86, opacity), // light purple
      hsla(173.0, 0.66, 0.975, opacity), // smokey white
      hsla(29.0, 0.93, 0.83, opacity) // orange/salmon
    )

    // these attract things
    // val gravityBodies: List<GravityBody> = listOf(
    //   GravityBody(width * 0.51, height * 0.51, 100000.0)
    // )
    val gravityBodies: List<GravityBody> = List(settings.nBodies) {
      GravityBody(
        // because of the way the "spiral" movement function works, offsetting the x/y of the gravity body makes it more centered
        random(width * -0.45, width * 1.45, settings.rand), // + width / 2.0 * settings.scale,
        random(height * -0.45, height * 1.45, settings.rand), // - height / 2.0 * settings.scale,
        random(900.0, 1000.0, settings.rand)
      )
    }

    // this calculates net forces on a point mass
    val system = GravitySystem(2.0, gravityBodies)

    val radius = hypot(width / 2.0, height / 2.0)
    val colorContours: List<ColorContours> = colors.mapIndexed { index, colorHSLa ->
      val angle = map(0.0, colors.size.toDouble(), 0.0, 2.0 * PI, index.toDouble())

      // create a "swarm" of contours originating from one spot at the given angle
      val contours = (0 until nLines).map {
        val randomizedAngle = random(angle * 0.8, angle * 1.2, settings.rand)
        val randomizedRadius = random(radius * 0.8, radius * 1.2, settings.rand)
        val body = PhysicalBody(
          center + Vector2(cos(randomizedAngle) * randomizedRadius, sin(randomizedAngle) * randomizedRadius),
          mass = random(205.0, 315.0, settings.rand),
          speed = 10.0
        )
        contour {
          moveTo(body.coords)
          List(settings.lineLength) {
            lineTo(body.spiral2(system, settings.rand, settings.scale))
          }
        }
      }
      ColorContours(colorHSLa.toRGBa(), contours)
    }

    extend {
      drawer.strokeWeight = strokeWeight
      drawer.lineCap = LineCap.ROUND
      colorContours.forEach { colorContour ->
        drawer.stroke = colorContour.color
        colorContour.contours.forEach { drawer.contour(it) }
      }
    }
  }
}

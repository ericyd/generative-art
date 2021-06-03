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
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import util.timestamp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class ColorContours(val color: ColorRGBa, val contours: List<ShapeContour>)

fun main() = application {
  configure {
    width = 1000
    height = 1000
  }

  program {
    backgroundColor = ColorRGBa.BLACK
    val lineLength = 1500
    var seed = random(1.0, 1000000000000.0).toLong() // know your seed 😛
    var nBodies = 2
    var scale = 2.0

    // Some collections of "settings" (probably should make a data class for this in future
    // seed = 596848407871
    // nBodies = 4
    // scale = 1.0

    // seed = 362244468987
    // nBodies = 2
    // scale = 2.0

    // seed = 230337025792
    // nBodies = 2
    // scale = 2.0

    // seed = 733914103222
    // nBodies = 2
    // scale = 2.0

    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    extend(Screenshots()) {
      quitAfterScreenshot = false
      this.scale = 4.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
    }

    println("seed: $seed")
    val rand = Random(seed)
    val center = Vector2(width / 2.0, height / 2.0)
    nBodies = random(2.0, 4.0, rand).toInt()

    // These are really beautiful but take a while for "prototyping"
    val strokeWeight = 0.75
    val opacity = 0.15
    val nLines = 1700

    // Good for quick design checks
    // val strokeWeight = 0.75
    // val opacity = 0.85
    // val nLines = 200

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
    val gravityBodies: List<GravityBody> = List(nBodies) {
      GravityBody(
        // because of the way the "spiral" movement function works, offsetting the x/y of the gravity body makes it more centered
        random(width * -0.45, width * 1.45, rand) + width / 2.0 * scale,
        random(height * -0.45, height * 1.45, rand) - height / 2.0 * scale,
        random(100.0, 1000.0, rand)
      )
    }

    // this calculates net forces on a point mass
    val system = GravitySystem(1.0, gravityBodies)

    val radius = hypot(width / 2.0, height / 2.0)
    val colorContours: List<ColorContours> = colors.mapIndexed { index, colorHSLa ->
      val angle = map(0.0, colors.size.toDouble(), 0.0, 2.0 * PI, index.toDouble())

      // create a "swarm" of contours originating from one spot at the given angle
      val contours = (0 until nLines).map {
        val randomizedAngle = random(angle * 0.8, angle * 1.2, rand)
        val randomizedRadius = random(radius * 0.8, radius * 1.2, rand)
        val body = PhysicalBody(
          center + Vector2(cos(randomizedAngle) * randomizedRadius, sin(randomizedAngle) * randomizedRadius),
          random(85.0, 115.0, rand)
        )
        contour {
          moveTo(body.coords)
          List(lineLength) {
            lineTo(body.spiral(system, scale))
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

/**
 * A gentle gravity field, where particles end up "orbiting" the gravity points
 *
 * Essentially, instead of calculating a force straight from the point,
 * it calculates a force from the perpendicular of the point
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
import org.openrndr.math.YPolarity
import org.openrndr.math.map
import org.openrndr.shape.contour
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class Settings(val seed: Long, val scaleOrNull: Double? = null, val lineLength: Int, var bodyCount: Int? = null) {
  val rand = Random(seed)
  val nBodies = bodyCount ?: random(1.0, 6.0, rand).toInt()
  val scale = scaleOrNull ?: random(1.0, 4.0, rand)
  // Since we define an instance of Random in this data class,
  // the results of random(rand) will be different depending on
  // whether or not it is used to define `nBodies` or `scale`.
  // Essentially, if it isn't used enough, we "compensate" by calling random(rand)
  // as many times as we need to. This ensures that the settings that are printed
  // will generate an identical image, because the Random instance will be used
  // the same number of times.
  // This is a **breaking change** and existing sketches will look different with this
  val throwaway = if (scaleOrNull != null && bodyCount != null) {
    random(random = rand) + random(random = rand)
  } else if (scaleOrNull != null || bodyCount != null) {
    random(random = rand)
  } else {
    0.0
  }

  override fun toString(): String = """
    Settings(
      seed = $seed,
      lineLength = $lineLength,
      scaleOrNull = $scale,
      bodyCount = $nBodies
    )
  """.trimIndent()
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
    val seed = random(1.0, Long.MAX_VALUE.toDouble()).toLong() // know your seed 😛
    var settings = Settings(
      seed,
      lineLength = 2000
    )

    // settings = Settings(
    //   1180569484161769472,
    //   lineLength = 1500,
    //   bodyCount = 3,
    //   scaleOrNull = 3.0
    // )

    // Some collections of "settings" (probably should make a data class for this in future
    // settings = Settings(
    //   seed = 4112586598597632001,
    //   scaleOrNull = 3.0,
    //   bodyCount = 8,
    //   lineLength = 1500
    // )

    // settings = Settings(
    //   seed = 8986313017738607615,
    //   scaleOrNull = 2.379761980399831,
    //   bodyCount = 4,
    //   lineLength = 1500
    // )

    // settings = Settings(
    //   seed = 7376653687330572287,
    //   lineLength = 1500,
    //   scaleOrNull = 2.3008692931618744,
    //   bodyCount = 6
    // )

    // settings = Settings(
    //   seed = 5580551685306258433,
    //   lineLength = 1500,
    //   scaleOrNull = 2.69,
    //   bodyCount = 8
    // )

    // settings = Settings(
    //   seed = 1517705629092318208,
    //   lineLength = 2000,
    //   scaleOrNull = 1.0938661171250148,
    //   bodyCount = 4
    // )

    // settings = Settings(
    //   seed = 5523075166130460672,
    //   lineLength = 2000,
    //   scaleOrNull = 1.510784284089783,
    //   bodyCount = 8
    // )

    settings = Settings(
      seed = 5523107165134601478,
      lineLength = 1000,
      scaleOrNull = 3.81,
      bodyCount = 6
    )

    println("seed: $seed; settings: $settings")
    val center = Vector2(width / 2.0, height / 2.0)

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

    val gravityBodies: List<GravityBody> = List(settings.nBodies) {
      GravityBody(
        // because of the way the "spiral" movement function works, offsetting the x/y of the gravity body makes it more centered
        x = random(width * -0.75, width * 1.75, settings.rand),
        y = random(height * -0.75, height * 1.75, settings.rand),
        mass = random(900.0, 1000.0, settings.rand),
        rand = settings.rand
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
        val randomizedRadius = random(radius * 0.08, radius * 1.2, settings.rand)
        val body = PhysicalBody(
          center + Vector2(cos(randomizedAngle) * randomizedRadius, sin(randomizedAngle) * randomizedRadius),
          mass = random(205.0, 315.0, settings.rand),
          speed = 1.30
        )
        contour {
          moveTo(body.coords)
          List(settings.lineLength) {
            lineTo(body.orbit(system, settings.scale, center))
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

/**
 * Identical to F5_Orbit, but in Black & White
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
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.contour
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class F7Settings(
  val seed: Long,
  val rand: Random,
  val scale: Double,
  val lineLength: Int,
  val nBodies: Int,
  val nLines: Int,
  val bodySpeed: Double,
  val strokeWeight: Double,
  val opacity: Double,
  val needToBurnCycles: Boolean = true
) {
  override fun toString(): String =
    """
    val settings = F7Settings(
      seed = $seed,
      rand = Random($seed),
      scale = $scale,
      lineLength = $lineLength,
      nBodies = $nBodies,
      nLines = $nLines,
      bodySpeed = $bodySpeed,
      strokeWeight = $strokeWeight,
      opacity = $opacity
    )
    """.trimIndent()
}

data class ColorPoints(val color: ColorRGBa, val points: List<Vector2>)

fun main() = application {
  configure {
    width = 1000
    height = 1200
  }

  program {
    // In future, consider implementing own screenshot mechanism that could run after each draw loop
    // screenshot: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-extensions/src/main/kotlin/org/openrndr/extensions/Screenshots.kt
    // timestamp: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-core/src/main/kotlin/org/openrndr/utils/NamedTimestamp.kt
    extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 2.0
    }

    val seed = random(1.0, Long.MAX_VALUE.toDouble()).toLong() // know your seed 😛
    val rand = Random(seed)
    // var settings = F7Settings(
    //   seed,
    //   rand,
    //   nBodies = random(1.0, 6.0, rand).toInt(),
    //   scale = random(1.0, 4.0, rand),
    //   strokeWeight = 0.95,
    //   // strokeWeight = random(0.95, 1.45, rand),
    //   opacity = 0.585,
    //   lineLength = random(500.0, 1500.0, rand).toInt(),
    //   nLines = random(100.0, 300.0, rand).toInt(),
    //   // nLines = 200,
    //   bodySpeed = random(0.0, 0.1, rand),
    //   needToBurnCycles = false // this is a silly thing... just trying to make my life easier re: saving settings
    // )

    val settings = F7Settings(
      seed = 1363440141224000512,
      rand = Random(1363440141224000512),
      scale = 2.5706738008551597,
      lineLength = 1166,
      nBodies = 5,
      nLines = 103,
      bodySpeed = 0.078998566399215,
      strokeWeight = 0.95,
      opacity = 0.785
    )

    println(settings)
    // burn some Random cycles
    if (settings.needToBurnCycles) {
      random(random = settings.rand); random(random = settings.rand); random(random = settings.rand); random(random = settings.rand); random(random = settings.rand)
    }
    val center = Vector2(width / 2.0, height / 2.0)

    val bg = ColorRGBa.BLACK
    val fg = ColorRGBa.WHITE.opacify(settings.opacity)
    backgroundColor = bg

    val gravityBodies: List<GravityBody> = List(settings.nBodies) {
      GravityBody(
        x = random(width * -0.75, width * 1.75, settings.rand),
        y = random(height * -0.75, height * 1.75, settings.rand),
        mass = random(400.0, 1000.0, settings.rand),
        rand = settings.rand
      )
    }

    // this calculates net forces on a point mass
    val system = GravitySystem(2.0, gravityBodies)

    val maxRadius = hypot(width / 2.0, height / 2.0)
    // val colors = listOf(
    //   ColorRGBa.fromHex("10183C"),
    //   ColorRGBa.fromHex("ACADF1"),
    //   ColorRGBa.fromHex("3C1F42"),
    //   ColorRGBa.fromHex("EBD1F5"),
    //   ColorRGBa.fromHex("420000"),
    //   ColorRGBa.fromHex("8C8EF2"),
    //   ColorRGBa.fromHex("020E36"),
    //   ColorRGBa.fromHex("D8EEDD")
    // )

    val colors = listOf(
      hsla(261.0, 0.45, 0.43, settings.opacity), // purple
      hsla(212.0, 0.67, 0.30, settings.opacity), // dark blue
      hsla(194.0, 0.70, 0.85, settings.opacity), // light blue
      hsla(10.0, 0.40, 0.15, settings.opacity), // dark brown
      hsla(255.0, 0.46, 0.86, settings.opacity), // light purple
      hsla(173.0, 0.66, 0.975, settings.opacity), // smokey white
      hsla(29.0, 0.93, 0.83, settings.opacity) // orange/salmon
    ).map { it.toRGBa() }

    val nColors = colors.size
    val angleVariation = PI / nColors.toDouble()
    val colorContours: List<ColorContours> = colors.mapIndexed { index, color ->
      val angle = map(0.0, colors.size.toDouble(), 0.0, 2.0 * PI, index.toDouble())

      // create a "swarm" of contours originating from one spot at the given angle
      val contours = (0 until settings.nLines).map {
        val randomizedAngle = random(angle - angleVariation, angle + angleVariation, settings.rand)
        val randomizedRadius = random(maxRadius * 0.735, maxRadius * 1.2, settings.rand)
        val body = PhysicalBody(
          center + Vector2(cos(randomizedAngle) * randomizedRadius, sin(randomizedAngle) * randomizedRadius),
          mass = random(205.0, 315.0, settings.rand),
          speed = settings.bodySpeed
        )
        contour {
          moveTo(body.coords + simplex(seed.toInt(), body.coords / 150.0) * 15.0)
          List(settings.lineLength) {
            lineTo(body.orbit(system, settings.scale, center) + simplex(seed.toInt(), body.coords / 150.0) * 15.0)
          }
        }
      }
      ColorContours(color, contours)
    }

    // val colorPoints = colorContours.map {
    //   ColorPoints(it.color, it.contours.flatMap { it.segments.map { it.start }.filterIndexed { index, pt -> index % (settings.strokeWeight.toInt() * 4) == 0 } })
    // }

    extend {
      drawer.fill = null
      drawer.strokeWeight = settings.strokeWeight
      drawer.lineCap = LineCap.ROUND
      drawer.stroke = fg
      colorContours.forEach { colorContour ->
        drawer.stroke = colorContour.color
        colorContour.contours.forEach { drawer.contour(it) }
      }

      // Use this for points, use above for lines
      // colorPoints.forEach { colorPoint ->
      //   drawer.stroke = colorPoint.color
      //   drawer.fill = colorPoint.color
      //   drawer.circles(colorPoint.points, settings.strokeWeight)
      // }
      // drawer.points(points)
    }
  }
}

/**
 * Identical to F5_Orbit, but in Black & White
 */
package sketch.flow

import force.GravityBody
import force.GravitySystem
import force.PhysicalBody
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgba
import org.openrndr.draw.LineCap
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.contour
import util.timestamp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class F8Settings(
  val seed: Long,
  val rand: Random,
  val scale: Double,
  val lineLength: Int,
  val nBodies: Int,
  val nLines: Int,
  val bodySpeed: Double,
  val strokeWeight: Double,
  val opacity: Double,
  val minRadius: Double,
  val maxRadius: Double,
  val needToBurnCycles: Boolean = true
) {
  override fun toString(): String =
    """
    val settings = F8Settings(
      seed = $seed,
      rand = Random($seed),
      scale = $scale,
      lineLength = $lineLength,
      nBodies = $nBodies,
      nLines = $nLines,
      bodySpeed = $bodySpeed,
      strokeWeight = $strokeWeight,
      minRadius = $minRadius,
      maxRadius = $maxRadius,
      opacity = $opacity
    )
    """.trimIndent()
}

fun main() = application {
  configure {
    width = 1000
    height = 1200
  }

  program {
    val seed = random(1.0, Long.MAX_VALUE.toDouble()).toLong() // know your seed 😛
    val rand = Random(seed)
    // val settings = F8Settings(
    //   seed,
    //   rand,
    //   nBodies = random(1.0, 4.2, rand).toInt(),
    //   scale = random(1.0, 4.0, rand),
    //   strokeWeight = 0.95,
    //   opacity = 0.585,
    //   lineLength = random(200.0, 2000.0, rand).toInt(),
    //   nLines = random(100.0, 800.0, rand).toInt(),
    //   minRadius = random(0.2, 0.5, rand),
    //   maxRadius = random(0.51, 1.2, rand),
    //   bodySpeed = if (random(random = rand) < 0.0) 0.0 else 0.5,
    //   needToBurnCycles = false // this is a silly thing... just trying to make my life easier re: saving settings
    // )

    val settings = F8Settings(
      seed = 8194798619954445312,
      rand = Random(8194798619954445312),
      scale = 2.1528353570436742,
      lineLength = 1542,
      nBodies = 2,
      nLines = 291,
      bodySpeed = 0.5,
      strokeWeight = 0.95,
      minRadius = 0.39534779589478436,
      maxRadius = 0.7625887368882514,
      opacity = 0.585
    )

    // val settings = F8Settings(
    //   seed = 6121586236746043392,
    //   rand = Random(6121586236746043392),
    //   scale = 2.9188363083383626,
    //   lineLength = 190,
    //   nBodies = 4,
    //   nLines = 960,
    //   bodySpeed = 0.0,
    //   strokeWeight = 0.95,
    //   minRadius = 0.1576940148962754,
    //   maxRadius = 0.5868613389825957,
    //   opacity = 0.585
    // )

    // circle
    // val settings = F8Settings(
    //   seed = 5949447342224600064,
    //   rand = Random(5949447342224600064),
    //   scale = 2.7966425950746716,
    //   lineLength = 412,
    //   bodySpeed = 0.0,
    //   nBodies = 1,
    //   nLines = 599,
    //   strokeWeight = 0.95,
    //   minRadius = 0.3659914961526285,
    //   maxRadius = 0.6460065576698675,
    //   opacity = 0.585
    // )

    // val settings = F8Settings(
    //   seed = 2063554222836573184,
    //   rand = Random(2063554222836573184),
    //   scale = 1.803228945094613,
    //   lineLength = 314,
    //   nBodies = 3,
    //   nLines = 656,
    //   bodySpeed = 0.250,
    //   strokeWeight = 0.95,
    //   minRadius = 0.3515561978115267,
    //   maxRadius = 0.6956094213724992,
    //   opacity = 0.585
    // )

    // val settings = F8Settings(
    //   seed = 2733654308464734208,
    //   rand = Random(2733654308464734208),
    //   scale = 3.768637761064892,
    //   lineLength = 1258,
    //   nBodies = 3,
    //   nLines = 181,
    //   bodySpeed = 0.0,
    //   strokeWeight = 0.95,
    //   minRadius = 0.31701434489957967,
    //   maxRadius = 0.5304896553948168,
    //   opacity = 0.585
    // )

    // Cool spiral
    // val settings = F8Settings(
    //   seed = 630687721682325504,
    //   rand = Random(630687721682325504),
    //   scale = 1.394200475918634,
    //   lineLength = 941,
    //   nBodies = 3,
    //   nLines = 294,
    //   bodySpeed = 01.0,
    //   strokeWeight = 0.95,
    //   minRadius = 0.4268086085861316,
    //   maxRadius = 0.7659692008014354,
    //   opacity = 0.585
    // )

    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    extend(Screenshots()) {
      quitAfterScreenshot = false
      this.scale = 4.0
      captureEveryFrame = false
      name = "screenshots/$progName/${timestamp()}-seed-${settings.seed}.jpg"
    }

    println(settings)
    // burn some Random cycles
    if (settings.needToBurnCycles) {
      random(random = settings.rand); random(random = settings.rand); random(random = settings.rand); random(random = settings.rand); random(random = settings.rand); random(random = settings.rand); random(random = settings.rand)
    }
    val center = Vector2(width / 2.0, height / 2.0)

    val bg = ColorRGBa(0.9, 0.9, 0.9, 1.0)
    val fg = ColorRGBa.BLACK.opacify(settings.opacity)
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
    val colors = listOf(
      rgba(0.10, 0.10, 0.10, settings.opacity),
      rgba(0.05, 0.05, 0.05, settings.opacity),
      rgba(0.20, 0.20, 0.20, settings.opacity),
      rgba(0.40, 0.40, 0.40, settings.opacity),
      rgba(0.10, 0.10, 0.10, settings.opacity),
      rgba(0.25, 0.25, 0.25, settings.opacity),
      rgba(0.07, 0.07, 0.07, settings.opacity)
    )

    val nColors = colors.size
    val angleVariation = PI / nColors.toDouble()
    val colorContours: List<ColorContours> = colors.mapIndexed { index, color ->
      val angle = map(0.0, colors.size.toDouble(), 0.0, 2.0 * PI, index.toDouble())

      // create a "swarm" of contours originating from one spot at the given angle
      val contours = (0 until settings.nLines).map {
        val randomizedAngle = random(angle - angleVariation, angle + angleVariation, settings.rand)
        val randomizedRadius = random(maxRadius * settings.minRadius, maxRadius * settings.maxRadius, settings.rand)
        val body = PhysicalBody(
          center + Vector2(cos(randomizedAngle) * randomizedRadius, sin(randomizedAngle) * randomizedRadius),
          mass = random(205.0, 315.0, settings.rand),
          speed = settings.bodySpeed
        )
        val lineLength = random(settings.lineLength * 0.75, settings.lineLength * 1.25, rand).toInt()
        contour {
          moveTo(body.coords)
          List(lineLength) {
            lineTo(body.orbit(system, settings.scale, center))
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

/**
 * The lesson here is...
 *
 * DON'T FUCK WITH CONCENTRATION GRADIENTS UNLESS YOU KNOW EXACTLY WHAT YOU'RE DOING
 * OR YOU'LL WASTE A BUNCH OF TIME DEBUGGING THINGS THAT DON'T MATTER
 */
package sketch.test

import extensions.CustomScreenshots
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import shape.HatchedShapePacked
import util.RadialConcentrationGradient
import util.intersects
import util.timestamp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1050
    height = 750
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    val screenshots = extend(CustomScreenshots()) {
      quitAfterScreenshot = false
      scale = 3.0
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      captureEveryFrame = false
    }

    val bg = ColorRGBa.BLACK
    val fgBase = ColorRGBa(0.99, 0.99, 0.99, 1.0)
    val fg = fgBase.opacify(0.1)
    backgroundColor = bg

    var failedAttempts = 0

    val planetCircle = Circle(width * 0.5, height * 0.5, width * 0.4)

    val atmosphereCircle = planetCircle.scaled(1.2)
    println(planetCircle)
    println(atmosphereCircle)
    println(hypot(0.8, 0.8))
    println(sqrt(2.0))
    val atmosophereGradient = RadialConcentrationGradient(
      Vector2(0.5, 0.5),
      minRadius = hypot(0.3, 0.3),
      maxRadius = 0.5,
      reverse = true
    )
    println(hypot(0.3, 0.3))
    println(hypot(0.4, 0.4))
    val atmosphereCirclePack = mutableListOf<Circle>()
    val maxFailedAttempts = Short.MAX_VALUE.toInt()
    val radiusRange = 01.40..20.0

    // get that rng
    val rng = Random(seed.toLong())
    extend {

      // println(atmosphereCircle.contour.bounds)
      // Rectangle(corner=Vector2(x=21.0, y=-129.0), width=1008.0, height=1008.0)
      if (failedAttempts < maxFailedAttempts) {
        val angle = random(PI * 0.5, PI * 2.0, rng)
        val distance = random(planetCircle.radius, planetCircle.radius * 1.2, rng)
        val position = Vector2(cos(angle) * distance, sin(angle) * distance) + atmosphereCircle.center
        // endInclusive and start are "reversed" here, because a gradient's lowest concentration maps to 0.0,
        // and that actually correlates to where we want the atmosphereCirclePack to be **most** spaced out.
        // That means we need low concentration to map to high radius, hence the reverse.
        // println(position)
        val radius = map(
          0.0, 1.0,
          radiusRange.endInclusive, radiusRange.start,
          atmosophereGradient.assess(atmosphereCircle.contour.bounds, position)
        )
        val circle = Circle(position, radius)
        // println(circle)

        if (atmosphereCirclePack.any { it.intersects(circle) }) {
          failedAttempts++
          if (failedAttempts % 1000 == 0) {
            println("$failedAttempts of $maxFailedAttempts")
          }
        } else {
          // this is better for some circle packing but it makes this take **forever** and I'm impatient
          // failedAttempts = 0
          atmosphereCirclePack.add(circle)
        }
      }
      val atmosphere = HatchedShapePacked(
        planetCircle.scaled(1.2).contour, rng = rng
      )
        .hatchedShape(
          hatchLength = 20.0,
          strokeWeight = 0.1,
          strokeColor = fg,
          differenceContours = listOf(planetCircle.contour),
          circlePack = atmosphereCirclePack,
        ).second

      drawer.isolated {
        fill = null
        stroke = fgBase
        circle(planetCircle)
        circle(atmosphereCircle)
        circles(atmosphereCirclePack)
      }

      drawer.composition(atmosphere)

      // set seed for next iteration
      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

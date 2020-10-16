/**
 * Layered curl noise,
 * with second-derivative curl
 *
 * This uses the custom screenshots extension which automatically takes pictures every frame.
 * Well, kinda...
 * I actually need to look into why it doesn't.
 * Personally I'd rather have it block the render loop than miss frames, but maybe that's just me
 */
package sketch.flow

import extensions.CustomScreenshots
import noise.perlinCurl
import noise.perlinCurlOfCurl
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.extra.noise.perlin
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.math.mix
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import java.lang.Math.pow
import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

fun main() = application {
  configure {
    width = 800
    height = 1000
  }

  program {
    // wow, sometimes you just gotta read the code! `extend` returns a reference to the extension,
    // which means we can manually trigger it as desired in the event loop!
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    val screenshots = extend(CustomScreenshots()) {
      quitAfterScreenshot = false
      scale = 2.0
      folder = "screenshots/$progName/"
    }

    backgroundColor = ColorRGBa.WHITE

    extend {
      var seed = random(1.0, Int.MAX_VALUE.toDouble()).toLong() // know your seed 😛
      // seed = 1750124224
      val rand = Random(seed)
      println("seed = $seed")

      val stepSize = 120
      val jitter = stepSize * 0.7
      val lineLength = 500
      val opacity = 0.08
      val center = Vector2(width / 2.0, height / 2.0)
      val diagonal = hypot(width.toDouble(), height.toDouble())
      val halfDiagonal = diagonal / 2.0
      val bounds = width / 4

      // generate random noise scales for the three noise "octaves"
      val noiseScales = listOf(
        random(1000.0, 1800.0, rand),
        random(100.0, 400.0, rand),
        random(20.0, 85.0, rand)
      )

      // generate noise influences, which dictate how much each "octave" influences the overall vector field
      val noiseInfluences = listOf(
        Pair(random(0.1, 0.4, rand), random(0.4, 0.95, rand)),
        Pair(random(0.01, 0.4, rand), random(0.4, 0.90, rand)),
        Pair(random(0.01, 0.1, rand), random(0.1, 0.4, rand))
      )

      // each noise "octave" varies spatially according to a noise function - define the scale of that noise map
      val noiseMapScales = listOf(
        random(noiseScales[1], noiseScales[0], rand),
        random(noiseScales[1], noiseScales[0], rand),
        random(noiseScales[1], noiseScales[0], rand)
      )

      val epsilon = random(0.25, 2.0, rand)

      val circleRadius = random(25.0, 150.0, rand)

      fun mixNoise(cursor: Vector2, angle: Double): Vector2 {
        val (scaleOne, scaleTwo, scaleThree) = noiseScales
        val (influenceOne, influenceTwo, influenceThree) = noiseInfluences
        val (mapScaleOne, mapScaleTwo, mapScaleThree) = noiseMapScales

        // scaleOne ratio varies by a simplex noise map
        // val ratioOne = map(
        //   -1.0, 1.0,
        //   influenceOne.first, influenceOne.second,
        //   simplex(seed.toInt(), cursor / mapScaleOne)
        // )
        val ratioOne = mix(influenceOne.second, influenceOne.first, cursor.distanceTo(center) / halfDiagonal)

        // scaleTwo ratio varies by a different simplex noise map
        // val ratioTwo = map(
        //   -1.0, 1.0,
        //   influenceTwo.first, influenceTwo.second,
        //   pow(perlin(seed.toInt(), cursor / mapScaleTwo), 2.0)
        // )
        val ratioTwo = mix(influenceTwo.first, influenceTwo.second, cursor.distanceTo(center) / halfDiagonal)

        // scaleThree ratio varies by distance from center
        // val ratioThree = map(
        //   0.0, sqrt(halfDiagonal),
        //   influenceThree.first, influenceThree.second,
        //   sqrt(cursor.distanceTo(center))
        // )
        val ratioThree = influenceThree.second

        // layer curl noise together, with primary angle influence diminishing with length
        // val ratioAngle = map(0.0, 1.0, 0.4, 0.005, i / lineLength.toDouble())
        // TODO: make custom lambda that mixes the result of perlin (mapped to radian) with the angle and performs curl on that
        val ratioAngle = map(0.0, halfDiagonal, 0.4, 0.005, cursor.distanceTo(center))
        val res = Vector2(cos(angle), sin(angle)) * ratioAngle +
          perlinCurl(seed.toInt(), cursor / scaleOne, epsilon) * ratioOne +
          perlinCurl(seed.toInt(), cursor / scaleTwo, epsilon) * ratioTwo +
          perlinCurl(seed.toInt(), cursor / scaleThree, epsilon) * ratioThree

        return res.normalized
      }

      val contours: List<ShapeContour> = (0 until (360 * stepSize)).map { degree ->
        val angle = toRadians(degree.toDouble() / stepSize.toDouble() + random(-jitter, jitter, rand))
        val radius = map(-1.0, 1.0, circleRadius, halfDiagonal, random(-1.0, 1.0, rand))
        contour {
          moveTo(
            cos(angle) * radius + center.x,
            sin(angle) * radius + center.y
          )

          List(lineLength) {
            lineTo(cursor + mixNoise(cursor, angle))
          }
        }
      }

      println("aww yeah, about to render...")
      drawer.fill = null
      drawer.stroke = null // overwritten below
      drawer.strokeWeight = 1.0
      drawer.lineCap = LineCap.ROUND

      // simple B&W
      drawer.stroke = ColorRGBa.BLACK.opacify(opacity)
      contours.chunked(500).forEach { drawer.contours(it) }

      drawer.fill = ColorRGBa.WHITE
      drawer.stroke = ColorRGBa.BLACK
      drawer.circle(center, circleRadius * 1.1)

      // trigger screenshot on every frame with seed appended to file name
      screenshots.trigger("seed-$seed")
    }
  }
}

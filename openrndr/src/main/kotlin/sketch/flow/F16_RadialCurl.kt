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
import noise.curlOfCurl
import noise.mapToRadians
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.extra.noise.perlin
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.math.mix
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import java.lang.Math.toRadians
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
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
      captureEveryFrame = true
    }

    // Static drawing properties
    val stepSize = 200
    val jitter = stepSize * 0.7
    val lineLength = 500
    val opacity = 0.06
    val strokeWeight = 1.0
    val center = Vector2(width / 2.0, height / 2.0)
    val diagonal = hypot(width.toDouble(), height.toDouble())
    val halfDiagonal = diagonal / 2.0
    val fg = ColorRGBa.BLACK.opacify(opacity) // foreground
    val bg = ColorRGBa.WHITE // background
    // INVERTED! (actually pretty trippy, kinda cool)
    // val fg = ColorRGBa(0.99, 0.99, 0.99, opacity)
    // val bg = ColorRGBa.BLACK
    backgroundColor = bg

    // Seed must be set before the loop, and at the end of the loop,
    // to be able to set screenshots.append correctly
    var seed = random(1.0, Int.MAX_VALUE.toDouble()).toLong() // know your seed 😛
    // seed = 1750124224
    // seed = 129481721
    // seed = 862483012
    seed = 700454207 // <-- this one is the best lol
    // seed = 1167433601
    // seed = 669983955
    println("seed = $seed")
    screenshots.append = "seed-$seed"

    extend {
      val rand = Random(seed)

      // generate random noise scales for the three noise "octaves"
      val scaleOne = random(1000.0, 1800.0, rand)
      val scaleTwo = random(100.0, 400.0, rand)
      val scaleThree = random(20.0, 85.0, rand)

      // generate noise influences, which dictate how much each "octave" influences the overall vector field
      val influenceOne = Pair(random(0.1, 0.4, rand), random(0.4, 0.95, rand))
      val influenceTwo = Pair(random(0.01, 0.4, rand), random(0.4, 0.90, rand))
      val influenceThree = Pair(random(0.01, 0.1, rand), random(0.1, 0.4, rand))

      // other mildly-randomized properties
      val epsilon = random(0.25, 2.0, rand)
      val circleRadius = random(100.0, 200.0, rand)

      /**
       * mixNoise1 creates three "octaves" of curl noise
       * and mixes them together based on the cursor position.
       * The noise is mixed with a straight vector pointing in the direction of the dominant "angle"
       * for the cursor. As the cursor moves away from the center, the mix of noise to straight angle changes.
       */
      fun mixNoise1(cursor: Vector2, angle: Double): Vector2 {
        val distPct = cursor.distanceTo(center) / halfDiagonal
        // ratios vary by distance from center
        val ratioOne = mix(influenceOne.second, influenceOne.first, distPct)
        val ratioTwo = mix(influenceTwo.first, influenceTwo.second, distPct)
        val ratioThree = influenceThree.second

        // define a lambda for our noise function,
        // which blends together a perlin field mapped to radians with
        // our angle for this line.
        // The `mix` could use some more experimentation
        val curlFunc = { i: Int, x: Double, y: Double ->
          mix(
            mapToRadians(-1.0, 1.0, perlin(i, x, y)),
            angle,
            0.75
          )
        }

        // layer curl noise together, with primary angle influence diminishing with length
        val ratioAngle = mix(0.4, 0.005, distPct)
        val res = Vector2(cos(angle), sin(angle)) * ratioAngle +
          curlOfCurl(curlFunc, seed.toInt(), cursor / scaleOne, epsilon) * ratioOne +
          curlOfCurl(curlFunc, seed.toInt(), cursor / scaleTwo, epsilon) * ratioTwo +
          curlOfCurl(curlFunc, seed.toInt(), cursor / scaleThree, epsilon) * ratioThree

        return res.normalized
      }

      /**
       * mixNoise2 uses a different mixing strategy than mixNoise1 and I couldn't quickly figure
       * out a good way to parameterize it so it's just a new function
       */
      fun mixNoise2(cursor: Vector2, angle: Double): Vector2 {
        val distPct = map(circleRadius, 0.1, 0.0, 1.0, cursor.distanceTo(center))
        val ratioTwo = influenceTwo.second * 1.5
        val ratioThree = influenceThree.second * 3.0

        // define a lambda for our noise function,
        // which blends together a perlin field mapped to radians with
        // our angle for this line.
        val curlFunc = { i: Int, x: Double, y: Double ->
          mix(
            mapToRadians(-1.0, 1.0, perlin(i, x, y)),
            angle,
            0.25
          )
        }

        // layer curl noise together, with primary angle influence diminishing with length
        val ratioAngle = mix(0.4, 0.0, distPct)
        val res = Vector2(cos(angle), sin(angle)) * ratioAngle +
          curlOfCurl(curlFunc, seed.toInt(), cursor / scaleTwo, 0.5) * ratioTwo +
          curlOfCurl(curlFunc, seed.toInt(), cursor / scaleThree, 0.5) * ratioThree

        return res.normalized
      }

      /******************************
       *
       * Define interior and exterior contours
       *
       *******************************/
      val exteriorContours: List<ShapeContour> = (0 until (360 * stepSize)).map { degree ->
        val angle = toRadians(degree.toDouble() / stepSize.toDouble() + random(-jitter, jitter, rand))
        val radius = map(-1.0, 1.0, circleRadius, halfDiagonal, random(-1.0, 1.0, rand))
        contour {
          moveTo(
            cos(angle) * radius + center.x,
            sin(angle) * radius + center.y
          )

          List(lineLength) {
            lineTo(cursor + mixNoise1(cursor, angle))
          }
        }
      }

      val interiorStepSize = stepSize / 4
      val interiorContours: List<ShapeContour> = (0 until (360 * interiorStepSize)).map { degree ->
        val angle = toRadians(degree.toDouble() / interiorStepSize.toDouble() + random(-jitter, jitter, rand))
        val radius = map(-1.0, 1.0, circleRadius, 1.0, random(-1.0, 1.0, rand))
        contour {
          moveTo(
            cos(angle) * radius + center.x,
            sin(angle) * radius + center.y
          )

          List(lineLength / 10) {
            // angle points inwards, hence "- PI"
            lineTo(cursor + mixNoise2(cursor, angle - PI))
          }
        }
      }

      println("""aww yeah, about to render...
        | scaleOne = $scaleOne
        | scaleTwo = $scaleTwo
        | scaleThree = $scaleThree
        | influenceOne = ${influenceOne.first}, ${influenceOne.second}
        | influenceTwo = ${influenceTwo.first}, ${influenceTwo.second}
        | influenceThree = ${influenceThree.first}, ${influenceThree.second}
      """.trimMargin())

      /******************************
       *
       * Drawing
       *
       *******************************/
      drawer.fill = null
      drawer.lineCap = LineCap.ROUND

      // interior contours
      drawer.strokeWeight = strokeWeight
      drawer.stroke = fg
      interiorContours.chunked(500).forEach { drawer.contours(it) }

      // slightly hacky way of drawing a "boundary" around the interior contours
      drawer.stroke = bg
      drawer.strokeWeight = halfDiagonal
      drawer.circle(center, circleRadius + halfDiagonal)

      // exterior contours
      drawer.strokeWeight = strokeWeight
      drawer.stroke = fg
      exteriorContours.chunked(500).forEach { drawer.contours(it) }

      // set seed for next iteration
      seed = random(1.0, Int.MAX_VALUE.toDouble()).toLong()
      println("seed = $seed")
      screenshots.append = "seed-$seed"
    }
  }
}

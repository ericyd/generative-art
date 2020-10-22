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
import noise.curl
import noise.curlOfCurl
import noise.mapToRadians
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.draw.isolated
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
    width = 1000
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
    val stepMultiplier = 100
    val jitter = stepMultiplier * 0.7
    val lineLength = 500
    val opacity = 0.15
    val strokeWeight = 1.0
    val origin = Vector2(width / 2.0, height * 1.025)
    val diagonal = hypot(width.toDouble(), height.toDouble())

    val black = ColorRGBa.BLACK
    val white = ColorRGBa(0.99, 0.99, 0.99, 1.0) // true white doesn't work with opacity, probably a bug
    val fg = black.opacify(opacity)
    val bg = white
    val fgInverted = white.opacify(opacity)
    // INVERTED! (actually pretty trippy, kinda cool)
    // val fg = white.opacify(opacity)
    // val bg = black
    // val fgInverted = black.opacify(opacity)
    backgroundColor = bg

    // Seed must be set before the loop, and at the end of the loop,
    // to be able to set screenshots.append correctly
    var seed = random(1.0, Int.MAX_VALUE.toDouble()).toLong() // know your seed 😛
    // seed = 150390157
    println("seed = $seed")
    screenshots.append = "seed-$seed"

    extend {
      val rand = Random(seed)

      // generate random noise scales for the three noise "octaves"
      val scaleOne = random(100.0, 400.0, rand)
      val scaleTwo = random(20.0, 550.0, rand)

      // generate noise influences, which dictate how much each "octave" influences the overall vector field
      val influenceOne = Pair(random(0.1, 0.4, rand), random(0.4, 0.95, rand))
      val influenceTwo = Pair(random(0.01, 0.3, rand), random(0.3, 0.60, rand))

      // other mildly-randomized properties
      val epsilon = random(0.25, 2.0, rand)
      val circleRadius = random(width / 3.0, width / 1.8, rand)

      /**
       * mixNoise1 creates three "octaves" of curl noise
       * and mixes them together based on the cursor position.
       * The noise is mixed with a straight vector pointing in the direction of the dominant "angle"
       * for the cursor. As the cursor moves away from the center, the mix of noise to straight angle changes.
       */
      fun mixNoise1(cursor: Vector2, angle: Double): Vector2 {
        val distPct = cursor.distanceTo(origin) / diagonal
        // ratios vary by distance from center
        val ratioOne = mix(influenceOne.second, influenceOne.first, distPct)
        val ratioTwo = mix(influenceTwo.first, influenceTwo.second, distPct)

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
          curl(curlFunc, seed.toInt(), cursor / scaleOne, epsilon) * ratioOne +
          curl(curlFunc, seed.toInt(), cursor / scaleTwo, epsilon) * ratioTwo

        return res.normalized
      }

      /**
       * mixNoise2 uses a different mixing strategy than mixNoise1 and I couldn't quickly figure
       * out a good way to parameterize it so it's just a new function
       */
      fun mixNoise2(cursor: Vector2, angle: Double): Vector2 {
        val distPct = map(circleRadius, 0.1, 0.0, 1.0, cursor.distanceTo(origin))
        val ratioOne = influenceOne.second * 1.5
        val ratioTwo = influenceTwo.second * 3.0

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
          curl(curlFunc, seed.toInt(), cursor / scaleOne, 0.5) * ratioOne +
          curl(curlFunc, seed.toInt(), cursor / scaleTwo, 0.5) * ratioTwo

        return res.normalized
      }

      /******************************
       *
       * Define interior and exterior contours
       *
       *******************************/
      val exteriorContours: List<ShapeContour> = (0 until (360 * stepMultiplier)).map { degree ->
        val angle = toRadians(degree.toDouble() / stepMultiplier.toDouble() + random(-jitter, jitter, rand))
        val radius = map(-1.0, 1.0, circleRadius, diagonal, random(-1.0, 1.0, rand))
        contour {
          moveTo(
            cos(angle) * radius + origin.x,
            sin(angle) * radius + origin.y
          )

          List(lineLength) {
            lineTo(cursor + mixNoise1(cursor, angle))
          }
        }
      }

      val interiorStepMultiplier = stepMultiplier * 3
      val interiorContours: List<ShapeContour> = (0 until (360 * interiorStepMultiplier)).map { degree ->
        val angle = toRadians(degree.toDouble() / interiorStepMultiplier.toDouble() + random(-jitter, jitter, rand))
        val radius = map(-1.0, 1.0, circleRadius * 1.15, 1.0, random(-1.0, 1.0, rand))
        contour {
          moveTo(
            cos(angle) * radius + origin.x,
            sin(angle) * radius + origin.y
          )

          List(lineLength / 20) {
            // angle points inwards, hence "- PI"
            lineTo(cursor + mixNoise2(cursor, angle - PI))
          }
        }
      }

      println("""aww yeah, about to render...
        | scaleOne = $scaleOne
        | scaleTwo = $scaleTwo
        | influenceOne = ${influenceOne.first}, ${influenceOne.second}
        | influenceTwo = ${influenceTwo.first}, ${influenceTwo.second}
      """.trimMargin())

      /******************************
       *
       * Drawing
       *
       *******************************/
      drawer.clear(bg)
      drawer.fill = null
      drawer.lineCap = LineCap.ROUND
      drawer.stroke = fg

      // interior contours
      drawer.isolated {
        this.fill = black
        this.circle(origin, circleRadius)
        this.strokeWeight = strokeWeight
        this.stroke = fgInverted
        interiorContours.chunked(500).forEach { this.contours(it) }
      }

      // slightly hacky way of drawing a "boundary" around the interior contours
      drawer.isolated {
        this.stroke = bg
        this.strokeWeight = diagonal
        this.circle(origin, circleRadius + diagonal)
      }

      // exterior contours
      exteriorContours.chunked(500).forEach { drawer.contours(it) }

      // set seed for next iteration
      seed = random(1.0, Int.MAX_VALUE.toDouble()).toLong()
      println("seed = $seed")
      screenshots.append = "seed-$seed"
    }
  }
}

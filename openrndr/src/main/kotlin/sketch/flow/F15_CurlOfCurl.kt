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
import noise.perlinCurlOfCurl
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.extra.noise.perlin
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import java.lang.Math.pow
import kotlin.math.hypot
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
      // these three seeds are the ones posted on the gram
      // seed = 189526243
      // seed = 56708742
      // seed = 987928529
      val rand = Random(seed)
      println("seed = $seed")

      val stepSize = 5
      val jitter = stepSize * 0.7
      val lineLength = 300
      val opacity = 0.12
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

      val chance = random(0.0, 1.0, rand) < 0.5

      fun mixNoise(cursor: Vector2): Vector2 {
        val (scaleOne, scaleTwo, scaleThree) = noiseScales
        val (influenceOne, influenceTwo, influenceThree) = noiseInfluences
        val (mapScaleOne, mapScaleTwo, mapScaleThree) = noiseMapScales

        // scaleOne ratio varies by a simplex noise map
        val ratioOne = map(
          -1.0, 1.0,
          influenceOne.first, influenceOne.second,
          simplex(seed.toInt(), cursor / mapScaleOne)
        )

        // scaleTwo ratio varies by distance from center
        val ratioTwo = map(
          0.0, pow(halfDiagonal, 2.0),
          influenceTwo.first, influenceTwo.second,
          cursor.squaredDistanceTo(center)
        )

        // scaleThree ratio varies by a different simplex noise map
        // and possibly y position
        val yScalePct = if (chance) 1.0 else cursor.y / height.toDouble()
        val ratioThree = map(
          -1.0, 1.0,
          influenceThree.first, influenceThree.second,
          pow(perlin(seed.toInt(), cursor / mapScaleThree), 2.0)
        ) * yScalePct

        // layer scaleTwo curl noises together, creating a sort of "scaleOne" and "scaleTwo" flow pattern
        val res = perlinCurlOfCurl(seed.toInt(), cursor / scaleOne) * ratioOne +
          perlinCurlOfCurl(seed.toInt(), cursor / scaleTwo) * ratioTwo +
          perlinCurlOfCurl(seed.toInt(), cursor / scaleThree) * ratioThree

        return res.normalized
      }

      val contours: List<ShapeContour> = ((0 - bounds) until (width + bounds) step stepSize).flatMap { x ->
        ((0 - bounds) until (height + bounds) step stepSize).map { y ->
          contour {
            moveTo(
              x + random(-jitter, jitter, rand),
              y + random(-jitter, jitter, rand)
            )

            List(lineLength) {
              lineTo(cursor + mixNoise(cursor))
            }
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

      // trigger screenshot on every frame with seed appended to file name
      screenshots.trigger("seed-$seed")
    }
  }
}

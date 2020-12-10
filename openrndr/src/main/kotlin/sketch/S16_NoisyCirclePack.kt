/**
 * Based on
 * http://www.codeplastic.com/2017/09/09/controlled-circle-packing-with-processing/
 */
package sketch

import extensions.CustomScreenshots
import force.MovingBody
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.extras.color.palettes.colorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import util.PackCompleteResult
import util.generateMovingBodies
import util.packCirclesControlled
import util.timestamp
import kotlin.math.abs
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 1000
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

    val radiusRange = 0.10 to 40.0
    // Some very interesting things can happen if you don't honor the actual simplex range of [-1.0, 1.0]
    val noiseRange = -0.5 to 0.5
    val noiseScale = 200.0

    val spectrum = colorSequence(
      0.0 to
        ColorRGBa.fromHex("9AC0C1"), // greenish blue
      0.25 to
        ColorRGBa.fromHex("E7CD7E"), // light yellow
      0.5 to
        ColorRGBa.fromHex("5A7687"), // dark gray-blue
      0.75 to
        ColorRGBa.fromHex("C8BAC9"), // light purple
      1.0 to
        ColorRGBa.fromHex("5E5978"), // dark purple
    )

    var packed = PackCompleteResult(generateMovingBodies(800, Vector2(width * 0.5, height * 0.5), 10.0))
    extend {
      // get that rng
      val rng = Random(seed.toLong())

      val sizeFn = { body: MovingBody ->
        val targetRadius = map(
          noiseRange.first, noiseRange.second,
          radiusRange.first, radiusRange.second,
          simplex(seed, body.position / noiseScale)
        )
        // technically the value will never "arrive" but it gets very close very fast so, y'know ... good enough for me!
        body.radius = abs(body.radius + targetRadius) / 2.0
      }

      packed = packCirclesControlled(bodies = packed.bodies, incremental = true, rng = rng, sizeFn = sizeFn)
      packed.bodies.forEachIndexed { index, body ->
        val shade = simplex(seed, body.position.yx / noiseScale) * 0.5 + 0.5
        drawer.stroke = spectrum.index(shade)
        drawer.fill = spectrum.index(shade).opacify(simplex(seed, body.position.xy0 / noiseScale) * 0.25 + 0.75)
        drawer.circle(Circle(body.position, body.radius))
      }

      // set seed for next iteration
      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

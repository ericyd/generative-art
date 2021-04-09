/**
 * More custom noise explorations
 */
package sketch.flow

import extensions.CustomScreenshots
import noise.perlinCurl
import noise.simplexCurl
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.extra.noise.perlin
import org.openrndr.extra.noise.perlinHermite
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.math.mix
import org.openrndr.shape.contour
import util.MixNoise
import util.MixableNoise
import util.timestamp
import java.lang.Math.pow
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 800
    height = 1000
  }

  // Define mixable noise function
  // TODO: run some tests on mixable noise to figure out what these artifacts are all about
  // seed = 1512392881
  fun generateMixable(width: Double, seed: Int, rng: Random): MixNoise {
    val scale1 = width * random(0.8, 1.05, rng)
    val epsilon1 = random(0.01, 0.5, rng)
    val noiseFn1 = { v: Vector2 -> simplexCurl(seed, v, epsilon1) }
    val noise1 = MixableNoise(
      scale = scale1,
      influenceScale = scale1 * 0.5,
      influenceRange = 0.1 to 0.75,
      noiseFn = noiseFn1,
      influenceNoiseFn = { v: Vector2 -> simplex(seed, v * 2.0) },
    )

    val scale2 = width * random(0.2, 0.4, rng)
    val offset2 = random(0.0, 2.0 * PI, rng)
    val noiseFn2 = { v: Vector2 ->
      val angle = map(-1.0, 1.0, -PI + offset2, PI + offset2, perlinHermite(seed, v.x, v.y, atan2(v.y, v.x) / scale2))
      Vector2(cos(angle), sin(angle))
    }
    val noise2 = MixableNoise(
      scale = scale2,
      noiseFn = noiseFn2,
      influenceRange = 0.2 to 0.75,
      influenceScale = scale2 * random(0.05, 0.8, rng),
      influenceNoiseFn = { v: Vector2 -> simplex(seed, v).pow(2.0) },
      influenceNoiseFnRange = 0.0 to 1.0
      // influenceNoiseFn = { v: Vector2 -> simplex(seed, v) },
      // influenceNoiseFnRange = -1.0 to 1.0
    )

    val scale3 = width * random(0.035, 0.1, rng)
    val influenceScale3 = width * random(0.05, 0.8, rng)
    val epsilon3 = random(0.01, 0.5, rng)
    val chance3 = random(0.0, 1.0, rng) < 0.5
    val offset3 = random(0.0, 2.0 * PI, rng)
    val noiseFn3 = if (chance3) {
      { v: Vector2 ->
        val angle = map(-1.0, 1.0, -PI + offset3, PI + offset3, simplex(seed, v))
        Vector2(cos(angle), sin(angle))
      }
    } else {
      { v: Vector2 -> perlinCurl(seed, v, epsilon3) }
    }
    val noise3 = MixableNoise(
      scale = scale3,
      noiseFn = noiseFn3,
      influenceRange = 0.1 to 0.75,
      influenceScale = influenceScale3,
      influenceNoiseFn = { v: Vector2 -> perlin(seed, v).pow(2.0) },
      influenceNoiseFnRange = 0.0 to 1.0
    )

    // println("""
    //   scale1: $scale1
    //   epsilon1: $epsilon1
    //   scale2: $scale2
    //   scale3: $scale3
    //   epsilon3: $epsilon3
    //   influenceScale3: $influenceScale3
    //   chance3: $chance3
    // """)

    return MixNoise(listOf(noise1, noise2, noise3))
  }

  program {
    var seed = random(1.0, Long.MAX_VALUE.toDouble()).toInt() // know your seed 😛

    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    val screenshots = extend(CustomScreenshots()) {
      quitAfterScreenshot = false
      scale = 3.0
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      captureEveryFrame = false
    }

    backgroundColor = ColorRGBa.WHITE

    // nice for "final"
    // val stepSize = 5
    // val jitter = stepSize * 0.7
    // val lineLength = 500
    // nice for "prototype"
    val stepSize = 10
    val jitter = stepSize * 0.7
    val lineLength = 250

    val opacity = 0.05
    val center = Vector2(width / 2.0, height / 2.0)
    val halfDiagonal = hypot(width / 2.0, height / 2.0)
    val bounds = width / 2

    extend {
      println("seed = $seed")
      val rng = Random(seed)

      val mixable = generateMixable(width.toDouble(), seed, rng)

      drawer.fill = null
      drawer.stroke = null // overwritten below
      drawer.strokeWeight = 1.0
      drawer.lineCap = LineCap.ROUND

      // simple B&W
      drawer.stroke = ColorRGBa.BLACK.opacify(opacity)

      for (x in (0 - bounds) until (width + bounds) step stepSize) {
        for (y in (0 - bounds) until (height + bounds) step stepSize) {
          val c = contour {
            moveTo(
              x + random(-jitter, jitter, rng),
              y + random(-jitter, jitter, rng)
            )

            List(lineLength) {
              lineTo(cursor + mixable.mix(cursor))
            }
          }
          drawer.contour(c)
        }
      }

      if (true || screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

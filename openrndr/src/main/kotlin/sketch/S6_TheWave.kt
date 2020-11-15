/**
 * Inspired by The Wave
 * http://grandcanyoncollective.com/2017/12/09/the-wave/
 *
 * But it turned out nothing like the inspiration!
 *
 * In fact, I just recreated (yet again), this algorithm:
 * https://dangries.com/rectangleworld/demos/MorphingCurve/MorphingCurve_RadialGradient.html
 */
package sketch

import extensions.CustomScreenshots
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.perlin
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.ShapeContour
import shape.SimplexBlob
import shape.vectorListToSegments
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 750
    height = 750
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    val screenshots = extend(CustomScreenshots()) {
      quitAfterScreenshot = false
      scale = 3.0
      folder = "screenshots/$progName/"
      captureEveryFrame = true
    }

    var seed = random(1.0, Int.MAX_VALUE.toDouble()).toInt()
    // seed = 1587814064
    println(
      """
        seed = $seed
      """.trimIndent()
    )

    val fg = ColorRGBa.BLACK.opacify(0.25)
    val bg = ColorRGBa.WHITE
    val opacity = 0.2
    backgroundColor = bg

    extend {
      // Where would we be, without randomness?
      val rng = Random(seed.toLong())

      // where would we be, without blob-iness?
      val blob = SimplexBlob(
        origin = Vector2(width / 2.0, height / 2.0),
        seed = seed,
        moreConvexPlz = random(-1.0, 1.0, rng) < 0.0,
        resolution = random(200.0, 1000.0, rng).toInt()
      )

      // where would we be, without ... ok shut up eric
      val phase0 = random(25.0, 140.0, rng)
      val amp0 = random(55.0, 115.0, rng)
      val path = (-200 until height + 200).map { i ->
        // horizontal sine wave
        // Vector2(i.toDouble(), height.toDouble() / 2.0 + sin(i.toDouble() / phase0) * amp0)

        // vertical sine wave
        Vector2(width / 2.0 + sin(i.toDouble() / phase0) * amp0, i.toDouble())

        // hug the baseboard
        // Vector2(i.toDouble(), height.toDouble())
      }
      val PATH_LEN = path.size.toDouble()

      // it's possible I lean a bit **too** heavily on randomness
      val radiusRange = Pair(10.0, random(150.0, 300.0, rng))
      val noiseScaleRange = Pair(0.50, random(0.95, 1.7, rng))
      val fuzzinessRange = Pair(0.0, random(0.0, 4.0, rng))
      val aspecRatioRange = Pair(random(0.6, 1.0, rng), random(1.0, 1.3, rng))
      val rotationRange = Pair(random(-4.0 * PI, 0.0, rng), random(0.0, 4.0 * PI, rng))

      val phase1 = random(175.0, 225.0, rng)
      val phase2 = random(135.0, 155.0, rng)
      val phase3 = random(95.0, 255.0, rng)
      val phase4 = random(25.0, 235.0, rng)

      val blobs = path.mapIndexed { index, origin ->
        val perlinPhase1 = perlin(seed, origin / phase1)
        val perlinPhase2 = perlin(seed, origin / phase2)
        val perlinPhase3 = perlin(seed, origin / phase3)
        val perlinPhase4 = perlin(seed, origin / phase4)
        blob.origin = origin
        blob.radius = map(-1.0, 1.0, radiusRange.first, radiusRange.second, perlinPhase1)
        blob.noiseScale = map(-1.0, 1.0, noiseScaleRange.first, noiseScaleRange.second, perlinPhase2)
        blob.rotation = map(0.0, 1.0, rotationRange.first, rotationRange.second, index / PATH_LEN)
        // fuzziness... meh
        // blob.fuzziness = map(-1.0, 1.0, fuzzinessRange.first, fuzzinessRange.second, perlinPhase2)
        blob.ridgediness = map(-1.0, 1.0, fuzzinessRange.first, fuzzinessRange.second, perlinPhase3)
        blob.aspectRatio = map(-1.0, 1.0, aspecRatioRange.first, aspecRatioRange.second, perlinPhase4)
        blob.shape()
      }

      val usePerpendiculars = random(-1.0, 1.0, rng) < 0.0
      // I wasn't a big fan of how this turned out, :shrug:
      val perpendiculars: List<ShapeContour> = blobs.first().segments.mapIndexed { segmentIndex, segment ->
        blobs.map { it.segments.get(segmentIndex).start }
      }.map { ShapeContour(vectorListToSegments(it), closed = false) }

      drawer.fill = null
      drawer.stroke = fg
      drawer.strokeWeight = 1.0
      path.forEachIndexed { index, p ->
        val shade = map(-1.0, 1.0, 0.0, 0.999, perlin(seed, p))
        val blobColor = ColorRGBa(shade, shade, shade, random(opacity * 0.15, opacity * 5.0, rng))
        drawer.stroke = blobColor

        // uncomment line below if you're a sucker
        // drawer.fill = blobColor.opacify(0.2)

        // use this if you like the perpendiculars look
        if (usePerpendiculars) {
          // y'know, sometimes a nested if just makes sense so SUCK IT
          if (index < perpendiculars.lastIndex) {
            drawer.contour(perpendiculars[index])
          }
        } else {
          drawer.contour(blobs[index])
        }
      }

      // set seed for next iteration
      seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
      screenshots.append = "seed-$seed"
    }
  }
}

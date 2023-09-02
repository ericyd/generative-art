/**
 * Inspired by The Wave
 * http://grandcanyoncollective.com/2017/12/09/the-wave/
 *
 * Getting a bit closer this time!!
 */
package sketch

import extensions.CustomScreenshots
import frames.circularFrame
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.hsla
import org.openrndr.extra.noise.perlin
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.color.palettes.colorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.map
import shape.SimplexBlob
import kotlin.math.PI
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

    var bg = ColorRGBa.WHITE
    backgroundColor = bg

    // colorSpectrum is an absolutely brilliant addition from orx-color

    // earthy red-brown-oranges
    // val spectrum = colorSequence(
    //   0.0 to ColorRGBa.fromHex("9F6B04"),
    //   0.2 to ColorRGBa.fromHex("F59700"),
    //   0.4 to ColorRGBa.fromHex("85410A"),
    //   0.6 to ColorRGBa.fromHex("E04300"),
    //   0.8 to ColorRGBa.fromHex("839791"),
    //   1.0 to ColorRGBa.fromHex("F98C77")
    // )

    // my fav spectrum from my "orbit" series
    // Note: opacity is very important for these colors, they look horrible at full opacity
    val opacity = 0.2
    val spectrum = colorSequence(
      0.0 to
        hsla(255.0, 0.46, 0.86, opacity), // light purple
      0.166 to
        hsla(212.0, 0.67, 0.30, opacity), // dark blue
      0.332 to
        hsla(261.0, 0.45, 0.43, opacity), // purple
      0.498 to
        hsla(29.0, 0.93, 0.83, opacity), // orange/salmon
      0.664 to
        hsla(10.0, 0.40, 0.15, opacity), // dark brown
      0.83 to
        hsla(194.0, 0.70, 0.85, opacity), // light blue
      1.0 to
        hsla(173.0, 0.66, 0.975, opacity) // smokey white
    )

    // From my "dreamscape" curl piece
    // val spectrum = colorSequence(
    //   0.0 to hsla(342.0, 0.50, 0.53, 0.8), // purple-ish
    //   0.5 to hsla(25.0, 0.50, 0.63, 0.8), // orange-ish
    //   1.0 to hsla(215.0, 0.52, 0.58, 0.8) // blue-ish
    // )

    // blue spectrum
    // val spectrum = colorSequence(
    //   0.0 to ColorRGBa.fromHex("a4c0fc"), // light pale blue
    //   0.5 to ColorRGBa.fromHex("2c4f99"), // dark blue
    //   1.0 to ColorRGBa.fromHex("653c85") // dark purple
    // )

    // reddish spectrum
    // val spectrum = colorSequence(
    //   0.0 to ColorRGBa.fromHex("fcc097"), // light orange red
    //   0.5 to ColorRGBa.fromHex("a63d2b"), // dark clay red
    //   1.0 to ColorRGBa.fromHex("ffb459") // light orange
    // )

    // simple purple spectrum
    // val spectrum = colorSequence(
    //   0.0 to ColorRGBa.fromHex("593566"), // dark purple
    //   1.0 to ColorRGBa.fromHex("f7d7fc") // light purple
    // )

    extend {
      // get that rng
      val rng = Random(seed.toLong())

      // initialize the blob
      val blob = SimplexBlob(
        origin = Vector2(width / 2.0, height / 2.0),
        seed = seed,
        moreConvexPlz = random(-1.0, 1.0, rng) < 0.0,
        resolution = 1500
      )

      // angle from bottom right to top left
      val path = List(height * 2) { i ->
        Vector2(
          map(0.0, height * 2.0, width.toDouble(), width * -1.5, i.toDouble()),
          map(0.0, height * 2.0, height * 2.0, height * -1.0, i.toDouble())
        )
      }

      val PATH_LEN = path.size.toDouble()

      // it's possible I lean a bit **too** heavily on randomness
      val noiseScaleRange = Pair(0.50, random(0.95, 1.2, rng))
      val fuzzinessRange = Pair(0.0, random(0.0, 2.5, rng))
      val aspecRatioRange = Pair(random(0.6, 1.0, rng), random(1.0, 1.4, rng))
      val rotationRange = Pair(random(-1.5 * PI, 0.0, rng), random(0.0, 1.5 * PI, rng))
      val radiusRange = Pair(800.0, 1100.0)

      val phase1 = random(275.0, 325.0, rng)
      val phase2 = random(235.0, 255.0, rng)
      val phase3 = random(295.0, 355.0, rng)
      val phase4 = random(225.0, 335.0, rng)

      val blobs = path.mapIndexed { index, origin ->
        val perlinPhase1 = perlin(seed, origin / phase1)
        val perlinPhase2 = perlin(seed, origin / phase2)
        val perlinPhase3 = perlin(seed, origin / phase3)
        val perlinPhase4 = perlin(seed, origin / phase4)
        blob.origin = origin
        blob.radius = map(-1.0, 1.0, radiusRange.first, radiusRange.second, perlinPhase1)
        // blob.radius = map(0.0, 1.0, radiusRange.first, radiusRange.second, index / PATH_LEN)
        blob.noiseScale = map(-1.0, 1.0, noiseScaleRange.first, noiseScaleRange.second, perlinPhase2)
        // blob.noiseScale = map(0.0, 1.0, noiseScaleRange.first, noiseScaleRange.second, index / PATH_LEN)
        blob.rotation = map(0.0, 1.0, rotationRange.first, rotationRange.second, index / PATH_LEN)
        // fuzziness... meh
        // blob.fuzziness = map(-1.0, 1.0, fuzzinessRange.first, fuzzinessRange.second, perlinPhase2)
        blob.ridgediness = map(-1.0, 1.0, fuzzinessRange.first, fuzzinessRange.second, perlinPhase3)
        blob.aspectRatio = map(-1.0, 1.0, aspecRatioRange.first, aspecRatioRange.second, perlinPhase4)
        blob.contour()
      }

      drawer.strokeWeight = 1.0
      path.forEachIndexed { index, p ->
        val shade = map(-1.0, 1.0, 0.0, 1.0, simplex(seed, index.toDouble()))
        val blobColor = spectrum.index(shade)
        drawer.stroke = blobColor
        drawer.fill = blobColor
        drawer.contour(blobs[index])
      }

      circularFrame(width, height, drawer, ColorRGBa.WHITE)

      // set seed for next iteration
      seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
      screenshots.append = "seed-$seed"
    }
  }
}

/**
 * Circle in the middle
 *
 * features
 * ✅ 1. flow lines based on sine waves with noise added in
 * ✅ 2. when flow lines cross barrier into circle in the middle, noise scale goes wild. hella noisy
 * ✅ 3. perlin blobs along the top and bottom. Density decreases towards center. Look like paint splotches thrown on canvas
 * TODO: 4. very faint lines in background, similar to tyler hobbes' recent drawing. vertical lines, short, in horizontal rows.
 */
package sketch

import extensions.CustomScreenshots
import noise.curl
import noise.curlOfCurl
import noise.mapToRadians
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.perlin
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.math.mix
import org.openrndr.shape.Circle
import org.openrndr.shape.contour
import shape.SimplexBlob
import util.grid
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 750
  }

  program {
    // Set up screenshots plugin
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    val screenshots = extend(CustomScreenshots()) {
      quitAfterScreenshot = false
      scale = 2.0
      folder = "screenshots/$progName/"
      captureEveryFrame = false
    }

    // Circle in the middle (... wow that is such a boring ass name ...)
    val origin = Vector2(width / 2.0, height / 2.0)
    val radius = width / 5.0
    val circleInTheMiddle = Circle(origin, radius)

    // Length of flow lines
    val lineLength = 200

    // Grid settings
    val xStep = 10
    val yStep = 2
    val jitter = (xStep + yStep) * 0.5 * 0.5

    // Controls the way in which the predominant "angle" (sin wave) mixes with the noise
    val minAngleInfluence = 0.1
    val maxAngleInfluence = 2.0
    val radiusFadeZonePercent = 0.2

    // number of blobs ("paint splotches")
    val nBlobs = 10000

    // Foreground and Background colors
    val fg = ColorRGBa.BLACK
    val bg = ColorRGBa.WHITE
    backgroundColor = bg

    // Seed pretty much determines everything about the drawing,
    // since it is either used directly in Perlin functions or indirectly as the RNG seed
    var seed = random(1.0, Int.MAX_VALUE.toDouble()).toInt()
    // seed = 766645261 // this looks much better than average, but not sure why
    // seed = 1384882992
    val rng = Random(seed.toLong())
    println(
      """
        seed = $seed
      """.trimIndent()
    )

    // There are three noise "octaves" that mix together, each with a different "scale"
    // (perhaps need a better term for this, it is more like the wavelength of the noise)
    val scaleOne = random(1000.0, 1800.0, rng)
    val scaleTwo = random(100.0, 400.0, rng)
    val scaleThree = random(20.0, 85.0, rng)

    // Influences dictate how much each "octave" influences the overall vector field.
    // They are pairs because a noise field will fluctuate each octave's influence spatially
    val influenceOne = Pair(random(0.01, 0.3, rng), random(0.3, 0.75, rng))
    val influenceTwo = Pair(random(0.01, 0.3, rng), random(0.3, 0.75, rng))
    val influenceThree = Pair(random(0.01, 0.3, rng), random(0.3, 0.75, rng))

    // noiseMapScales control the noise functions which map the spatial distribution of the various noise "octaves"
    val noiseMapScaleOne = random(scaleOne, scaleTwo, rng)
    val noiseMapScaleTwo = random(scaleTwo, scaleThree, rng)
    val noiseMapScaleThree = random(scaleThree, scaleOne, rng)

    // other less-important parameters -- might as well randomize for good fun
    val epsilon = random(0.25, 1.0, rng)
    val sinScale = random(35.0, 100.0, rng)
    val sinAmp = random(0.4, 0.8, rng)

    // You can never have too much logging*
    // *that is a lie ^
    println(
      """
        scaleOne = $scaleOne
        scaleTwo = $scaleTwo
        scaleThree = $scaleThree
        influenceOne = $influenceOne
        influenceTwo = $influenceTwo
        influenceThree = $influenceThree
        noiseMapScaleOne = $noiseMapScaleOne
        noiseMapScaleTwo = $noiseMapScaleTwo
        noiseMapScaleThree = $noiseMapScaleThree
        epsilon = $epsilon
        sinScale = $sinScale
        sinAmp = $sinAmp
      """.trimIndent()
    )

    /**
     * mixNoise1 creates three "octaves" of curl noise
     * and mixes them together based on the cursor position.
     * The noise is mixed with a straight vector pointing in the direction of the dominant "angle"
     * for the cursor. As the cursor moves away from the center, the mix of noise to straight angle changes.
     */
    fun mixNoise(cursor: Vector2, angle: Double = 0.0): Vector2 {
      // ratios determine the way of how the octaves will mix together
      val ratioOne = map(
        -1.0, 1.0,
        influenceOne.first, influenceOne.second,
        simplex(seed, cursor / noiseMapScaleOne)
      )
      val ratioTwo = map(
        -1.0, 1.0,
        influenceTwo.first, influenceTwo.second,
        simplex(seed, cursor / noiseMapScaleTwo)
      )
      val ratioThree = map(
        -1.0, 1.0,
        influenceThree.first, influenceThree.second,
        simplex(seed, cursor / noiseMapScaleThree)
      )

      // define a lambda for our noise function,
      // which blends together a perlin field mapped to radians with
      // our angle for this line.
      // The `mix` could use some more experimentation
      val curlFunc = { i: Int, x: Double, y: Double ->
        mapToRadians(-1.0, 1.0, perlin(i, x, y))
      }

      // If line is inside circle, most of the influence comes from the noise.
      // If it is outside the circle, most of the influence comes from the angle.
      // The boundary between the two fades for smoother effect
      val ratioAngle = if (circleInTheMiddle.contains(cursor))
        minAngleInfluence
      else if (cursor.distanceTo(origin) < radius * (1.0 + radiusFadeZonePercent))
        mix(minAngleInfluence, maxAngleInfluence, (cursor.distanceTo(origin) - radius) / (radius * radiusFadeZonePercent))
      else
        maxAngleInfluence

      // layer curl noise together, with primary angle influence diminishing with length
      val res = Vector2(cos(angle), sin(angle)) * ratioAngle +
        curlOfCurl(curlFunc, seed, cursor / scaleOne, epsilon) * ratioOne +
        curl(curlFunc, seed, cursor / scaleTwo, epsilon) * ratioTwo +
        curlOfCurl(curlFunc, seed, cursor / scaleThree, epsilon) * ratioThree

      return res.normalized
    }

    // sin wave is the predominant flow of the flow lines
    val sinWave = { x: Double -> sin(x / sinScale) * sinAmp }

    // Create the flow lines. Starting points are roughly grid-like
    val flowLines = grid(
      0, width, xStep,
      (origin.y - radius * 0.35).toInt(), (origin.y + radius * 0.35).toInt(), yStep
    ) { x: Double, y: Double ->
      contour {
        val x = x + random(-jitter, jitter, rng)
        val y = y + random(-jitter, jitter, rng)
        moveTo(x, y + sinWave(x) + mixNoise(Vector2(x, y)).y)
        List(lineLength) {
          val newCursor = cursor + Vector2(1.0, sinWave(cursor.x))
          val differentialAngle = atan2(newCursor.y - cursor.y, newCursor.x - cursor.x)
          lineTo(cursor + mixNoise(cursor, differentialAngle))
        }
      }
    }

    // Blobs are the "paint splotches" along the top and bottom which add some texture.
    // Functionally they could be circles but distorting them slightly gives a much more organic texture
    val blobs = List(nBlobs) {
      // y position needs to be semi-random, but with distribution concentrated towards edges outside
      val x = random(-10.0, width + 10.0, rng)
      val yOffset = gaussian(0.0, height / 8.0, rng)
      val y = if (yOffset > 0.0) yOffset else height.toDouble() + yOffset

      // Blob needs a few values randomized. Seed, radius, and noiseScale should all be slightly randomized
      val blobSeed = random(0.0, Int.MAX_VALUE.toDouble(), rng).toInt()
      val blobRadius = random(0.5, 4.0, rng)
      val noiseScale = random(0.5, 0.9, rng)

      SimplexBlob(origin = Vector2(x, y), seed = blobSeed, radius = blobRadius, noiseScale = noiseScale, moreConvexPlz = true).contour()
    }

    extend {
      drawer.isolated {
        fill = null
        stroke = fg
        strokeWeight = 1.0
        circle(circleInTheMiddle)
      }

      drawer.isolated {
        this.stroke = fg.opacify(0.1)
        flowLines.chunked(500).forEach { this.contours(it) }
      }

      drawer.isolated {
        stroke = null
        blobs.chunked(500).forEach {
          fill = fg.opacify(random(0.1, 0.3, rng))
          this.contours(it)
        }
      }

      // set seed for next iteration
      // seed = random(1.0, Int.MAX_VALUE.toDouble()).toLong()
      // screenshots.append = "seed-$seed"
    }
  }
}

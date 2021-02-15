/**
 * Shameless clone of http://roberthodgin.com/project/meander
 * Extensive notes on implementation in the MeanderingRiver class
 * src/main/kotlin/shape/MeanderingRiver.kt
 */
package sketch

import noise.perlinCurl
import noise.simplexCurl
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.shadestyles.LinearGradient
import org.openrndr.extra.shadestyles.RadialGradient
import org.openrndr.extras.color.presets.LIGHT_SLATE_GRAY
import org.openrndr.extras.color.presets.WHITE_SMOKE
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import shape.FractalizedLine
import shape.meanderRiver
import util.MixNoise
import util.MixableNoise
import util.timestamp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.random.Random

fun main() = application {
  configure {
    width = 500
    height = 800
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    // seed = 359561763
    // seed = 1288818041
    println("seed = $seed")
    val rng = Random(seed)

    val bgGradient = RadialGradient(ColorRGBa.WHITE_SMOKE, ColorRGBa.WHITE_SMOKE.mix(ColorRGBa.LIGHT_SLATE_GRAY, 0.2), Vector2.ZERO)
    val purple = ColorRGBa.fromHex("8A4DCB")
    val blue = ColorRGBa.fromHex("4F8CE8")
    val opacity = 0.05
    val fgGradient = LinearGradient(purple.opacify(opacity), blue.opacify(opacity), rotation = PI / 2.0)

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 4.0
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }

    val noiseScale = hypot(width * 0.5, height * 0.5) * 0.5

    val noise1 = MixableNoise(noiseScale * 0.7, 0.1 to 0.5, { pt -> perlinCurl(seed, pt) }, { pt -> simplex(seed, pt / noiseScale * 0.15) }, -1.0 to 1.0)
    val noise2 = MixableNoise(noiseScale * 1.5, 0.5 to 1.0, { pt -> simplexCurl(seed, pt) }, { pt -> simplex(seed, pt / noiseScale * 0.75) }, -1.0 to 1.0)
    val mixable = MixNoise(listOf(noise1, noise2))

    // taking the log10 of the simplex noise (adjusted to range 0.1 to 1.0) gives us a distribution weighted towards the larger magnitudes
    val scaledParam = { start: Double, end: Double ->
      { pt: Vector2 ->
        // map(-1.0, 0.0, start, end, log10(simplex(seed, pt / noiseScale) * 0.45 + 0.55)) // alternative, non-mixed noise
        val noise = mixable.mix(pt)
        map(-PI, PI, start, end, atan2(noise.y, noise.x))
      }
    }

    val river = meanderRiver {
      this.points = FractalizedLine(listOf(Vector2(width * 0.5, height * -0.1), Vector2(width * 0.5, height * 1.1)), rng).perpendicularSubdivide(11, 0.45).points
      // definitely some fine-tuning that could happen with these params...
      this.meanderStrength = scaledParam(5.0, 6.5)
      // this.curvatureScale = { pt: Vector2 ->
      //   val noiseScaleMap = map(-1.0, 1.0, noiseScale * 0.75, noiseScale * 1.5, simplex(seed, noiseScale))
      //   map(-1.0, 0.0, 1.0, 175.0, log10(simplex(seed, pt / noiseScaleMap) * 0.45 + 0.55)).toInt()
      // }
      this.curvatureScale = { pt: Vector2 ->
        // map(-1.0, 0.0, start, end, log10(simplex(seed, pt / noiseScale) * 0.45 + 0.55))
        val noise = mixable.mix(pt)
        map(-PI, PI, 1.0, 175.0, atan2(noise.y, noise.x)).toInt()
      }
      this.tangentBitangentRatio = scaledParam(0.1, 0.9)
      // this.shouldSmooth = false
      this.smoothness = 1
      this.oxbowShrinkRate = 10.0
      this.pointTargetDistance = scaledParam(1.0, 2.0)
      this.firstFixedPointPct = 0.01
      this.lastFixedPointPct = 0.99
    }

    val layerSize = 12
    val historicalRecord = mutableListOf<ShapeContour>()
    var channels = mutableListOf<ShapeContour>()

    val drawEveryNth = 3
    var drawIndex = 1

    fun channelsToContour(channels: List<ShapeContour>): ShapeContour {
      val first = channels.first().clockwise.segments
      val last = channels.last().counterClockwise.segments
      // Sometimes the first and last contours don't match up correctly.
      // ShapeContour doesn't allow this, but we don't really care for our purposes,
      // so we can just draw an open ShapeContour of the first line. It will just look like one edge of the shape
      return try {
        ShapeContour(first + last, closed = true)
      } catch (e: IllegalArgumentException) {
        ShapeContour(first, closed = false)
      }
    }

    extend {
      drawer.isolated { // bg gradient
        stroke = null
        shadeStyle = bgGradient
        rectangle(Rectangle(Vector2.ZERO, width.toDouble(), height.toDouble()))
      }

      river.run()
      channels.add(river.channel)

      if (channels.size > 0 && channels.size % layerSize == 0) {
        if (drawIndex == drawEveryNth) {
          historicalRecord.add(channelsToContour(channels))
          channels = mutableListOf()
          drawIndex = 0
        }
        drawIndex++
      }

      drawer.strokeWeight = 0.5

      drawer.isolated { // "historical record"
        stroke = purple.mix(blue, 0.5).opacify(opacity * 0.5)
        shadeStyle = fgGradient
        contours(historicalRecord)
      }

      drawer.isolated { // active channel
        if (channels.size > 0) {
          // fill = blue.mix(ColorRGBa.WHITE, 0.5).opacify(opacity * 2.0)
          shadeStyle = fgGradient
          stroke = purple.mix(blue, 0.5).opacify(opacity * 3.0)
          contour(channelsToContour(channels))
        }
      }

      if (true && frameCount % 1000 == 0) {
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
        screenshots.trigger()
      }
    }
  }
}

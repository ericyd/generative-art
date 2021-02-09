/**
 * Shameless clone of http://roberthodgin.com/project/meander
 * Extensive notes on implementation in the MeanderingRiver class
 * src/main/kotlin/shape/MeanderingRiver.kt
 *
 * In this sketch, we aren't using the oxbows at all because they basically overlap the historical record
 */
package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.hsla
import org.openrndr.draw.isolated
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.shadestyles.RadialGradient
import org.openrndr.extras.color.palettes.colorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Segment
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import shape.FractalizedLine
import shape.meanderRiver
import util.timestamp
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 850
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    // seed = 1633427433
    // seed = 1243183535
    println("seed = $seed")
    val rng = Random(seed)

    val bg = ColorRGBa.fromHex("543D76") // Cyber Grape
    val gradient = RadialGradient(ColorRGBa.fromHex("815DB6"), bg, Vector2.ZERO)
    val border = ColorRGBa.fromHex("2A0637") // Russian Violet
    backgroundColor = bg

    val colors = listOf(
      ColorRGBa.fromHex("db7a5c"), // Burnt Sienna
      ColorRGBa.fromHex("f9d071"), // Orange Yellow Crayola
      ColorRGBa.fromHex("74BDDC"), // Dark sky blue
      ColorRGBa.fromHex("ffb3ad"), // Melon
      ColorRGBa.fromHex("589ABB"), // Air superiority blue
      ColorRGBa.fromHex("fdedd3"), // Papaya Whip
    )

    var shuffledColors = colors.shuffled(rng).mapIndexed { index, color ->
      map(0.0, colors.size - 1.0, 0.1, 1.0, index.toDouble()) to color.toRGBa()
    }
    // add the last color to the front so the blending is smooth
    shuffledColors = listOf(0.0 to shuffledColors.last().second) + shuffledColors

    val spectrum = colorSequence(*shuffledColors.toTypedArray())

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 3.0
      captureEveryFrame = false
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }

    val noiseScale = hypot(width * 0.5, height * 0.5) * 0.2

    val river = meanderRiver {
      this.points = FractalizedLine(listOf(Vector2(width * -0.1, height * 0.5), Vector2(width * 1.1, height * 0.5)), rng).perpendicularSubdivide(10, 0.3).points
      this.meanderStrength = { 50.0 }
      this.curvatureScale = 10
      this.tangentBitangentRatio = { pt -> map(-1.0, 1.0, 0.3, 0.7, simplex(seed, (pt ?: Vector2.ONE) / noiseScale)) }
      this.smoothness = 5
      this.oxbowShrinkRate = 10.0
      this.curveMagnitude = { pt -> map(-1.0, 1.0, 0.25, 7.5, simplex(seed, (pt ?: Vector2.ONE) / noiseScale)) }
    }

    val layerSize = 12
    val historicalRecord = mutableListOf<Pair<ColorRGBa, ShapeContour>>()
    var channels = mutableListOf<ShapeContour>()

    val drawEveryNth = 3
    var drawIndex = 1

    fun channelsToContour(channels: List<ShapeContour>): ShapeContour {
      val first = channels.first().clockwise.segments
      val last = channels.last().counterClockwise.segments
      // val segments = first + listOf(Segment(first.last().end, last.first().start)) + last + listOf(Segment(last.last().end, first.first().start))
      val short = if (first.size > last.size) last else first
      val long = if (first.size > last.size) first else last
      val segments = short + listOf(Segment(short.last().end, long.first().start)) + long + listOf(Segment(long.last().end, short.first().start))
      return ShapeContour(segments, closed = true)
    }

    extend {
      drawer.isolated {
        shadeStyle = gradient
        rectangle(Rectangle(Vector2.ZERO, width.toDouble(), height.toDouble()))
      }
      river.run()

      channels.add(river.channel)
      val pct = (frameCount % (layerSize * layerSize * drawEveryNth) / (layerSize * layerSize * drawEveryNth).toDouble())
      if (channels.size > 0 && channels.size % layerSize == 0) {
        if (drawIndex == drawEveryNth) {
          // only add the part of the list to create gaps in the design
          //  TODO: figure out how to introduce gaps, without causing weird artifacts where the layers move after placement
          // val targetChannels = channels.subList(channels.size / drawEveryNth, channels.size)
          val targetChannels = channels
          historicalRecord.add(spectrum.index(pct) to channelsToContour(targetChannels))
          channels = mutableListOf()
          drawIndex = 0
        }
        drawIndex++
      }

      drawer.fill = null
      drawer.strokeWeight = 0.5

      if (historicalRecord.size >= layerSize) {
        historicalRecord.removeAt(0) // this is slow but necessary
      }

      // draw "historical record"
      historicalRecord.forEachIndexed { i, (color, layer) ->
        drawer.isolated {
          // the last layer is about to be unshifted, so we should fade out
          val opacity = (1.0 - channels.size / (layerSize * drawEveryNth).toDouble()).pow(2.0) // I don't think opacity scales linearly
          fill = if (i == 0) color.opacify(opacity) else color
          stroke = if (i == 0) border.opacify(opacity) else border
          contour(layer)
        }
      }

      // draw active channel
      drawer.isolated {
        if (channels.size > 0) {
          fill = spectrum.index(pct)
          stroke = border
          contour(channelsToContour(channels))
        }
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

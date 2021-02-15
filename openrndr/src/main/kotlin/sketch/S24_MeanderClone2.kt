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
import org.openrndr.draw.isolated
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.ShapeContour
import shape.FractalizedLine
import shape.meanderRiver
import util.timestamp
import kotlin.math.hypot
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 500
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    // seed = 1633427433
    // seed = 1849979458 // <-- used in posted images
    println("seed = $seed")
    val rng = Random(seed)

    val primary = ColorRGBa.fromHex("00344D")
    val secondary = ColorRGBa.fromHex("C2DEFF")
    backgroundColor = primary

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 4.0
      captureEveryFrame = false
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }

    val noiseScale = hypot(width * 0.5, height * 0.5) * 0.2

    val river = meanderRiver {
      this.points = FractalizedLine(listOf(Vector2(width * -0.1, height * 0.5), Vector2(width * 1.1, height * 0.5)), rng).perpendicularSubdivide(10, 0.3).points
      this.meanderStrength = { 50.0 }
      this.curvatureScale = { 10 }
      this.tangentBitangentRatio = { pt -> map(-1.0, 1.0, 0.3, 0.7, simplex(seed, pt / noiseScale)) }
      this.smoothness = 5
      this.oxbowShrinkRate = 10.0
      this.pointTargetDistance = { pt -> map(-1.0, 1.0, 0.25, 5.5, simplex(seed, pt / noiseScale)) }
    }

    fun channelsToContour(channels: List<ShapeContour>): ShapeContour {
      val first = channels.first().counterClockwise
      var last = channels.last().clockwise
      // Sometimes the first and last contours don't match up correctly.
      // ShapeContour doesn't allow this, but we don't really care for our purposes,
      // so we can just draw an open ShapeContour of the first line. It will just look like one edge of the shape
      return try {
        ShapeContour(first.segments + last.segments, closed = true)
      } catch (e: IllegalArgumentException) {
        ShapeContour(first.segments, closed = false)
      }
    }

    val layerSize = 12
    val historicalRecord = mutableListOf<ShapeContour>()
    var channels = mutableListOf<ShapeContour>()

    val drawEveryNth = 3
    var drawIndex = 1

    extend {
      river.run()

      channels.add(river.channel)
      // val pct = (frameCount % (layerSize * layerSize * drawEveryNth) / (layerSize * layerSize * drawEveryNth).toDouble())
      if (channels.size > 0 && channels.size % layerSize == 0) {
        if (drawIndex == drawEveryNth) {
          historicalRecord.add(channelsToContour(channels))
          channels = mutableListOf()
          drawIndex = 0
        }
        drawIndex++
      }

      if (historicalRecord.size >= layerSize) {
        historicalRecord.removeAt(0) // this is slow but necessary
      }

      drawer.fill = null
      drawer.stroke = null

      // attempt to draw an outline of channel
      if (channels.isNotEmpty()) {
        // the elements of the channel are typically closed ShapeContours, so we only want half to draw a single line
        val mirror = channels.last()
        for (offset in 50 until 1000 step 50) {
          drawer.isolated {
            strokeWeight = 0.4
            stroke = secondary.mix(primary, 1.0 - (offset / 1000.0) * 0.5)
            lineStrip(mirror.segments.map { it.start + Vector2(0.0, offset.toDouble()) })
            lineStrip(mirror.segments.map { it.start - Vector2(0.0, offset.toDouble()) })
          }
        }
      }

      // draw "historical record"
      // The historical record colors will mix between the secondary and the primary (background).
      // 0.0 is 100% secondary, and 1.0 is 100% primary.
      // The layers should fade from 0.0 to 1.0 at the end of their life.
      historicalRecord.forEachIndexed { i, cs ->
        drawer.isolated {
          // // THIS WORKS
          // // The general idea is that each layer should start at a certain concentration, and increase towards the next "mark" in the iteration
          // // It increases because increasing correlates with higher mix of the background color (which appears as "fading out")
          // // When the historicalRecord is just starting, it doesn't have enough pieces in it for the math to work, so you have to adjust the values by the offset of the layer count
          // val adjustForWhenHistoricalRecordHasFewerThanCountPerLayer = (layerSize - 1.0 - historicalRecord.size) / layerSize
          // // I think the "minus 2" has to do with the fact that we remove a section before we get here 🤷‍
          // val pctMixLayer = map(0.0, layerSize - 2.0, 1.0 - 1.0 / layerSize, 0.0, i.toDouble()) - adjustForWhenHistoricalRecordHasFewerThanCountPerLayer
          // val pctMixTime = map(0.0, (layerSize * drawEveryNth) - 1.0, 0.0, 1.0 / layerSize, channels.size.toDouble())
          // val pct = pctMixLayer + pctMixTime
          // fill = secondary.mix(primary, pct)

          // Debugging message (hopefully never needed again 🙏)
          // println("i: $i, pctMixLayer: $pctMixLayer, pctMixTime: $pctMixTime, pct: $pct, adjustment: $adjustForWhenHistoricalRecordHasFewerThanCountPerLayer")

          // ok so this is logically wrong, but from a visual perspective alone it looks pretty cool
          val pctMixLayer = map(0.0, layerSize.toDouble(), 0.0, 1.0 - 1.0 / (historicalRecord.size.toDouble() + 1.0), i.toDouble())
          val pctMixTime = map(0.0, layerSize.toDouble(), 0.0, 1.0 / historicalRecord.size, channels.size.toDouble())
          val pct = pctMixLayer + pctMixTime
          fill = secondary.mix(primary, pct)

          contour(cs)
        }
      }

      // draw active channel
      drawer.isolated {
        stroke = secondary
        contours(channels)
      }

      if (true && channels.size > 0 && channels.size % layerSize == 0 && drawIndex == drawEveryNth) {
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
        screenshots.trigger()
      }
    }
  }
}

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
import org.openrndr.shape.Circle
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
    seed = 1633427433
    println("seed = $seed")
    val rng = Random(seed)

    val primary = ColorRGBa.fromHex("00344D")
    val secondary = ColorRGBa.fromHex("C2DEFF")
    backgroundColor = primary

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
      this.curveMagnitude = { pt -> map(-1.0, 1.0, 0.25, 5.5, simplex(seed, (pt ?: Vector2.ONE) / noiseScale)) }
    }

    val countPerLayer = 12
    val historicalRecord = mutableListOf<MutableList<ShapeContour>>()
    var channels = mutableListOf<ShapeContour>()


    extend {
      river.run()

      channels.add(river.channel)
      if (channels.size >= countPerLayer) {
        historicalRecord.add(channels)
        channels = mutableListOf()
      }

      if (historicalRecord.size >= countPerLayer) {
        historicalRecord.removeAt(0) // this is slow but necessary
      }

      drawer.fill = null
      drawer.strokeWeight = 4.0

      // draw "historical record"
      // The historical record colors will mix between the secondary and the primary (background).
      // 0.0 is 100% secondary, and 1.0 is 100% primary.
      // The layers should fade from 0.0 to 1.0 at the end of their life.
      historicalRecord.forEachIndexed { i, cs ->
        drawer.isolated {
          // THIS WORKS
          // The general idea is that each layer should start at a certain concentration, and increase towards the next "mark" in the iteration
          // It increases because increasing correlates with higher mix of the background color (which appears as "fading out")
          // When the historicalRecord is just starting, it doesn't have enough pieces in it for the math to work, so you have to adjust the values by the offset of the layer count
          val adjustForWhenHistoricalRecordHasFewerThanCountPerLayer = (countPerLayer - 1.0 - historicalRecord.size) / countPerLayer
          // I think the "minus 2" has to do with the fact that we remove a section before we get here 🤷‍
          val pctMixLayer = map(0.0, countPerLayer - 2.0, 1.0 - 1.0 / countPerLayer, 0.0, i.toDouble()) - adjustForWhenHistoricalRecordHasFewerThanCountPerLayer
          val pctMixTime = map(0.0, countPerLayer - 1.0, 0.0, 1.0 / countPerLayer, channels.size.toDouble())
          val pct = pctMixLayer + pctMixTime
          stroke = secondary.mix(primary, pct)

          // Debugging message (hopefully never needed again 🙏)
          // println("i: $i, pctMixLayer: $pctMixLayer, pctMixTime: $pctMixTime, pct: $pct, adjustment: $adjustForWhenHistoricalRecordHasFewerThanCountPerLayer")

          // ok so this is logically wrong, but from a visual perspective alone it looks pretty cool
          // val pctMixLayer = map(0.0, countPerLayer.toDouble(), 0.0, 1.0 - 1.0 / (historicalRecord.size.toDouble() + 1.0), i.toDouble())
          // val pctMixTime = map(0.0, countPerLayer.toDouble(),  0.0, 1.0 / historicalRecord.size, channels.size.toDouble())
          // val pct = pctMixLayer + pctMixTime
          // stroke = secondary.mix(primary, pct)

          contours(cs)
        }
      }

      // draw active channel
      drawer.isolated {
        stroke = secondary
        contours(channels)
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

/**
 * Shameless clone of http://roberthodgin.com/project/meander
 * Extensive notes on implementation in the MeanderingRiver class
 * src/main/kotlin/shape/MeanderingRiver.kt
 */
package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import shape.FractalizedLine
import shape.meanderRiver
import util.timestamp
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
    // seed = 1604248274
    println("seed = $seed")
    val rng = Random(seed)

    backgroundColor = ColorRGBa.WHITE

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 3.0
      captureEveryFrame = false
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }

    val river = meanderRiver {
      this.points = FractalizedLine(listOf(Vector2(0.0, height * 0.5), Vector2(width.toDouble(), height * 0.5)), rng).perpendicularSubdivide(10, 0.6).points
      // vertical alternative
      // this.points = FractalizedLine(listOf(Vector2(width * 0.5, 0.0), Vector2(width * 0.5, height.toDouble())), rng).perpendicularSubdivide(10, 0.6).points
      this.meanderStrength = { 50.0 }
      this.curvatureScale = { 10 }
      this.tangentBitangentRatio = { 0.550 }
      this.smoothness = 5
      this.oxbowShrinkRate = 10.0
      this.pointTargetDistance = { 2.50 }
    }

    extend {
      river.run()

      drawer.fill = null
      drawer.stroke = ColorRGBa.BLACK
      drawer.strokeWeight = 4.0
      drawer.contours(river.oxbows)
      drawer.contour(river.channel)

      // This draws the vector that determines the direction the line will grow
      drawer.lineSegments(river.influenceVectors(1).map { it.extend(1.50) })

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

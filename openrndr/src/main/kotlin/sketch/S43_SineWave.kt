/**
 * Create new works here, then move to parent package when complete
 */
package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.color.palettes.ColorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.map
import util.timestamp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 800
    height = 800
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 4.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }

    backgroundColor = ColorRGBa.WHITE

    val spectrum = ColorSequence(
      listOf(
        0.0 to ColorRGBa.fromHex("efb999"),
        0.2 to ColorRGBa.fromHex("b9eae4"),
        0.4 to ColorRGBa.fromHex("4f41a3"),
        0.6 to ColorRGBa.fromHex("92b76f"),
        0.8 to ColorRGBa.fromHex("ddb742"),
        1.0 to ColorRGBa.fromHex("e094be")
      )
    )


    extend {
      val rng = Random(seed)
      
      val lineHeight = height * 0.75
      
      val topMajorOffsetX = random(width * 0.1, width * 0.9, rng)
      val topMinorWaveAmplitude = random(0.1, 0.25, rng)
      val topMinorScaleFactor = random(1.1, 5.0, rng)
      fun topSine(x: Double): Double {
        val majorWave = sin(x + topMajorOffsetX)
        val minorWave = topMinorWaveAmplitude * sin(x * topMinorScaleFactor + topMajorOffsetX)
        val scaleFactor = (2.0 + topMinorWaveAmplitude * 2.0)
        return ((majorWave + minorWave) / scaleFactor + 0.5) * lineHeight
      }
      val bottomMajorOffsetX = random(width * 0.1, width * 0.9, rng)
      val bottomMinorWaveAmplitude = random(0.1, 0.25, rng)
      val bottomMinorScaleFactor = random(1.1, 5.0, rng)
      fun bottomSine(x: Double): Double {
        val majorWave = sin(x + bottomMajorOffsetX)
        val minorWave = bottomMinorWaveAmplitude * sin(x * bottomMinorScaleFactor + bottomMajorOffsetX)
        val scaleFactor = (2.0 + bottomMinorWaveAmplitude * 2.0)
        return ((majorWave + minorWave) / scaleFactor + 0.5) * lineHeight
      }
      val stepSize = 8
      drawer.strokeWeight = stepSize / 4.0
      drawer.lineCap = LineCap.ROUND
      val x = { i: Int -> map(0.0, width.toDouble(), 0.0, PI, i.toDouble()) } 
      
      // top
      for (i in 0..width step stepSize) {
        drawer.stroke = spectrum.index(random(0.0, 1.0, rng))
        drawer.lineSegment(Vector2(i.toDouble(), 0.0), Vector2(i.toDouble(), topSine(x(i))))
      }
      
      // bottom
      for (i in (stepSize / 2)..width step stepSize) {
        drawer.stroke = spectrum.index(random(0.0, 1.0, rng))
        drawer.lineSegment(Vector2(i.toDouble(), height.toDouble()), Vector2(i.toDouble(), height - bottomSine(x(i))))
      }
      

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
      }
    }
  }
}

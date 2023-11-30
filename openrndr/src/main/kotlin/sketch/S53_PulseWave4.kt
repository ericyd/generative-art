package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.math.*
import org.openrndr.shape.*
import util.rotatePoint
import util.timestamp
import kotlin.math.*
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
      contentScale = 3.0
      captureEveryFrame = false
      name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
    }

    val bg = ColorRGBa.WHITE
    backgroundColor = bg

    // this basically generates a wave function inspired by fBm
    fun generateWaveFn(oscillatorCount: Int, amplitude: Double, scale: Double, rng: Random): (x: Double) -> Double {
      // next level = allow oscillator amplitudes to vary over time -- i.e. oscillatorAmplitude is actually a function
      val oscillators = List(oscillatorCount) { index ->
        val i = index + 1.0
        val oscillatorScale = scale * random(i.pow(2.0) * 2.5, i.pow(2.0) * 3.5, rng)
        val phase = random(-PI, PI, rng)
        val oscillatorAmplitude = { x: Double ->
          // this is completely arbitrary...
          // higher is "more attenuated at higher frequencies"
          val amplitudeBase = 30.0
          val variation = sin(x * scale / 3.0 + (phase - PI * i * 0.5))
          variation * amplitudeBase.pow(1.0 / i) / amplitudeBase
        }
        Triple(oscillatorScale, oscillatorAmplitude, phase)
      }

      return { x: Double ->
        oscillators.sumOf { (scale, amplitude, phase) -> sin(x * scale + phase) * amplitude(x) } * amplitude
      }
    }

    // generates a line based on the defined wave function
    fun baseline(waveBaseline: Double, wave: (x: Double) -> Double): List<Vector2> {
      val min = width / -2
      val max = width * 3 / 2
      return List(max - min) {
        val x = it.toDouble()
        val y = wave(x) + waveBaseline
        Vector2(x, y)
      }
    }

    extend {
      seed = 1862747061
      val rng = Random(seed)
      drawer.stroke = ColorRGBa.BLACK
      drawer.strokeWeight = 1.0

      /* wave props */
      val majorScale = 1.0 / width * 1.25
      val waveAmplitude = height / 3.0
      val wave = generateWaveFn(3, waveAmplitude, majorScale, rng)
      var offset = random(height * 0.1, height * 0.5, rng)
      /* end wave props */
      val waveAmplitudes = List(2) { random(height * 0.01, height * 0.1,  rng) }
      val baselines = waveAmplitudes.map {
        offset += it
        baseline(offset, wave)
      }

      // draw base contours
      drawer.contours(baselines.map { ShapeContour.fromPoints(it, closed = false) })

      val top = baselines[0]
      val bottom = baselines[1]
      val slopeSize = 40
      val stepSize = 10
      for (index in 0..top.size step stepSize) {
        // don't overflow; we're connecting top to "bottom + slopeSize"
        if (index + slopeSize > bottom.size - 1) {
          continue
        }
        // the higher this is, the more "ease" is in the curve
        val rotation = PI * -0.2
        drawer.contour(contour {
          val start = top[index]
          moveTo(start)
          val end = bottom[index + slopeSize]
          val diff = end - start
          val ctrl1 = rotatePoint(diff * 0.3 + start, rotation, start)
          val ctrl2 = rotatePoint(diff * 0.7 + start, rotation, end)
          curveTo(ctrl1, ctrl2, end)
        })
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
      }
    }
  }
}

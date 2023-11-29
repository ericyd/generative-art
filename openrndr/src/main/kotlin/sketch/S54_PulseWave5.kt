package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.math.*
import org.openrndr.shape.*
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

    val ease = object {
      /**
       * from https://www.febucci.com/2018/08/easing-functions/
       * @param start
       * @param end
       * @param percentage [0.0, 1.0]
       */
      fun lerp(start: Double, end: Double, percentage: Double): Double {
        return (start + (end - start) * percentage)
      }

      // this "should" be called "in" but that's a Kotlin keyword
      fun easeIn(t: Double): Double {
        return t.pow(4.0)
      }

      fun flip(t: Double): Double {
        return 1.0 - t
      }

      fun out(t: Double): Double {
        return flip(easeIn(flip(t)))
      }

      fun inOut(t: Double): Double {
        return lerp(easeIn(t), out(t), t)
      }

    }

    /**
     * Core algorithm
     * 1. generate a wave function based on various wave parameters; similar to fBm
     * 2. generate a set of "baselines" - the boundaries of each section.
     *    They all use the same wave function, but have different amplitudes (thickness)
     * 3. for each pair of baselines, draw a set of "waves" inside of them, where a wave is defined as
     *    - a line that starts on one baseline, and gradually moves to the other baseline over
     *      a period of `curveSize` points.
     *    - the mixing function is defined by an ease-in-out function
     */
    extend {
      val rng = Random(seed)
      drawer.stroke = ColorRGBa.BLACK
      drawer.strokeWeight = 1.0

      /* wave props */
      val majorScale = 1.0 / width * 1.25
      val waveAmplitude = height / 3.0
      val wave = generateWaveFn(3, waveAmplitude, majorScale, rng)
      var offset = random(height * 0.1, height * 0.5, rng)
      /* end wave props */
      val waveCount = 8
      val waveAmplitudes = List(waveCount) { random(height * 0.01, height * 0.1,  rng) }
      val baselines = waveAmplitudes.map {
        offset += it
        baseline(offset, wave)
      }

      // draw base contours
      // drawer.contours(baselines.map { ShapeContour.fromPoints(it, closed = false) })

      /** Draw wave sections */
      for (baselineIndex in 0 until waveCount - 1) {
        val top = baselines[baselineIndex]
        val bottom = baselines[baselineIndex + 1]
        val curveSize = 40
        val curveStepSize = 10
        for (index in 0 until top.size step curveStepSize) {
          val curveStart = clamp(index, 0, top.size - 1)
          val curveEnd = clamp(curveStart + curveSize, curveStart, top.size - 1)
          drawer.contour(contour {
            val start = top[curveStart]
            moveTo(start)

            for (curveIndex in curveStart until curveEnd) {
              val mixPercentage = ease.inOut((curveIndex - curveStart) / curveSize.toDouble())
              val next = top[curveIndex].mix(bottom[curveIndex], mixPercentage)
              lineTo(next)
            }
          })
        }
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
      }
    }
  }
}

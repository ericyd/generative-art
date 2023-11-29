// this isn't really "complete", but keeping it b/c it demonstrates a "custom fBm" function
package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.color.palettes.ColorSequence
import org.openrndr.extra.noise.gaussian
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
//    val colors = listOf(
//      "23214A",
//      "94EF86",
//      "F3ED76",
//      "E15F33",
//      "BE59E7",
//      "E56DB1",
//      "FF9B4E",
//      "A2F2EC",
//      "9C99E5",
//      "005D7E",
//    )

    /**
     * A wave pulse is given by the equation y=f(x,t)=Aexp(−B(x−vt)^2).
     * https://www.toppr.com/ask/en-us/question/a-wave-pulse-is-given-by-the-equation-yfxta-expbxvt2-given-a10mb10m2-and-v20ms-which/
     *
     * @param x the position on the x-axis at which to calculate the pulse function
     * @param t the "x offset". The pulse formula naturally centers at the origin,
     *          so `t` allows the pulse to occur in other spacial locations.
     * @param A amplitude. When negative, the pulse goes "up" since
     *          OPENRNDR has origin at upper left.
     * @return the `y` coordinate for the pulse function
     */
    fun pulse(x: Double, t: Double, A: Double): Double {
      // "spread" - lower is more spread out, higher is tighter
      val B = 0.00000001
      // the "squareness" -- higher is more square
      // 3.5 is kinda interesting
      // odd numbers are NOT symmetric -- this might be the most critical single value in this whole file
      val exp = 4.0
      return A * E.pow(-B * (x - t).pow(exp))
    }

    fun waveSet(tMin: Double,
                tMax: Double,
                A: Double,
                /**
                 * Pair is of <Point, Derivative at that point in radians>
                 */
                baseline: Sequence<Pair<Vector2, Double>>,
                stepSize: Double,
                rotateAbout: Vector2,
                spectrum: ColorSequence,
                rng: Random): Sequence<(d: Drawer) -> Unit> {
      // each waveSet pulls from a small portion of the total spectrum.
      val spectrumMin = random(0.0, 0.75, rng)
      val spectrumMax = spectrumMin + 0.25

      return sequence {
        // move successively through the values of "t",
        // where "t" is the peak of the wave pulse
        var t = tMin
        while (t < tMax) {
          var newContour = contour {
            val (start) = baseline.first()
            moveTo(start.x, start.y)

            for ((point, derivative) in baseline) {
              // this cuts off the pulse at the apex of the curve
//              if (point.x > t) {
//                continue
//              }

              val y = pulse(point.x, t, A) + point.y
              // I think rotating about "point" is a more "accurate" way to draw the wave, but
              // it creates some ugly overlaps
              // welllllllllll............. ok......... this sucks...
              val contourPoint = rotatePoint(Vector2(point.x, y), derivative, point)
//              lineTo(contourPoint)

              // debugging: draw the "baseline"
              lineTo(point)
            }
          }

          yield { drawer: Drawer ->
            drawer.stroke = spectrum.index(map(tMin, tMax, spectrumMin, spectrumMax, t))
            drawer.contour(newContour)
          }
          t += stepSize
        }
      }
    }

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

    fun baseline(waveBaseline: Double, wave: (x: Double) -> Double): Sequence<Pair<Vector2, Double>> {
      return sequence {
        // previous point is used to calculate derivative;
        // starts at the "first" position
        var previousPoint = Vector2(width / -2.0, wave(width / -2.0) + waveBaseline)

        // add some buffer .... just for fun (i.e. width / -2, width * 3/2)
        for (xInt in width / -2 until width * 3 / 2) {
          val x = xInt.toDouble()
          val y = wave(x) + waveBaseline
//          debugging: just make a straight line
//          val y = height / 2.0
          val derivative = atan2(y - previousPoint.y, x - previousPoint.x)
          previousPoint = Vector2(x, y)
          yield(Pair(previousPoint, derivative))
        }
      }
    }

    extend {
      seed = 1862747061
      val rng = Random(seed)
      val center = Vector2(width * 0.5, height * 0.5)
      // fake "all black" spectrum, just added for easier code re-use
      val spectrum = ColorSequence(listOf(Pair(0.0, ColorRGBa.BLACK), Pair(0.0, ColorRGBa.BLACK)))
      drawer.stroke = ColorRGBa.BLACK
      drawer.strokeWeight = 2.0

      val stepSize = 30.0
      val rotationAxis = Vector2.gaussian(center, center * 0.41, rng) // center

      /* wave props */
      val majorScale = 1.0 / width * 1.25
      val waveAmplitude = height / 3.0
      val wave = generateWaveFn(3, waveAmplitude, majorScale, rng)
      var offset = random(height * 0.1, height * 0.5, rng)
      /* end wave props */
      val waveAmplitudes = List(4) { random(height * 0.01, height * 0.1,  rng) }
      for (waveAmplitude in waveAmplitudes) {
        val waveSequence = waveSet(
          tMin = width * 0.25,
          tMax = width * 0.625,
          A = waveAmplitude,
          baseline(offset, wave),
          stepSize,
          rotationAxis,
          spectrum,
          rng
        )
        for (waveFn in waveSequence) {
          waveFn(drawer)
        }
        offset += waveAmplitude
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
      }
    }
  }
}

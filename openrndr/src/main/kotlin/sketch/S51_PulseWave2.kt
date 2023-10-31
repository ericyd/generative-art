package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.color.palettes.ColorSequence
import org.openrndr.extra.fx.edges.Contour
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.*
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.translate
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
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
    }

    val bg = ColorRGBa.WHITE
    backgroundColor = bg
    val colors = listOf(
      "23214A",
      "94EF86",
      "F3ED76",
      "E15F33",
      "BE59E7",
      "E56DB1",
      "FF9B4E",
      "A2F2EC",
      "9C99E5",
      "005D7E",
    )

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
      val exp = 3.0
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
      // currentContour gets merged with "newContour" to create a closed ShapeContour that can be filled
      // each contour itself is just a single line of the wave, so we must join two offset contours
      // to create a single fillable shape
      var currentContour: ShapeContour = contour {  }

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
              // // ok... so this is what I was "trying" to do...
              // val y = pulse(point.x, t, A) + point.y
              // lineTo(Vector2(point.x, y))

              val y = pulse(point.x, t, A) + point.y
              // rotating about `point` vs a fixed point is a **substantial** difference.
              // rotating about a fixed point is probably "cooler" but very unexpected patterns
              val contourPoint = rotatePoint(Vector2(point.x, y), derivative, rotateAbout)
              lineTo(contourPoint)

              // debugging: draw the "baseline"
              // lineTo(point)
            }
          }

          // once we've established at least one base contour, we can start combining them
          // and drawing our fillable shape contours
          if (!currentContour.empty) {
            yield { drawer: Drawer ->
              drawer.fill = spectrum.index(map(tMin, tMax, spectrumMin, spectrumMax, t))

              drawer.contour(
                ShapeContour.fromPoints(
                  // I **think** "asReversed" might be more efficient than "reversed" but not 100% sure
                  currentContour.segments.map { it.start } + newContour.segments.asReversed().map { it.start },
                  closed = true
                )
              )
            }
          }
          currentContour = newContour
          t += stepSize
        }
      }
    }

    fun baseline(waveBaseline: Double = 0.0, phase: Double, majorScale: Double, minorAmplitude: Double, minorScale: Double): Sequence<Pair<Vector2, Double>> {
      return sequence {
        val waveAmplitude = height / 3.0
        val wave = { x: Double ->
          val majorWave = sin(x * majorScale + phase)
          val minorWave = sin(x * minorScale + phase) * minorAmplitude
          (majorWave + minorWave) * waveAmplitude + waveBaseline
        }

        // previous point is used to calculate derivative;
        // starts at the "first" position
        var previousPoint = Vector2(width / -2.0, wave(width / -2.0))

        // add some buffer .... just cuz (i.e. width / -2, width * 3/2)
        for (xInt in width / -2 until width * 3 / 2) {
          val x = xInt.toDouble()
          val y = wave(x)
          val derivative = atan2(y - previousPoint.y, x - previousPoint.x)
          previousPoint = Vector2(x, y)
          yield(Pair(previousPoint, derivative))
        }
      }
    }

    extend {
      // wow not sure if other seeds will be interesting but this one ROCKS
//      seed = 1808905369
//      seed = 1703181398
//      seed = 1252262956
      val rng = Random(seed)
      val center = Vector2(width * 0.5, height * 0.5)
      val spectrum = ColorSequence(colors.shuffled(rng).mapIndexed { index, hex ->
        Pair(map(0.0, colors.size - 1.0, 0.0, 1.0, index.toDouble()), ColorRGBa.fromHex(hex))
      })
      drawer.stroke = ColorRGBa.BLACK
      drawer.strokeWeight = 2.0

      val stepSize = 30.0
      val rotationAxis = Vector2.gaussian(center, center * 0.41, rng) // center

      /* wave props */
      val phase = random(-PI, PI, rng)
      val majorScale = 1.0 / width * 2.25
      val minorAmplitude = random(0.1, 0.2, rng)
      val minorScale = majorScale * random(2.2, 3.3, rng)
      var offset = random(height * 0.1, height * 0.5, rng)
      /* end wave props */
      val waveAmplitudes = List(4) { random(height * 0.05, height * 0.2,  rng) }
      for (waveAmplitude in waveAmplitudes) {
        val waveSequence = waveSet(
          tMin = width * -0.25,
          tMax = width * 1.625,
          A = waveAmplitude,
          baseline(offset, phase, majorScale, minorAmplitude, minorScale),
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

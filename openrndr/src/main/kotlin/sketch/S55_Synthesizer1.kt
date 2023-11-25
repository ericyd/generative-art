package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.color.palettes.ColorSequence
import org.openrndr.extra.fx.edges.Contour
import org.openrndr.extra.noise.fbm
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.*
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.translate
import org.openrndr.shape.*
import util.grid
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
    class Oscillator(val period: Double, val amplitude: Double, val wave: (t: Double) -> Double, val phase: Double = 0.0) {
      val frequencyModulators = mutableListOf<Oscillator>()
      val amplitudeModulators = mutableListOf<Oscillator>()
      val phaseModulators = mutableListOf<Oscillator>()

      fun freq(t: Double): Double {
        val modulated = frequencyModulators.sumOf { it.output(t) }
        val baseFrequency = 2 * PI * 1.0 / period
        return baseFrequency + modulated
      }

      fun amp(t: Double): Double {
        val modulated = amplitudeModulators.sumOf { it.output(t) }
        return amplitude + modulated
      }

      fun phase(t: Double): Double {
        val modulated = phaseModulators.sumOf { it.output(t) }
        return phase + modulated
      }

      fun modulateFrequency(osc: Oscillator): Oscillator {
        frequencyModulators.add(osc)
        return this
      }

      fun modulateAmplitude(osc: Oscillator): Oscillator {
        amplitudeModulators.add(osc)
        return this
      }

      fun modulatePhase(osc: Oscillator): Oscillator {
        phaseModulators.add(osc)
        return this
      }

      fun output(t: Double): Double {
        return wave(t * freq(t) + phase(t)) * amp(t)
      }
    }

    class OscillatorXY(val period: Double, val amplitude: Double, val wave: (t: Double) -> Double, val phase: Double = 0.0) {
      val frequencyModulators = mutableListOf<OscillatorXY>()
      val amplitudeModulators = mutableListOf<OscillatorXY>()
      val phaseModulators = mutableListOf<OscillatorXY>()

      fun freq(x: Double, y: Double): Double {
        val modulated = frequencyModulators.sumOf { it.output(x, y) }
        val baseFrequency = 2 * PI * 1.0 / period
        println("$modulated, $baseFrequency")
        return baseFrequency + modulated
      }

      fun amp(x: Double, y: Double): Double {
        val modulated = amplitudeModulators.sumOf { it.output(x, y) }
        return amplitude + modulated
      }

      fun phase(x: Double, y: Double): Double {
        val modulated = phaseModulators.sumOf { it.output(x, y) }
        return phase + modulated
      }

      fun modulateFrequency(osc: OscillatorXY): OscillatorXY {
        frequencyModulators.add(osc)
        return this
      }

      fun modulateAmplitude(osc: OscillatorXY): OscillatorXY {
        amplitudeModulators.add(osc)
        return this
      }

      fun modulatePhase(osc: OscillatorXY): OscillatorXY {
        phaseModulators.add(osc)
        return this
      }

      fun output(x: Double, y: Double): Double {
        val effectiveFreq = freq(x, y)
        val effectivePhase = phase(x, y)
        return wave(x * effectiveFreq + effectivePhase) + wave(y * effectiveFreq + effectivePhase + PI / 2.0) * amp(x, y)
      }

      fun output(vec2: Vector2): Double {
        return output(vec2.x, vec2.y)
      }
    }

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

    extend {
      val rng = Random(seed)
      drawer.stroke = ColorRGBa.BLACK
      drawer.strokeWeight = 1.0

      val osc = Oscillator(width * 1.0, 1.0, ::sin, 0.0)
        .modulateAmplitude(Oscillator(width * 0.3, 0.08, ::sin, PI))
        .modulateAmplitude(Oscillator(width * 0.25, 0.16, ::sin, PI * 2.3))
        .modulateFrequency(Oscillator(width * 0.20, 0.001, ::sin, PI))
        .modulatePhase(Oscillator(width * 0.15, 0.4, ::sin, PI* 4.2))

      val osc2 = Oscillator(width * 1.1, 1.0, ::cos, 0.8)
        .modulateAmplitude(Oscillator(width * 0.232, 0.099, ::cos, PI * 0.248))
        .modulateAmplitude(Oscillator(width * 0.32, 0.12, ::sin, PI * 0.3))
        .modulateFrequency(Oscillator(width * 0.17, 0.0019, ::sin, PI * 4.9))
        .modulatePhase(Oscillator(width * 0.19, 0.32, ::cos, PI* 1.2))

      val stepSize = 10
//      grid(0, width, stepSize, 0, height, stepSize) { x: Double, y: Double ->
//        drawer.contour(contour {
//          var point = Vector2(x, y)
//          moveTo(point)
//          for (i in 0..30) {
////            val angle = map(-1.0, 1.0, -PI, PI, osc.output(point.x))
////            val output = osc.output(hypot(point.x, point.y))
//            val output = osc.output(atan2(point.x, point.y) * 300.0)
//            val angle = map(-1.0, 1.0, -PI, PI, output)
////            val angle = map(-1.0, 1.0, -PI, PI, simplex(seed, point / 100.0))
//            point = point + Vector2(sin(angle), cos(angle))
//            lineTo(point)
//          }
//        })
//      }


//      grid(0, width, stepSize, 0, height, stepSize) { x: Double, y: Double ->
////        val output = osc.output(atan2(x, y) * 300.0)
//        val output = osc.output(hypot(x, y))
////        val output = osc.output(x) * 0.5 + osc.output(y) * 0.5
////        val output = simplex(seed, x / 100.0, y / 100.0)
////        val output = simplex(seed, x / 100.0)
////        val output = osc.output(x) * 0.5 + osc2.output(y) * 0.5
//        val shade = map(-1.0, 1.0, 0.0, 1.0, output)
//        drawer.stroke = null
//        drawer.fill = ColorRGBa.WHITE.shade(shade)
//        drawer.rectangle(Rectangle(x, y, stepSize.toDouble(), stepSize.toDouble()))
//      }

      /**
       * OK!
       *
       * Before I forget:
       * Previous tries were resulting in "plaid" distributions, because the noise was 1-dimensional,
       * and did not vary in space. For example, the gradient operating on the "x" input was outputting
       * the same value whether it was at y=0 or y=100 or y=1000.
       * This resulted in "banding" because the same x positions had the same minima/maxima
       * on all vertical positions in the drawing.
       *
       * The same phenomenon was present in the y axis, so even if the y oscillator had
       * "different" banding, it still had banding which meant that the resulting distribution
       * would have right-angle bands and look like a plaid distribution.
       *
       * This approach is different, in a fairly simple way: we simply
       * - adjust the phase of the x-oscillator based on the y-position
       * - adjust the phase of the y-oscillator based on the x-position
       *
       * which means that the oscillator varies spatially in ways that are not the same
       * between x and y, because the oscillator is not "visibly periodic".
       * Note that if the oscillator was a pure wave function, the banding would still be present,
       * but because we are modulating the signal so heavily, simple phase offset allows us to
       * simulate a random output
       *
       * This is of course far too much code to do such a simple thing,
       * I'll need to think about what API makes the most sense for this.
       * I think the most user-friendly option would be if there was some sort of
       * "oscillator system" (e.g. a synth) that took both an x and y input and
       * did this kind of "magic" itself.
       */
      grid(0, width, stepSize, 0, height, stepSize) { x: Double, y: Double ->
        val phaseX = map(0.0, width.toDouble(), -PI, PI * 4.0, x)
        val phaseY = map(0.0, height.toDouble(), PI * -2.34, PI * 2.43, y)
        val oscX = Oscillator(width * 1.1, 1.0, ::cos, phaseY)
          .modulateAmplitude(Oscillator(width * 0.232, 0.099, ::cos, PI * 0.248))
          .modulateAmplitude(Oscillator(width * 0.32, 0.12, ::sin, PI * 0.3))
          .modulateFrequency(Oscillator(width * 0.17, 0.0019, ::sin, PI * 4.9))
          .modulatePhase(Oscillator(width * 0.19, 0.32, ::cos, PI* 1.2))
        val oscY = Oscillator(width * 1.1, 1.0, ::cos, phaseX)
          .modulateAmplitude(Oscillator(width * 0.232, 0.099, ::cos, PI * 0.248))
          .modulateAmplitude(Oscillator(width * 0.32, 0.12, ::sin, PI * 0.3))
          .modulateFrequency(Oscillator(width * 0.17, 0.0019, ::sin, PI * 4.9))
          .modulatePhase(Oscillator(width * 0.19, 0.32, ::cos, PI* 1.2))
        val output = oscX.output(x) * 0.5 + oscY.output(y) * 0.5
        val shade = map(-1.0, 1.0, 0.0, 1.0, output)
//        drawer.stroke = null
//        drawer.fill = ColorRGBa.WHITE.shade(shade)
//        drawer.rectangle(Rectangle(x, y, stepSize.toDouble(), stepSize.toDouble()))
      }

      grid(0, width, stepSize, 0, height, stepSize) { x: Double, y: Double ->
        drawer.contour(contour {
          var point = Vector2(x, y)
          moveTo(point)
          for (i in 0..30) {
            val phaseX = map(0.0, width.toDouble(), -PI, PI * 4.0, point.x)
            val phaseY = map(0.0, height.toDouble(), PI * -2.34, PI * 2.43, point.y)
            val oscX = Oscillator(width * 1.1, 1.0, ::cos, phaseY)
              .modulateAmplitude(Oscillator(width * 0.232, 0.099, ::cos, PI * 0.248))
              .modulateAmplitude(Oscillator(width * 0.32, 0.12, ::sin, PI * 0.3))
              .modulateFrequency(Oscillator(width * 0.17, 0.0019, ::sin, PI * 4.9))
              .modulatePhase(Oscillator(width * 0.19, 0.32, ::cos, PI* 1.2))
            // this can have identical params/modulation to oscX (except phase), but trying out new things here
            val oscY = Oscillator(width * 1.2, 1.0, ::cos, phaseX)
              .modulateAmplitude(Oscillator(width * 0.332, 0.123, ::cos, PI * 0.448))
              .modulateAmplitude(Oscillator(width * 0.29, 0.16, ::sin, PI * 0.5))
              .modulateFrequency(Oscillator(width * 0.12, 0.0009, ::sin, PI * 4.59))
              .modulatePhase(Oscillator(width * 0.28, 0.42, ::cos, PI* 1.52))
            val output = oscX.output(point.x) * 0.5 + oscY.output(point.y) * 0.5
            val angle = map(-1.75, 1.75, -PI, PI, output)
//            val angle = map(-1.0, 1.0, -PI, PI, simplex(seed, point / 100.0))
            point = point + Vector2(sin(angle), cos(angle))
            lineTo(point)
          }
        })
      }

      // woot, basic oscillator works
      val yOffset = height / 2.0
//      drawer.contour(contour {
//        moveTo(Vector2(0.0, yOffset))
//        for (x in 0..width) {
////        // TODO: this range should be normalized somehow
//          val y = map(-1.0, 1.0, height * -0.25, height * 0.25, osc.output(x.toDouble()))
//          lineTo(Vector2(x.toDouble(), y + yOffset))
//        }
//      })

      // simplex noise, for comparison
//      drawer.contour(contour {
//        moveTo(Vector2(0.0, yOffset))
//        for (x in 0..width) {
//          println("${simplex(seed, x.toDouble())}")
//          lineTo(x.toDouble(), simplex(seed, x.toDouble() / width.toDouble() * 9.0) * height * 0.25 + yOffset)
//        }
//      })

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
      }
    }
  }
}

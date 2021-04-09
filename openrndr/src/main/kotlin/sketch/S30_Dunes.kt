/**
 * Algorithm in a nutshell
 * 1. Generate an irregular path (FractalizedPath)
 * 2. For each position on the path, generate a large number of points
 *    Position of points is determined by a gaussian distribution from the path position
 * For each generated point:
 *   1. apply a color based on the distance from the path position
 *   2. apply opacity and size based on a "mixed noise" function
 *   3. Travel the point along a path determined by the "mixed noise" function for some period of time
 */
package sketch

import noise.perlinCurl
import noise.simplexCurl
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.TransformTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.perlin
import org.openrndr.extra.noise.perlinHermite
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.extras.color.palettes.colorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.math.map
import org.openrndr.math.mix
import org.openrndr.shape.Circle
import shape.FractalizedLine
import util.MixNoise
import util.MixableNoise
import util.timestamp
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1200
    height = 600
  }

  // Define mixable noise function
  fun generateMixable(width: Double, seed: Int, rng: Random): MixNoise {
    val scale1 = width * random(0.8, 1.05, rng)
    val epsilon1 = random(0.01, 0.5, rng)
    val noiseFn1 = { v: Vector2 -> simplexCurl(seed, v, epsilon1) }
    val noise1 = MixableNoise(
      scale = scale1,
      influenceScale = scale1 * 0.5,
      influenceRange = 0.1 to 0.75,
      noiseFn = noiseFn1,
      influenceNoiseFn = { v: Vector2 -> simplex(seed, v * 2.0) },
    )

    val offset2 = random(0.0, 2.0 * PI, rng)
    val noiseFn2 = { v: Vector2 ->
      val angle = map(-1.0, 1.0, -PI + offset2, PI + offset2, perlinHermite(seed, v.x, v.y, atan2(v.y, v.x) / offset2))
      Vector2(cos(angle), sin(angle))
    }
    val noise2 = MixableNoise(
      scale = width * random(0.2, 0.4, rng),
      noiseFn = noiseFn2,
      influenceRange = 0.2 to 0.75,
      influenceScale = width * random(0.025, 0.5, rng),
      influenceNoiseFn = { v: Vector2 -> simplex(seed, v).pow(2.0) },
      influenceNoiseFnRange = 0.0 to 1.0
    )

    val epsilon3 = random(0.01, 0.5, rng)
    val chance3 = random(0.0, 1.0, rng) < 0.5
    val offset3 = random(0.0, 2.0 * PI, rng)
    val noiseFn3 = if (chance3) {
      { v: Vector2 ->
        val angle = map(-1.0, 1.0, -PI + offset3, PI + offset3, simplex(seed, v))
        Vector2(cos(angle), sin(angle))
      }
    } else {
      { v: Vector2 -> perlinCurl(seed, v, epsilon3) }
    }
    val noise3 = MixableNoise(
      scale = width * random(0.035, 0.1, rng),
      noiseFn = noiseFn3,
      influenceRange = 0.1 to 0.75,
      influenceScale = width * random(0.05, 0.8, rng),
      influenceNoiseFn = { v: Vector2 -> perlin(seed, v).pow(2.0) },
      influenceNoiseFnRange = 0.0 to 1.0
    )

    // Doubtful this would ever be useful, but who knows ¯\_(ツ)_/¯
    // println("""
    //   noise1: $noise1
    //   noise2: $noise2
    //   noise3: $noise3
    // """.trimIndent())

    return MixNoise(listOf(noise1, noise2, noise3))
  }

  program {
    val canvasScale = 4.0
    val w = width * canvasScale
    val h = height * canvasScale
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    // This is out hi-res render target which we draw to, before scaling it for the screen "preview"
    val rt = renderTarget(w.toInt(), h.toInt(), multisample = BufferMultisample.Disabled) { // multisample requires some weird copying to another color buffer
      colorBuffer()
      depthBuffer()
    }

    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()

    // will render these in order, then produce random versions afterwards
    val seedList = listOf<Int>(
      // // These are some nice starter seeds!
      // 1629345285,
      // 1380165392,
      // 383392961,
      // 1099266297,
      // // This seed has an odd artifact where subtle rays trace to the origin
      // // It actually looks kind of interesting but also suggests a bug!
      // 1811205709,
      // // this particular seed looks way cool with sharpEdges = true
      // seed = 1392132200
    )

    if (seedList.isNotEmpty()) {
      seed = seedList[0]
    }

    var rng = Random(seed)

    val colors = listOf(
      ColorRGBa.fromHex("FCB264"), // orange
      ColorRGBa.fromHex("8DC0E2"), // blue
      ColorRGBa.fromHex("C4B2E1"), // purple
      ColorRGBa.fromHex("CEE4F2"), // light blue
      ColorRGBa.fromHex("FEE1C3"), // light orange
      ColorRGBa.fromHex("E9E2F4"), // light purple
    )

    // demoMode goes a lot faster.... approx 1 render per minute
    // non-demo mode is much slower, approx 1 render per 10 minutes!!! 🤯
    // Best approach: generate some images you like using demoMode, then render a final result with the same seed
    val demoMode = true
    val dotCount = if (demoMode) 100.0 else 1000.0
    val sizeRange = if (demoMode) 2.25 to 3.5 else 1.3 to 2.5

    val baseOpacity = 0.60

    // sharpEdges produces much sharper contrast between spatial areas.
    // This is because of the cross-over between -PI and PI;
    // when angles cross that threshold, they immediately jump from minimum to maximum values,
    // creating a sharp contrast.
    // A way around this is to use the absolute value of the resulting noise angle,
    // which makes it so that jumps between (originally) -PI and PI are now approximately adjacent, near +PI.
    // Of course, with this change, we must also adjust the ranges for size and opacityFactor
    val sharpEdges = false // could randomize, but I think no sharp edges is my preferences for this iteration

    extend {
      rng = Random(seed)
      println("seed = $seed")

      val pathStart = Vector2(0.0, random(0.0, h, rng))
      val pathEnd = Vector2(w, random(0.0, h, rng))
      val basePath = listOf(pathStart, Vector2(w * 0.5, h * 0.5), pathEnd)
      val path = FractalizedLine(basePath, rng)
        .perpendicularSubdivide(10).points

      val shuffledColors = colors.shuffled(rng).mapIndexed { index, color ->
        map(0.0, colors.size - 1.0, 0.1, 0.9, index.toDouble()) to color.toRGBa()
      }

      val spectrum = colorSequence(*shuffledColors.toTypedArray())

      val mixable = generateMixable(w, seed, rng)

      // travel the point along a flow field
      fun travel(point: Vector2): Vector2 {
        var newPoint = point.copy()
        // possible enhancement: variable travel distance???
        List(100) {
          newPoint += mixable.mix(newPoint)
        }
        return newPoint
      }

      // Render to the render target, then scale and draw to screen
      drawer.isolatedWithTarget(rt) {
        this.ortho(rt)
        this.stroke = null
        this.clear(ColorRGBa.BLACK)

        for (pos in path) {
          List((dotCount * canvasScale).toInt()) {
            val noise = simplex(seed, pos / w * 4.0) * 0.5 + 0.5
            val offset = map(0.0, 1.0, h * 0.15, h * 0.35, noise)
            var point = Vector2.gaussian(pos, Vector2(offset * 0.5, offset), rng)
            val spectrumIndex = mix(point.distanceTo(pos) / h * 2.0, noise, 0.67)

            // size and brightness clusters
            val sizeJitter = random(-0.7, 0.7, rng)
            val noiseVector = mixable.mix(point)

            val (noiseAngle, angleRange) = if (sharpEdges) {
              val noiseAngle = atan2(noiseVector.y, noiseVector.x)
              Pair(noiseAngle, -PI to PI)
            } else {
              val noiseAngle = abs(atan2(noiseVector.y, noiseVector.x))
              Pair(noiseAngle, 0.0 to PI)
            }

            val size = map(angleRange.first, angleRange.second, sizeRange.first, sizeRange.second, noiseAngle) +
              sizeJitter
            val opacityFactor = map(angleRange.first, angleRange.second, baseOpacity * 0.5, baseOpacity * 2.5, noiseAngle)

            val opacity = clamp(size * opacityFactor, 0.0, 1.0)

            point = travel(point)
            val circle = Circle(point, size)

            this.stroke = null
            this.fill = spectrum.index(spectrumIndex).opacify(opacity)
            this.circle(circle)
          }
        }
      }

      drawer.scale(width.toDouble() / rt.width, TransformTarget.MODEL)
      drawer.image(rt.colorBuffer(0))

      // wow... jpg is like 3x smaller than PNG without a substantial (perceptible) quality loss :thinking face:
      val targetFile = File("screenshots/$progName/${timestamp()}-pointCount-${path.size * (dotCount * canvasScale).toInt()}-sharpEdges-$sharpEdges-seed-$seed.jpg")
      targetFile.parentFile?.let { file ->
        if (!file.exists()) {
          file.mkdirs()
        }
      }
      rt.colorBuffer(0).saveToFile(targetFile, async = false)

      // if there are remaining seeds, advance in the list, otherwise generate
      if (seedList.isNotEmpty() && seedList.indexOf(seed) != seedList.size - 1) {
        seed = seedList[seedList.indexOf(seed) + 1]
      } else {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
      }
    }
  }
}

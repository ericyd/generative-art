/**
 * Algorithm in a nutshell
 * 1. Generate starting points for each flow line.
 *    Start lines are based on a grid, but each point is randomized by adding a gaussian offset to the grid position
 * 2. For each point, assign a flow field function (2 options) and assign it to a spatial bucket.
 *    Buckets are just sections of the grid - buckets are drawn together, to increase the changes
 *    that adjacent flow paths will travel further together and create more cohesive patterns.
 *    Also assign other core components like color and max length
 * 3. Shuffle the bucket order, and cycle through each.
 * 4. Shuffle the line order within each bucket
 * 5. For each line, draw it point-by-point based on the parameters assigned to the line
 * 6. You're done!
 */
package sketch.flow

import extensions.CustomScreenshots
import noise.perlinCurl
import noise.simplexCurl
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.noise.cubicHermite
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.perlin
import org.openrndr.extra.noise.perlinHermite
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.extras.color.palettes.ColorSequence
import org.openrndr.extras.color.palettes.colorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.map
import org.openrndr.shape.Rectangle
import util.MixNoise
import util.MixableNoise
import util.grid
import util.quantize
import util.timestamp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 800
    height = 800
  }

  // This is 100% copy/pasta from S30_Dunes - perhaps find a way to extract such a function?
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

  class FlowLine(var cursor: Vector2, var length: Int, val maxLength: Int, val field: (Vector2) -> Vector2, val color: ColorRGBa, val bounds: Rectangle) {
    var isComplete: Boolean = false

    // TODO: the first cursor is never drawn. Hmmm. An issue? Maybe not? Maybe add an init{} block that draws the first one?
    fun move(totalFilledPositions: MutableList<Vector2>, collisionDistance: Double, drawer: Drawer) {
      val noise = field(cursor).normalized * collisionDistance * 1.1
      val next = cursor + noise
      // If the position collides with points from other lines, exit the line.
      if (totalFilledPositions.any { it.distanceTo(next) < collisionDistance }) {
        isComplete = true
      } else {
        length++
        cursor = next
        totalFilledPositions.add(cursor)

        // only draw inside bounds
        // however, the line can continue moving, because it may circle back and re-enter the bounds at a later point
        if (bounds.contains(cursor)) {
          // drawer.contour(SimplexBlob.pointBlob(cursor, rng, blobRadiusRange))
          drawer.fill = color
          drawer.circle(cursor, collisionDistance * 0.75)
        }

        if (length >= maxLength) {
          isComplete = true
        }
      }
    }
  }

  fun generateSpectrum(colors: List<ColorRGBa>, rng: Random): ColorSequence {
    val shuffledColors = colors.shuffled(rng).mapIndexed { index, color ->
      map(0.0, colors.size - 1.0, 0.1, 0.9, index.toDouble()) to color.toRGBa()
    }

    return colorSequence(*shuffledColors.toTypedArray())
  }

  program {
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()

    // nice seeds go here 🙃
    // seed = 1034215449
    // seed = 247798497
    // seed = 600571359
    seed = 931353732

    println("seed = $seed")
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    val screenshots = extend(CustomScreenshots()) {
      quitAfterScreenshot = false
      scale = 3.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
    }

    // backgroundColor = ColorRGBa.GHOST_WHITE
    backgroundColor = ColorRGBa.fromHex("d8cfc3")

    extend {
      drawer.stroke = null
      val rng = Random(seed)

      val distributionNoiseScale = random(width * 0.7, width * 1.1, rng)

      val colors = listOf(
        ColorRGBa.fromHex("c6463d"),
        ColorRGBa.fromHex("a04606"),
        ColorRGBa.fromHex("efa134"),
        ColorRGBa.fromHex("465e54"),
        ColorRGBa.fromHex("1e4547"),
        ColorRGBa.fromHex("5483a3"),
        ColorRGBa.fromHex("524368"),
        ColorRGBa.fromHex("562546"),
      )

      val seed1 = seed / 2
      val mixable = generateMixable(width.toDouble(), seed1, Random(seed1))
      val flowFieldFn1 = { v: Vector2 ->
        mixable.mix(v)
      }
      val spectrum1 = generateSpectrum(colors, rng)

      val noiseScale2 = random(width * 0.4, width * 0.8, rng)
      val flowFieldFn2 = { v: Vector2 ->
        val noise = cubicHermite(seed * 2, Vector3(v.x, v.y, atan2(v.y, v.x)) / noiseScale2)
        val angle = map(-1.0, 1.0, -PI, PI, noise)
        Vector2(cos(angle), sin(angle))
      }
      val spectrum2 = generateSpectrum(colors, rng)

      // how long the flow line will continue (assuming no collisions)
      val meanLineLength = random(300.0, 700.0, rng)
      // this is the minimum distance points must be apart in order to be drawn
      val collisionDistance = 6.0

      val totalFilledPositions = mutableListOf<Vector2>()

      val stepSize = 10

      val gridSize = 5
      val buckets = List(gridSize * gridSize) {
        mutableListOf<FlowLine>()
      }

      val bucketIndex = { x: Double, y: Double ->
        y.toInt() * gridSize + x.toInt()
      }

      // draw lines
      val gridScale = -0.1 // matted
      // val gridScale = 0.2 // full bleed
      val w = width * (1.0 + gridScale * 2.0)
      val h = height * (1.0 + gridScale * 2.0)
      val quantumX = w / (gridSize - 1.0)
      val quantumY = h / (gridSize - 1.0)
      val bounds = Rectangle(
        x = width * -gridScale,
        y = height * -gridScale,
        width = w,
        height = h,
      )
      grid(
        rect = bounds,
        stepSize
      ) { v: Vector2 ->
        val cursor = Vector2.gaussian(v, Vector2(stepSize * 0.5), rng)

        val lineLength = random(meanLineLength * 0.75, meanLineLength * 1.25, rng).toInt()

        val bucketX = quantize(quantumX, cursor.x + (width * gridScale)) / quantumX
        val bucketY = quantize(quantumY, cursor.y + (height * gridScale)) / quantumY

        val spectrumIndex = simplex(seed, v / distributionNoiseScale * 0.5).pow(2.0)

        val line = if (simplex(seed, v / distributionNoiseScale) < 0.0) {
          FlowLine(cursor, 1, lineLength, flowFieldFn1, spectrum1.index(spectrumIndex), bounds)
        } else {
          FlowLine(cursor, 1, lineLength, flowFieldFn2, spectrum2.index(spectrumIndex), bounds)
        }
        buckets[bucketIndex(bucketX, bucketY)].add(line)
      }

      for (bucket in buckets.shuffled(rng)) {
        for (line in bucket.shuffled(rng)) {
          while (!line.isComplete) {
            line.move(totalFilledPositions, collisionDistance, drawer)
          }
        }
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
      }
    }
  }
}

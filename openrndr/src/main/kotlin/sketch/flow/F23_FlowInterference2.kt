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
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.extras.color.palettes.ColorSequence
import org.openrndr.extras.color.palettes.colorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Rectangle
import util.grid
import util.quantize
import util.rotatePoint
import util.timestamp
import kotlin.math.PI
import kotlin.random.Random

fun main() = application {
  configure {
    width = 800
    height = 800
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
          // this is very annoying... OPENRNDR 0.47 doesn't render radiuses the same (they are larger)
          // but, the new version of IntelliJ isn't able to download the old version I was using...
          // not sure what to do for old pieces...
          drawer.circle(cursor, collisionDistance * 0.6)
        }

        if (length >= maxLength) {
          isComplete = true
        }
      }
    }
  }

  fun generateSpectrum(colors: List<ColorRGBa>, rng: Random? = null): ColorSequence {
    val baseColors = if (rng != null) colors.shuffled(rng) else colors
    val shuffledColors = baseColors.mapIndexed { index, color ->
      map(0.0, colors.size - 1.0, 0.1, 0.9, index.toDouble()) to color.toRGBa()
    }

    return colorSequence(*shuffledColors.toTypedArray())
  }

  program {
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()

    // nice seeds go here 🙃
    seed = 1057241548

    println("seed = $seed")
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    val screenshots = extend(CustomScreenshots()) {
      quitAfterScreenshot = false
      scale = 3.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
    }

    backgroundColor = ColorRGBa.fromHex("202956")

    extend {
      drawer.stroke = null
      val rng = Random(seed)

      val distributionNoiseScale = random(width * 0.4, width * 0.9, rng)

      val colors = listOf(
        ColorRGBa.fromHex("3d0901"),
        ColorRGBa.fromHex("c62b2b"),
        ColorRGBa.fromHex("873310"),
        ColorRGBa.fromHex("f78538"),
        // ColorRGBa.fromHex("dda221"),
        ColorRGBa.fromHex("1c5c77"),
        ColorRGBa.fromHex("2d5fb5"),
        ColorRGBa.fromHex("4f55a5"),
        ColorRGBa.fromHex("452f6d"),
        ColorRGBa.fromHex("f1def4"),
      )

      val spectrum = generateSpectrum(colors)

      drawer.clear(spectrum.index(random(0.0, 1.0, rng)))

      val rotationAngle = PI / 35.0
      val flowFieldFn1 = { v: Vector2 ->
        // the `- v` is necessary because the flow field must return the difference to travel
        rotatePoint(v, rotationAngle, about = Vector2.ZERO) - v
      }

      val circleCenter = Vector2(width * 0.5, height.toDouble())
      val flowFieldFn2 = { v: Vector2 ->
        rotatePoint(v, rotationAngle, about = circleCenter) - v
      }

      // how long the flow line will continue (assuming no collisions)
      val meanLineLength = random(100.0, 500.0, rng)
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

        val spectrumIndex = (cursor.distanceTo(circleCenter) / Vector2.ZERO.distanceTo(circleCenter)) +
          random(-0.2, 0.2, rng)

        val line = if (simplex(seed, v / distributionNoiseScale) < 0.0) {
          FlowLine(cursor, 1, lineLength, flowFieldFn1, spectrum.index(spectrumIndex), bounds)
        } else {
          FlowLine(cursor, 1, lineLength, flowFieldFn2, spectrum.index(spectrumIndex), bounds)
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

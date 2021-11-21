/**
 * Algorithm in a nutshell
 *
 * Generating dots
 * 1. Generate ~60-100 "circle parameters": a center point and a radius
 * 2. For each circle, loop through each discrete radius that will not overlap its neighbors, based on the set collision distance
 * 3. Find a random angle for that radius, and create a "flow line"
 * 4. For each flow line, and draw all the points in the flow line
 *    (a flow line is just a collection of dots along the arc of the radius),
 *    stopping if any collisions occur with other points
 * 5. Repeat steps 2-4 several times to build up a "layered" effect
 *
 * Generating colors
 * 1. Define two list of colors. In OPENRNDR we use the ColorSequece to group these color lists into easily-selectable color spectrums
 * 2. When each flow line is generated (step 3 above), assign a spectrum function to the flow line
 *    One of two spectrum functions are available. The choice is made based on the distance of the circle center
 *    to the distance of two randomly generated "spectrum centers" (with a little noise for good measure).
 *    Each spectrum function utilizes a different spectrum from step 1
 * 3. When each point is drawn, it's position is fed into the spectrum function to determine the color assignment.
 *    The logic varies but it's typically a combination of noise based on the spatial position of the dot,
 *    and possibly the distance of the dot to one of the "spectrum centers"
 * 4. The spectrum index (a number between 0-1 which grabs a discrete color from the spectrum) is jittered a little bit for visual interest.
 * 5. In 10% of the dots, the shade is randomly lightened for visual interest
 */
package sketch.flow

import noise.perlinCurl
import noise.simplexCurl
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.cubicHermite
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.perlin
import org.openrndr.extra.noise.perlinHermite
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.extras.color.palettes.ColorSequence
import org.openrndr.extras.color.palettes.colorSequence
import org.openrndr.extras.color.presets.WHITE_SMOKE
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.clamp
import org.openrndr.math.map
import org.openrndr.shape.Rectangle
import shape.SimplexBlob
import util.MixNoise
import util.MixableNoise
import util.grid
import util.quantize
import util.timestamp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 800
    height = 1000
  }

  class FlowLine(var cursor: Vector2, val maxLength: Int, val field: (Vector2) -> Vector2, val color: ColorRGBa, val bounds: Rectangle, val rng: Random) {
    var length = 1
    var isComplete: Boolean = false
    val currentLine = mutableListOf(cursor)

    // TODO: the first cursor is never drawn. Hmmm. An issue? Maybe not? Maybe add an init{} block that draws the first one?
    fun move(totalFilledPositions: MutableList<Vector2>, collisionDistance: Double, drawer: Drawer) {
      val noise = field(cursor)
      cursor += noise
      length++

      if (length >= maxLength) {
        isComplete = true
        return
      }

      // avoid collisions with current line
      if (currentLine.any { it.distanceTo(cursor) < collisionDistance }) {
        return
      }

      // If the position collides with points from other lines, exit the line.
      if (totalFilledPositions.any { it.distanceTo(cursor) < collisionDistance }) {
        isComplete = true
        return
      }

      // if no intersections with current line, and in bounds, draw it
      if (bounds.contains(cursor)) {
        totalFilledPositions.add(cursor)
        currentLine.add(cursor)

        // wow... this makes for a convincing sphere effect but it's not the most attractive
        // drawer.shadeStyle = RadialGradient(
        //   color0 = color,
        //   color1 = color.shade(0.9),
        //   offset = Vector2(0.67),
        //   rotation = PI / 4.0,
        //   length = 0.25, // longer length makes gradient shrink...
        // )
        drawer.fill = color
        // drawer.circle(cursor, collisionDistance * 0.55)
        drawer.contour(SimplexBlob.pointBlob(cursor, rng, collisionDistance * 0.25 to collisionDistance * 0.55))
      }
    }
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

    return MixNoise(listOf(noise1, noise2, noise3))
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

    // good seeds go here 🙃
    seed = 682136510
    seed = 180286083

    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 4.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
    }

    extend {
      val rng = Random(seed)
      println("seed = $seed")

      drawer.stroke = null

      //
      // Drawing params
      //
      val numberOfLayers = 20
      // how long the flow line will continue (assuming no collisions)
      val meanLineLength = random(100.0, 300.0, rng)
      // this is the minimum distance points must be apart in order to be drawn
      val collisionDistance = 8.0

      val distributionNoiseScale = random(width * 0.6, width * 1.3, rng)

      //
      // Drawing bounds
      //
      val gridScale = -0.05 // matted
      // val gridScale = 0.2 // full bleed
      val w = width * (1.0 + gridScale * 2.0)
      val h = height * (1.0 + gridScale * 2.0)
      val bounds = Rectangle(
        x = width * -gridScale,
        y = height * -gridScale,
        width = w,
        height = h,
      )
      val gridSize = 5
      val quantumX = w / (gridSize - 1.0)
      val quantumY = h / (gridSize - 1.0)

      //
      // Colors
      // lots going on here
      //
      val spectrum1 = generateSpectrum(
        listOf(
          ColorRGBa.fromHex("ede9e8"),
          ColorRGBa.fromHex("dbd7d6"),
          ColorRGBa.fromHex("c1bbb4"),
          ColorRGBa.fromHex("141313"),
          ColorRGBa.fromHex("75625c"),
          ColorRGBa.fromHex("421f1f"),
        ),
        rng
      )

      val spectrum2 = generateSpectrum(
        listOf(
          ColorRGBa.fromHex("5d3e77"),
          ColorRGBa.fromHex("7697d3"),
          ColorRGBa.fromHex("5b80af"),
          ColorRGBa.fromHex("514966"),
          ColorRGBa.fromHex("8c75ad"),
          ColorRGBa.fromHex("d38797"),
        ),
        rng
      )

      val spectrumChance = random(0.0, 1.0, rng)
      val spectrumCenter = if (spectrumChance < 0.25) {
        Vector2(0.0, random(0.0, height.toDouble(), rng))
      } else if (spectrumChance < 0.5) {
        Vector2(width.toDouble(), random(0.0, height.toDouble(), rng))
      } else if (spectrumChance < 0.75) {
        Vector2(random(0.0, width.toDouble(), rng), 0.0)
      } else {
        Vector2(random(0.0, width.toDouble(), rng), height.toDouble())
      }

      val spectrumCenter2angle = random(-PI, PI, rng)
      val spectrumCenter2dist = random(hypot(w, h) * 0.25, hypot(w, h), rng)
      val spectrumCenter2 = Vector2(cos(spectrumCenter2angle), sin(spectrumCenter2angle)) * spectrumCenter2dist + Vector2(w, h) * 0.5

      val seed1 = seed / 2
      val mixable = generateMixable(width.toDouble(), seed1, Random(seed1))
      val flowFieldFn1 = { v: Vector2 ->
        mixable.mix(v)
      }

      val noiseScale2 = random(width * 0.4, width * 0.8, rng)
      val flowFieldFn2 = { v: Vector2 ->
        val noise = cubicHermite(seed * 2, Vector3(v.x, v.y, atan2(v.y, v.x)) / noiseScale2)
        val angle = map(-1.0, 1.0, -PI, PI, noise)
        Vector2(cos(angle), sin(angle))
      }

      drawer.clear(ColorRGBa.WHITE_SMOKE)

      //
      // generate "FlowLines"
      //
      for (n in 0..numberOfLayers) {
        val totalFilledPositions = mutableListOf<Vector2>()

        val buckets = List(gridSize * gridSize) {
          mutableListOf<FlowLine>()
        }
        val bucketIndex = { x: Double, y: Double ->
          y.toInt() * gridSize + x.toInt()
        }

        val stepSize = 2
        grid(
          rect = bounds,
          stepSize
        ) { v: Vector2 ->
          val cursor = Vector2.gaussian(v, Vector2(stepSize * 0.5), rng)

          val lineLength = random(meanLineLength * 0.75, meanLineLength * 1.25, rng).toInt()

          val dist1 = cursor.distanceTo(spectrumCenter)
          val dist2 = cursor.distanceTo(spectrumCenter2)

          val line = if (dist1 * (1.0 + simplex(seed, cursor / distributionNoiseScale) * 0.4) < dist2) {
            val dist = v.distanceTo(spectrumCenter)

            val distPct = dist / hypot(width.toDouble(), height.toDouble())
            val spectrumIndex = distPct + (simplex(seed, v / distributionNoiseScale * 0.5) + 1.0).pow(1.4) / 2.325 / 3.0 + // 3.0 is only necessary because this is adjusting the distPct
              random(-0.05, 0.05, rng)
            var color = spectrum1.index(spectrumIndex)

            if (random(0.0, 1.0, rng) < 0.1) {
              val lightened = color.toHSLa().shade(random(0.5, 2.5, rng))
              color = lightened.invoke(l = clamp(lightened.l, color.luminance, 0.9)).toRGBa()
            }
            FlowLine(cursor, lineLength, flowFieldFn1, color, bounds, rng)
          } else {
            // this bizarre adjustment ended up being a personal preference after testing logarithmic (base10 and base e), exponential, linear shift, and absolute value adjustments
            val spectrumIndex = (simplex(seed, v / distributionNoiseScale * 0.5) + 1.0).pow(1.4) / 2.325 +
              random(-0.05, 0.05, rng)
            var color = spectrum2.index(spectrumIndex)

            if (random(0.0, 1.0, rng) < 0.1) {
              val lightened = color.toHSLa().shade(random(0.5, 2.5, rng))
              color = lightened.invoke(l = clamp(lightened.l, color.luminance, 0.9)).toRGBa()
            }
            FlowLine(cursor, lineLength, flowFieldFn2, color, bounds, rng)
          }

          val bucketX = quantize(quantumX, cursor.x + (width * gridScale)) / quantumX
          val bucketY = quantize(quantumY, cursor.y + (height * gridScale)) / quantumY
          buckets[bucketIndex(bucketX, bucketY)].add(line)
        }

        //
        // Draw flow lines
        //
        for (bucket in buckets.shuffled(rng)) {
          for (line in bucket.shuffled(rng)) {
            while (!line.isComplete) {
              line.move(totalFilledPositions, collisionDistance, drawer)
            }
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

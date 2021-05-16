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

import extensions.CustomScreenshots
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.hsla
import org.openrndr.draw.Drawer
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.extras.color.palettes.ColorSequence
import org.openrndr.extras.color.palettes.colorSequence
import org.openrndr.extras.color.presets.WHITE_SMOKE
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.math.map
import org.openrndr.shape.Rectangle
import util.quantize
import util.rotatePoint
import util.timestamp
import java.util.*
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 800
    height = 1000
  }

  data class CircleParams(val center: Vector2, val size: Int)

  class FlowLine(var cursor: Vector2, val maxLength: Int, val field: (Vector2) -> Vector2, val spectrumFn: (Vector2) -> ColorRGBa, val bounds: Rectangle) {
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

      //
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
        // drawer.contour(SimplexBlob.pointBlob(cursor, rng, blobRadiusRange))
        val color = spectrumFn(cursor)
        drawer.stroke = color.shade(0.6)
        // would be nice to randomly increase brightness with a low-probability chance
        drawer.fill = color
        drawer.circle(cursor, collisionDistance * 0.55)
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

    // good seeds go here 🙃
    // seed = 210792187
    // seed = 2062832415
    // seed = 700272972
    // seed = 1415266582
    // seed = 599353856

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

      //
      // Drawing params
      //
      val numberOfLayers = 20
      // how long the flow line will continue (assuming no collisions)
      val meanLineLength = random(100.0, 300.0, rng)
      // this is the minimum distance points must be apart in order to be drawn
      val collisionDistance = 8.0

      //
      // Drawing bounds
      //
      val boundsPct = 0.2
      val gridScale = -0.1 // matted
      // val gridScale = 0.2 // full bleed
      val w = width * (1.0 + gridScale * 2.0)
      val h = height * (1.0 + gridScale * 2.0)
      val bounds = Rectangle(
        x = width * -gridScale,
        y = height * -gridScale,
        width = w,
        height = h,
      )

      //
      // Circle params
      //
      val numberOfCircles = random(60.0, 100.0, rng).toInt()
      val circleParams = List(numberOfCircles) {
        CircleParams(
          Vector2(
            random(width * -boundsPct, width * (1.0 + boundsPct), rng),
            random(height * -boundsPct, height * (1.0 + boundsPct), rng),
          ),
          random(20.0, 60.0, rng).toInt()
        )
      }

      val distributionNoiseScale = random(width * 0.6, width * 1.3, rng)

      //
      // COLORS
      // Wow this ended up being way more work that I expected
      //
      val spectrum1 = generateSpectrum(listOf(
        ColorRGBa.fromHex("dfe2e0"), // off-white
        ColorRGBa.fromHex("67341e"), // brown
        ColorRGBa.fromHex("e07024"), // light orange
        ColorRGBa.fromHex("4f6459"), // gray-green
        ColorRGBa.fromHex("cce2dd"), // light minty
        ColorRGBa.fromHex("3e5468"), // gray-blue
        ColorRGBa.fromHex("cad7ed"), // light-blue
      ))

      val spectrum2 = generateSpectrum(listOf(
        ColorRGBa.fromHex("b4cec8"), // light minty (modified from cce2dd)
        ColorRGBa.fromHex("394856"), // gray-blue (modified from 3e5468)
        ColorRGBa.fromHex("9eadc6"), // light-blue (modified from cad7ed)
        ColorRGBa.fromHex("605880"), // purple-y
      ))

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

      // Two paradigms:
      // 1. Decide color spectrum inside the spectrumFn, which results in sharper lines
      // 2. Make two discrete spectrumFns that use different spectrums, and assign to the FlowLine
      // It took a damn while, but I believe I prefer #2
      //
      // val spectrumFn = { v: Vector2 ->
      //   val dist1 = v.distanceTo(spectrumCenter)
      //   val dist2 = v.distanceTo(spectrumCenter2)
      //
      //   val color = if (dist1 * (1.0 + simplex(seed, v / distributionNoiseScale) * 0.4) < dist2) {
      //     val distPct = dist1 / hypot(width.toDouble(), height.toDouble())
      //     val spectrumIndex = distPct + (simplex(seed, v / distributionNoiseScale * 0.5) + 1.0).pow(1.4) / 2.325 / 3.0 + // 3.0 is only necessary because this is adjusting the distPct
      //       random(-0.05, 0.05, rng)
      //     spectrum1.index(spectrumIndex)
      //   } else {
      //     // this bizarre adjustment ended up being a personal preference after testing logarithmic (base10 and base e), exponential, linear shift, and absolute value adjustments
      //     val spectrumIndex = (simplex(seed, v / distributionNoiseScale * 0.5) + 1.0).pow(1.4) / 2.325 +
      //       random(-0.05, 0.05, rng)
      //     spectrum2.index(spectrumIndex)
      //   }
      //
      //   if (random(0.0, 1.0, rng) < 0.1) {
      //     color.shade(random(0.5, 2.5, rng))
      //   } else {
      //     color
      //   }
      // }

      val spectrumFn1 = { v: Vector2 ->
        val dist = v.distanceTo(spectrumCenter)

        val distPct = dist / hypot(width.toDouble(), height.toDouble())
        val spectrumIndex = distPct + (simplex(seed, v / distributionNoiseScale * 0.5) + 1.0).pow(1.4) / 2.325 / 3.0 + // 3.0 is only necessary because this is adjusting the distPct
          random(-0.05, 0.05, rng)
        val color = spectrum1.index(spectrumIndex)

        if (random(0.0, 1.0, rng) < 0.1) {
          val lightened = color.toHSLa().shade(random(0.5, 2.5, rng))
          lightened.invoke(l = clamp(lightened.l, color.luminance, 0.9)).toRGBa()
        } else {
          color
        }
      }

      val spectrumFn2 = { v: Vector2 ->
        // this bizarre adjustment ended up being a personal preference after testing logarithmic (base10 and base e), exponential, linear shift, and absolute value adjustments
        val spectrumIndex = (simplex(seed, v / distributionNoiseScale * 0.5) + 1.0).pow(1.4) / 2.325 +
          random(-0.05, 0.05, rng)
        val color = spectrum2.index(spectrumIndex)

        if (random(0.0, 1.0, rng) < 0.1) {
          val lightened = color.toHSLa().shade(random(0.5, 2.5, rng))
          lightened.invoke(l = clamp(lightened.l, color.luminance, 0.9)).toRGBa()
        } else {
          color
        }
      }

      drawer.clear(ColorRGBa.WHITE_SMOKE)

      //
      // generate "FlowLines"
      //
      for (n in 0..numberOfLayers) {
        val totalFilledPositions = mutableListOf<Vector2>()

        val lines = mutableListOf<FlowLine>()

        // draw lines
        for (params in circleParams) {
          val circleOffset = random(-collisionDistance, collisionDistance, rng)
          // start point is based on random angle + radius
          for (r in 1..params.size) {
            val angle = random(-PI, PI, rng)
            val radius = r * collisionDistance + circleOffset
            val cursor = Vector2(cos(angle), sin(angle)) * radius + params.center

            val rotationAngle = if(random(0.0, 1.0, rng) < 0.5) PI / 100.0 else -PI / 100.0
            val flowFieldFn = { v: Vector2 ->
              rotatePoint(v, rotationAngle, about = params.center) - v
            }

            val lineLength = random(meanLineLength * 0.75, meanLineLength * 1.25, rng).toInt()

            // val line = FlowLine(cursor, lineLength, flowFieldFn, spectrumFn, bounds)

            val dist1 = params.center.distanceTo(spectrumCenter)
            val dist2 = params.center.distanceTo(spectrumCenter2)
            val line = if (dist1 * (1.0 + simplex(seed, cursor / distributionNoiseScale) * 0.4) < dist2) {
              FlowLine(cursor, lineLength, flowFieldFn, spectrumFn1, bounds)
            } else {
              FlowLine(cursor, lineLength, flowFieldFn, spectrumFn2, bounds)
            }

            lines.add(line)
          }

          //
          // Draw flow lines
          //
          for (line in lines) {
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

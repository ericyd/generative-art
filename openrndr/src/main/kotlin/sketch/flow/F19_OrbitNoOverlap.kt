/**
 * Create new works here, then move to parent package when complete
 */
package sketch.flow

import extensions.CustomScreenshots
import force.GravityBody
import force.GravitySystem
import force.PhysicalBody
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.hsla
import org.openrndr.extra.noise.random
import org.openrndr.extras.color.palettes.colorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.contour
import shape.SimplexBlob
import util.timestamp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1200
    height = 800
  }

  program {
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed.toLong()))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()

    // some nice seeds 😊
    // seed = 1859366810
    // seed = 1319521965
    // seed = 458649401

    println("seed = $seed")
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    val screenshots = extend(CustomScreenshots()) {
      quitAfterScreenshot = false
      scale = 4.0
      captureEveryFrame = false
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }

    val center = Vector2(width / 2.0, height / 2.0)

    val bg = ColorRGBa(0.03, 0.03, 0.03, 1.0)
    val opacity = 0.7
    backgroundColor = bg

    extend {
      drawer.stroke = null

      val rng = Random(seed)

      // number of gravitational bodies to create in our gravity system
      val nBodies = random(1.0, 7.0, rng).toInt()
      // total number of flow lines to draw
      val nLines = random(500.0, 1050.0, rng).toInt()
      // how fast a body will travel (affects how much it is influenced by the gravity of the gravitational bodies)
      val bodySpeed = random(0.05, 0.1, rng)
      val scale = random(1.5, 3.5, rng)
      // how long the flow line will continue (assuming no collisions)
      val meanLineLength = random(100.0, 300.0, rng)
      // the flow line start position is put on a radius from the center, with minRadius and maxRadius defining the bounds
      val maxRadius = hypot(width / 2.0, height / 2.0) * random(0.6, 1.3, rng)
      val minRadius = 0.0
      // randomization amount of the start angle for the flow line
      val angleVariation = random(PI / 16.0, PI / 4.0, rng)
      // this is the minimum distance points must be apart in order to be drawn
      val collisionDistance = 8.0

      val gravityBodies: List<GravityBody> = List(nBodies) {
        GravityBody(
          x = random(width * -0.75, width * 1.75, rng),
          y = random(height * -0.75, height * 1.75, rng),
          mass = random(400.0, 1000.0, rng),
          rand = rng
        )
      }

      // this calculates net forces on a point mass
      val system = GravitySystem(2.0, gravityBodies)

      val colors = listOf(
        hsla(261.0, 0.45, 0.43, opacity), // purple
        hsla(212.0, 0.67, 0.30, opacity), // dark blue
        hsla(194.0, 0.70, 0.85, opacity), // light blue
        hsla(10.0, 0.40, 0.15, opacity), // dark brown
        hsla(255.0, 0.46, 0.86, opacity), // light purple
        hsla(173.0, 0.66, 0.975, opacity), // smokey white
        hsla(29.0, 0.93, 0.83, opacity) // orange/salmon
      )

      val shuffledColors = colors.shuffled(rng).mapIndexed { index, color ->
        map(0.0, colors.size - 1.0, 0.1, 0.9, index.toDouble()) to color.toRGBa()
      }

      val spectrum = colorSequence(*shuffledColors.toTypedArray())

      val filledPositions = mutableListOf<Vector2>()

      // draw lines
      for (i in 0 until nLines) {
        // We start our lines (PhysicalBody instances) radially around the center of the image
        // with slightly randomized angle and radius from the center
        val angle = map(0.0, nLines.toDouble(), 0.0, 2.0 * PI, i.toDouble())
        val randomizedAngle = random(angle - angleVariation, angle + angleVariation, rng)
        val randomizedRadius = random(minRadius, maxRadius, rng)
        val body = PhysicalBody(
          center + Vector2(cos(randomizedAngle) * randomizedRadius, sin(randomizedAngle) * randomizedRadius),
          mass = random(205.0, 315.0, rng),
          speed = bodySpeed
        )

        // Each particle trace ("line") get it's own unique length, and we grab the color from the spectrum
        val lineLength = random(meanLineLength * 0.75, meanLineLength * 1.25, rng).toInt()
        drawer.fill = spectrum.index(i.toDouble() / nLines)

        // The particle trace follows an orbital pattern defined by the gravity system and the physical body.
        // We use simple check to see if there are any overlaps,
        // and if there are then we close the contour
        try {
          contour {
            moveTo(body.coords)
            for (j in 0 until lineLength) {
              val nextPosition = body.orbit(system, scale, center)
              if (filledPositions.any { it.distanceTo(nextPosition) < collisionDistance }) {
                break
              } else {
                lineTo(nextPosition)
              }
            }
          }.segments
            .filterIndexed { i, _s -> i % 12 == 0 }
            .forEach {
              filledPositions.add(it.start)
              drawer.contour(SimplexBlob.pointBlob(it.start, rng))
            }
        } catch (e: NoSuchElementException) {
          // if a contour can't draw more than a single point it will throw an error,
          // but we don't really care, we can just ignore it
        }
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

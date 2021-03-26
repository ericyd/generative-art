/**
 * Create new works here, then move to parent package when complete
 */
package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.hsl
import org.openrndr.color.hsla
import org.openrndr.draw.isolated
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.drawComposition
import org.openrndr.shape.intersection
import shape.SimplexBlob
import util.timestamp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 1000
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    val w = width.toDouble()
    val h = height.toDouble()

    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    seed = 48062633
    println("seed = $seed")

    // Alternatively, use Screenshots built-in
    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 3.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }

    val center = Vector2(w * 0.5, h * 0.5)
    val circleRadius = hypot(w, h) * 0.25
    val circle = Circle(center, circleRadius)

    backgroundColor = ColorRGBa.WHITE

    extend {
      val rng = Random(seed)
      val strokeWeight = random(0.5, 02.0, rng) // smaller is actually better! could even go 0.1-0.9

      // blobs / mottled background
      for (i in 0..5000) {
        val c = SimplexBlob(
          origin = Vector2(random(w * -0.5, w * 1.5, rng), random(h * -0.5, h * 1.5, rng)),
          radius = random(50.0, 150.0, rng),
          seed = seed,
          fuzziness = 0.2
        ).contour()
        drawer.isolated {
          stroke = null
          fill = hsla(0.0, 0.0, 0.0, 0.045).toRGBa()
          contour(c)
        }
      }

      // circles in circle
      val angle = random(-PI, PI, rng)
      val distance = random(circleRadius * 0.9, circleRadius * 1.1, rng)
      val offset = Vector2(cos(angle), sin(angle)) * distance + center
      val noiseScale = w * random(0.25, 0.55, rng)
      val composition = drawComposition {
        this.strokeWeight = strokeWeight
        for (i in 1..(width * 2)) {
          val radius = i * strokeWeight
          val shade = map(-1.0, 1.0, 0.0, 0.8, simplex(seed, (offset + radius) / noiseScale)) + random(-0.1, 0.1, rng)
          this.stroke = hsl(0.0, 0.0, shade).toRGBa()
          val partialCircle = Circle(offset, radius)
          shape(intersection(circle.shape, partialCircle.shape))
        }
        this.stroke = ColorRGBa.BLACK
        this.strokeWeight = strokeWeight * 1.5
        circle(circle)
      }
      drawer.composition(composition)
      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

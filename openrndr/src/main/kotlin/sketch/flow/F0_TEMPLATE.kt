/**
 * Flow field template
 * ===================
 * It turns out I like flow fields a lot.
 * Some people call these particle flows or particle fields or other things.
 * Those all make sense, because essentially it is the result of tracing a point
 * in space over time as it moves through some vector field.
 * I still call it flow field because I learned the technique from the inimitable Tyler Hobbes
 * and he uses the term "flow field", so I shall do the same.
 *
 * This is a dead simple implementation which is meant to be extended,
 * but shows the basic mechanics of drawing a flow field.
 */
package sketch.flow

import extensions.CustomScreenshots
import noise.curl
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import util.grid
import util.timestamp
import kotlin.random.Random

fun main() = application {
  configure {
    width = 800
    height = 800
  }

  program {
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    val screenshots = extend(CustomScreenshots()) {
      quitAfterScreenshot = false
      scale = 3.0
      captureEveryFrame = false
      name = "screenshots/${progName}/${timestamp()}-seed-${seed}.png"
    }

    backgroundColor = ColorRGBa.WHITE

    val rand = Random(seed)

    // a few properties for the sketch;
    // These don't need to be declared separately but it's nice to have them in one place for tweakability
    val stepSize = 5
    val jitter = stepSize * 0.7
    val lineLength = 100
    val opacity = 0.05
    val bounds = width / 2
    val noiseScale = 650.0

    // This can be any function that returns a vector2, to determine where the line goes next
    fun nextPosition(cursor: Vector2): Vector2 {
      // This is the most classic flow field implementation, but not terribly exciting
      //   val angle = map(-1.0, 1.0, -PI, PI, simplex(seed, cursor.xy0 / noiseScale))
      //   return cursor + Vector2(cos(angle), sin(angle))
      return cursor + curl(::simplex, seed, cursor / noiseScale, 0.01)
    }

    val contours: List<ShapeContour> = grid(-bounds, width + bounds, stepSize, -bounds, height + bounds, stepSize) { x: Double, y: Double ->
      contour {
        moveTo(
          x + random(-jitter, jitter, rand),
          y + random(-jitter, jitter, rand)
        )

        List(lineLength) {
          lineTo(nextPosition(cursor))
        }
      }
    }

    println("aww yeah, about to render...")
    extend {
      // simple B&W
      drawer.fill = null
      drawer.stroke = ColorRGBa.BLACK.opacify(opacity)
      drawer.strokeWeight = 1.0
      drawer.lineCap = LineCap.ROUND

      // Sometimes trying to draw too many contours at once causes issues
      contours.chunked(500).forEach { drawer.contours(it) }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/${progName}/${timestamp()}-seed-${seed}.png"
      }
    }
  }
}

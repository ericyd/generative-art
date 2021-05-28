package sketch.test

import force.MovingBody
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.drawComposition
import org.openrndr.shape.intersection
import org.openrndr.shape.union
import org.openrndr.shape.difference
import shape.FractalizedLine
import shape.differentialLine
import util.timestamp
import kotlin.math.PI
import kotlin.math.absoluteValue
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

    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")
    val rng = Random(seed)

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 1.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }

    val nodeList = MutableList(100) {
      val angle = map(0.0, 100.0, -PI, PI, it.toDouble())
      val radius = hypot(width.toDouble(), height.toDouble()) * 0.2 * random(0.9, 1.1, rng)
      val center = Vector2(width * 0.5, height * 0.5)
      MovingBody(Vector2(cos(angle), sin(angle)) * radius + center)
    }

    val line = differentialLine {
      nodes = nodeList
      maxEdgeLen = { 6.0 }
      desiredSeparation = 25.0
      fixedEdges = false
    }

    backgroundColor = ColorRGBa.WHITE

    extend {
      line.run()

      val growth = ShapeContour.fromPoints(line.smoothLine.movingAverage(3), closed = true)

      drawer.strokeWeight = 1.0
      drawer.stroke = ColorRGBa.BLACK
      drawer.fill = ColorRGBa.BLACK
      drawer.contour(growth)

      // Or, if using screenshots
      if (screenshots.captureEveryFrame && frameCount % 100 == 0) {
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

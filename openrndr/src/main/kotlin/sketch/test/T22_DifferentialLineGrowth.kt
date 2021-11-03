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
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.drawComposition
import org.openrndr.shape.intersection
import org.openrndr.shape.union
import org.openrndr.shape.difference
import shape.FractalizedLine
import shape.differentialLine
import util.QuadTreeNode
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
      captureEveryFrame = false
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }

    val center = Vector2(width * 0.5, height * 0.5)
    val nodeList = MutableList(100) {
      val angle = map(0.0, 100.0, -PI, PI, it.toDouble())
      val radius = hypot(width.toDouble(), height.toDouble()) * 0.2 * random(0.9, 1.1, rng)
      MovingBody(Vector2(cos(angle), sin(angle)) * radius + center)
    }

    val line = differentialLine {
      nodes = nodeList
      // maxForce = {
      //   2.5
      // }
      // fixedEdges = false
      spawnRule = { node, qtree ->
        node.position.distanceTo(center) < hypot(width.toDouble(), height.toDouble()) * 0.14
      }
      closed = true
      bounds = Rectangle(Vector2.ZERO, width.toDouble(), height.toDouble())
    }

    backgroundColor = ColorRGBa.WHITE

    extend {
      line.run()

      val growth = ShapeContour.fromPoints(line.smoothLine.movingAverage(3), closed = true)

      drawer.strokeWeight = 1.0
      drawer.stroke = ColorRGBa.BLACK
      drawer.fill = ColorRGBa.BLACK
      drawer.contour(growth)

      // this is some debugging shit right here
      val scaledRange = line.bounds.scale(0.2)
      val searchRange = scaledRange.moved(line.nodes.first().position - scaledRange.center)
      val otherNodes = line.qtree.query<QuadTreeNode>(searchRange)
      // println(searchRange)

      drawer.fill = null
      drawer.rectangle(searchRange)

      drawer.fill = ColorRGBa.GREEN
      drawer.stroke = null
      drawer.circles(otherNodes.map { it.position}, 3.0)

      drawer.fill = ColorRGBa.RED
      drawer.circle(line.nodes.first().position, 3.0)

      // if (screenshots.captureEveryFrame && frameCount % 500 == 0) {
      //   screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      // }
    }
  }
}

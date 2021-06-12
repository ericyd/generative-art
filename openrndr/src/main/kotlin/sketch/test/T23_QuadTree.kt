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
import util.QuadTree
import util.QTreeNode
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

    val bounds = Rectangle(Vector2.ZERO, width.toDouble(), height.toDouble())
    val qtree = QuadTree(bounds, 4)
    val radius = 3.0

    val circles = List(100) {
      val point = Vector2(
        random(0.0, width.toDouble(), rng),
        random(0.0, height.toDouble(), rng),
      )
      qtree.add(QTreeNode(point))
      Circle( point,  radius)
    }

    val specialCircle = circles[random(0.0, 100.0, rng).toInt()]

    val scaledRange = bounds.scale(0.2)
    val searchRange = scaledRange.moved(specialCircle.center - scaledRange.center)
    val queriedCircles = qtree.query(searchRange)

    backgroundColor = ColorRGBa.WHITE

    extend {
      drawer.stroke = ColorRGBa.BLACK
      drawer.fill = null
      drawer.rectangle(bounds)
      drawer.rectangle(scaledRange)

      drawer.stroke = ColorRGBa.RED
      drawer.rectangle(searchRange)

      drawer.stroke = null
      drawer.fill = ColorRGBa.BLACK

      drawer.circles(circles)

      drawer.fill = ColorRGBa.GREEN
      drawer.circles(queriedCircles.map { it.position }, radius)

      drawer.fill = ColorRGBa.RED
      drawer.circle(specialCircle)
    }
  }
}

/**
 * TODO: write about algo
 */
package sketch

import force.MovingBody
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.shadestyles.RadialGradient
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.intersection
import org.openrndr.shape.difference
import shape.SmoothLine
import shape.differentialLine
import util.QuadTreeNode
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

    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")
    val rng = Random(seed)

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 3.0
      captureEveryFrame = false
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }

    val noiseScale = random(width * 0.01, width * 0.2, rng)

    val nodeList = MutableList(100) {
      val angle = map(0.0, 100.0, -PI, PI, it.toDouble())
      val radius = hypot(width.toDouble(), height.toDouble()) * 0.2
      val center = Vector2(width * 0.25, height * 0.75)
      MovingBody(Vector2(cos(angle), sin(angle)) * radius + center)
    }

    val circle = Circle(width * 0.5, height * 0.5, hypot(width.toDouble(), height.toDouble()) * 0.29)

    val bounds = Rectangle(Vector2.ZERO, width.toDouble(), height.toDouble())
    val line = differentialLine {
      nodes = nodeList

      cohesionForceFactor = { 0.7 }

      // meh, this is "interesting" but is it "better"???
      spawnRule = { node, qtree ->
        val scaledRange = bounds.scale(0.035)
        val searchRange = scaledRange.moved(node.position - scaledRange.center)
        val otherNodes = qtree.query<QuadTreeNode>(searchRange)
        // println(otherNodes.size)
        // node.position.distanceTo(next.position) > maxNodeSeparation(current)
        otherNodes.size > 15 && otherNodes.size < 40
        // val noise = simplex(seed, node.position / noiseScale)
        // otherNodes.size < 10 && otherNodes.size < 95 && noise < 0.0
      }

      closed = true
      this.bounds = bounds
    }

    backgroundColor = ColorRGBa.WHITE

    val gradient = RadialGradient(
      color0 = ColorRGBa.BLACK,
      color1 = ColorRGBa.WHITE,
      offset = Vector2(0.65, -0.65),
      length = 0.55,
    )

    val bands = mutableListOf<Shape>()
    var bandStart: Shape? = null
    val bandSize = 10
    var bandStartFrame = 0
    var bandEndFrame = -bandSize

    extend {
      line.run()
      val growth = ShapeContour.fromPoints(line.smoothLine.movingAverage(3), closed = true)

      // "background" circle that is being eroded
      drawer.isolated {
        this.shadeStyle = gradient
        this.stroke = null
        this.circle(circle)
      }

      // "remaining" circle part
      drawer.isolated {
        this.stroke = null
        // this.strokeWeight = 1.0
        // this.stroke = ColorRGBa.BLACK
        this.fill = ColorRGBa.WHITE
        this.shadeStyle = null
        val diff = difference(circle.contour, growth.shape)
        // diff.closedContours.forEach { c ->
        //   val line = SmoothLine(c.adaptivePositions(0.8)).movingAverage(5)
        //   this.contour(ShapeContour.fromPoints(line, closed = true))
        // }
        this.shape(diff)
      }

      // val shouldBandStart = frameCount >= bandEndFrame + bandSize && bandStart == null
      // if (shouldBandStart) {
      //   bandStartFrame = frameCount
      //   bandStart = intersection(growth.shape, circle.contour)
      // }
      //
      // val shouldBandEnd = frameCount >= bandStartFrame + bandSize
      // if (shouldBandEnd) {
      //   bandEndFrame = frameCount
      //   bandStartFrame = frameCount + bandSize
      //   val bandEnd = intersection(growth.shape, circle.contour)
      //   bands.add(difference(bandEnd, bandStart!!))
      //   bandStart = null
      // }

      // "dissolving" part
      // drawer.isolated {
      //   this.stroke = null
      //   this.fill = ColorRGBa.RED
      //   // this is an interesting effect but I really can't imagine how to use it well
      //   this.shapes(bands)
      // }

      if (false && frameCount % 30 == 0) {
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
        screenshots.trigger()
      }
    }
  }
}

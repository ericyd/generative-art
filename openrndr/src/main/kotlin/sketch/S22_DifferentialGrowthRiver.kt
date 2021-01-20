/**
 * This is a straightforward differential line growth algorithm,
 * drawn over time to emulate a meandering river system.
 *
 * The algorithm is contained in the DifferentialLine class
 *   src/main/kotlin/shape/DifferentialLine.kt
 * and fully inspired by the Codeplastic implementation here:
 *   http://www.codeplastic.com/2017/07/22/differential-line-growth-with-processing/
 */
package sketch

import force.MovingBody
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.TransformTarget
import org.openrndr.draw.isolated
import org.openrndr.draw.renderTarget
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.extras.color.presets.GHOST_WHITE
import org.openrndr.math.Vector2
import org.openrndr.math.map
import shape.FractalizedLine
import shape.differentialLine
import util.saveToFile
import util.timestamp
import java.lang.Math.pow
import kotlin.random.Random

fun main() = application {
  configure {
    width = 500
    height = 500
  }

  program {
    // We draw to a separate render target to support BOTH multi-frame drawing and screenshots (combining orx-no-clear and screenshots does not work super great)
    val scale = 8.0
    val rt = renderTarget((width * scale).toInt(), (height * scale).toInt(), multisample = BufferMultisample.Disabled) { // multisample requires some weird copying to another color buffer
      colorBuffer()
      depthBuffer()
    }

    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")
    val rng = Random(seed)
    val noiseScale = random(50.0, 100.0, rng)

    backgroundColor = ColorRGBa.GHOST_WHITE

    // // Not using this currently but leaving for easy switching between different designs
    // val center = Vector2(width * 0.5, height * 0.5)
    // val radius = hypot(width * 0.15, height * 0.15)
    // val numStartNodes = 10
    // val nodeListCircular = (0 until numStartNodes).map {
    //     val angle = map(0.0, numStartNodes - 1.0, 0.0, 2.0 * PI, it.toDouble())
    //     MovingBody(center + Vector2(cos(angle), sin(angle)) * radius)
    //   }.toMutableList()

    val nodeListLinear = FractalizedLine(listOf(Vector2(-20.0, height + 20.0), Vector2(width + 20.0, -20.0)), rng = rng)
      .gaussianSubdivide(6, 0.3)
      .points
      .map { MovingBody(it) }
      .toMutableList()

    val line = differentialLine {
      nodes = nodeListLinear

      maxEdgeLen = { m ->
        map(-1.0, 1.0, 1.5, 10.0, simplex(seed, m.position / noiseScale))
      }
      desiredSeparation = 10.0
      squaredDesiredSeparation = { m ->
        map(-1.0, 1.0, pow(desiredSeparation, 0.25), pow(desiredSeparation, 3.5), simplex(seed, m.position / noiseScale))
      }
      fixedEdges = true
      // // experiments that I didn't love
      // maxSpeed = { m ->
      //   simplex(seed, m.position / noiseScale) * 2.0 + 2.0
      // }
      // maxForce = { m ->
      //   simplex(seed, m.position / noiseScale) * 2.0 + 2.0
      // }
    }

    // running a few times to get things "started" looks better
    List(200) { line.run() }

    extend {
      line.run()

      drawer.isolated {
        // bind the scaled render target to automatically scale the drawing
        rt.bind()

        if (frameCount == 1) {
          this.clear(ColorRGBa.GHOST_WHITE)
        }

        this.stroke = ColorRGBa.BLACK.opacify(0.1)
        this.strokeWeight = 0.10
        this.fill = null

        this.lineStrip(line.smoothLine.movingWindow(3))
        // // Uncomment this line if you want to see the individual "nodes" - can be useful for visualizing the effects of the algorithm
        // drawer.circles(line.nodes.map { it.position }, 3.0)

        // Must unbind the render target to allow drawing to the main canvas
        rt.unbind()
      }

      drawer.scale(width.toDouble() / rt.width, TransformTarget.MODEL)
      drawer.image(rt.colorBuffer(0))

      // Save the render target to file every 500 frames
      if (frameCount % 500 == 0) {
        saveToFile(rt, "screenshots/$progName/${timestamp()}-seed-$seed-frame-$frameCount.png")
        // Kind of cool to "reset" the river every now and then so the image doesn't get too cluttered
        drawer.isolated {
          rt.bind()
          this.clear(ColorRGBa.GHOST_WHITE)
          rt.unbind()
        }
      } else if (frameCount % 50 == 0) {
        println("frameCount: $frameCount")
      }
    }
  }
}

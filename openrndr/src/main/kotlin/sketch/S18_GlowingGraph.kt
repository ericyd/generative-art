/**
 * WOW
 * What did we learn today?
 *
 * Do NOT add excessive layers to the compositor.
 * DO add nested loops within a single Layer to add all your things at once.
 *
 * Circle packing algorithm based on
 * http://www.codeplastic.com/2017/09/09/controlled-circle-packing-with-processing/
 */
package sketch

import extensions.CustomScreenshots
import force.MovingBody
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.compositor.blend
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blend.Add
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.extras.color.palettes.colorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import util.generateMovingBodies
import util.packCirclesControlled
import util.timestamp
import kotlin.math.abs
import kotlin.random.Random

fun main() = application {
  configure {
    width = 2000
    height = 2000
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    val screenshots = extend(CustomScreenshots()) {
      quitAfterScreenshot = false
      // Trying to figure out why scaling an image with filters looks like shit, but until I do this needs to be disabled
      // scale = 3.0
      // multisample = BufferMultisample.SampleCount(8)
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      captureEveryFrame = true
    }

    val nBodies = 1000
    val radiusRange = 4.0 to 60.0
    // These are more appropriate for width = 1000
    // val nBodies = 700
    // val radiusRange = 0.10 to 40.0
    // Some very interesting things can happen if you don't honor the actual simplex range of [-1.0, 1.0]
    val noiseRange = -0.65 to 0.5
    val noiseScale = 200.0
    val circleRadius = 3.5

    val spectrum = colorSequence(
      0.25 to
        ColorRGBa.fromHex("0B8EDA"), // blue
      0.5 to
        ColorRGBa.fromHex("D091F2"), // purple
      0.75 to
        ColorRGBa.fromHex("F8AB54"), // orange
    )

    fun connectNodes(node: MovingBody, others: List<MovingBody>, tolerance: Double = 1.0): List<LineSegment> {
      val segments = mutableListOf<LineSegment>()
      val nearNodes = others.filter {
        it != node && it.position.distanceTo(node.position) < (it.radius + node.radius) * tolerance
      }
      for (other in nearNodes) {
        segments.add(LineSegment(node.position, other.position))
      }
      return segments
    }

    // Define some sick FX
    val blur = ApproximateGaussianBlur().apply {
      window = 30
      sigma = 5.0
      spread = 2.0
      gain = 2.0
    }

    // this is OK but not as good as the ApproximateGaussianBlur
    // val blur = Bloom().apply {
    //   blendFactor = 1.50
    //   brightness = 1.50
    //   downsamples = 10
    // }

    // Other cool blends include ColorDodge and Lighten - both subtly different but similar effects
    val blendFilter = Add()

    extend {
      // get that rng
      val rng = Random(seed.toLong())

      val sizeFn = { body: MovingBody ->
        val targetRadius = map(
          noiseRange.first, noiseRange.second,
          radiusRange.first, radiusRange.second,
          simplex(seed, body.position / noiseScale)
        )
        // technically the value will never "arrive" but it gets very close very fast so, y'know ... good enough for me!
        body.radius = abs(body.radius + targetRadius) / 2.0
      }

      // Using `incremental = false` will not return bodies until the packing is complete
      var lastTime = System.currentTimeMillis()
      val packed = packCirclesControlled(
        bodies = generateMovingBodies(nBodies, Vector2(width * 0.5, height * 0.5), 10.0),
        incremental = false, rng = rng, sizeFn = sizeFn
      )
      println("Circle packing complete. Took ${(System.currentTimeMillis() - lastTime) / 1000.0} seconds")
      lastTime = System.currentTimeMillis()

      val composite = compose {
        // A layer for the non-blurred things
        layer {
          draw {
            packed.bodies.forEachIndexed { index, body ->
              val shade = simplex(seed, Vector3(body.position.x, body.position.y, index.toDouble()) / noiseScale) * 0.5 + 0.5

              drawer.strokeWeight = 0.25
              drawer.stroke = spectrum.index(shade)
              drawer.circle(Circle(body.position, circleRadius))
              drawer.lineSegments(connectNodes(body, packed.bodies, 1.25))
            }
          }
        }

        // And a layer for the blurred things 😎
        layer {
          blend(blendFilter)
          draw {
            // We duplicate the loop through `bodies` to reduce the number of layers `compositor` must process.
            // It was crashing the program with all the extra layers that all required a post(blur) effect 😱
            packed.bodies.forEachIndexed { index, body ->
              val shade = simplex(seed, Vector3(body.position.x, body.position.y, index.toDouble()) / noiseScale) * 0.5 + 0.5

              drawer.strokeWeight = 1.0
              drawer.stroke = spectrum.index(shade) // .opacify(0.1)
              drawer.lineSegments(connectNodes(body, packed.bodies, 1.25))
            }
          }
          post(blur)
        }
      }
      println("Composing layers complete. Took ${(System.currentTimeMillis() - lastTime) / 1000.0} seconds")

      composite.draw(drawer)

      // set seed for next iteration
      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

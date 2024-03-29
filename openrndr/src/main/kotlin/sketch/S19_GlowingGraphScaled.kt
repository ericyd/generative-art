/**
 * This is the same sketch as S18_GlowingGraph,
 * but using an oversized render target to try to render a hi-res version with blur.
 * It's kind of a lot of extra work, but I guess maybe worth it? Big question mark on that one.
 *
 * It's kind of annoying that some factors need to be scaled with the render target, and others don't.
 * As mentioned in the OPENRNDR slack, some properties are scale-invariant and others are scale-variant.
 * It's kind of difficult to know which ones are which, though, so it's a bit of a guessing game to get
 * equivalent "styling".
 */
package sketch

import force.MovingBody
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.TransformTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.blend
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blend.Add
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.color.palettes.colorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import util.PackCompleteResult
import util.generateMovingBodies
import util.packCirclesControlled
import util.timestamp
import java.io.File
import kotlin.math.abs
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 1000
  }

  program {
    val w = width * 3
    val h = height * 3
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    // These are more appropriate for width = 1000
    val nBodies = 700
    val radiusRange = 10.0 to 100.0
    // Some very interesting things can happen if you don't honor the actual simplex range of [-1.0, 1.0]
    val noiseRange = -0.5 to 0.5
    val noiseScale = 500.0
    val circleRadius = 4.0

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
      sigma = 15.0
      spread = 5.0
      gain = 2.5
    }

    // Other cool blends include ColorDodge and Lighten - both subtly different but similar effects
    val blendFilter = Add()

    val rt = renderTarget(w, h, multisample = BufferMultisample.Disabled) {
      colorBuffer()
      depthBuffer()
    }

    var packed = PackCompleteResult(generateMovingBodies(nBodies, Vector2(w * 0.5, h * 0.5), 30.0))

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
      packed = packCirclesControlled(
        bodies = packed.bodies,
        incremental = false, rng = rng, sizeFn = sizeFn
      )
      println("Circle packing complete. Took ${(System.currentTimeMillis() - lastTime) / 1000.0} seconds")
      lastTime = System.currentTimeMillis()

      val composite = compose {
        // A layer for the non-blurred things
        layer {
          draw {
            for ((index, body) in packed.bodies.withIndex()) {
              if (body.radius < 1.0) continue
              val shade =
                simplex(seed, Vector3(body.position.x, body.position.y, index.toDouble()) / noiseScale) * 0.5 + 0.5

              drawer.strokeWeight = 1.0
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
            for ((index, body) in packed.bodies.withIndex()) {
              if (body.radius < 1.0) continue
              val shade = simplex(seed, Vector3(body.position.x, body.position.y, index.toDouble()) / noiseScale) * 0.5 + 0.5

              drawer.strokeWeight = 3.0
              drawer.stroke = spectrum.index(shade) // .opacify(0.1)
              drawer.lineSegments(connectNodes(body, packed.bodies, 1.25))
            }
          }
          post(blur)
        }
      }
      println("Composing layers complete. Took ${(System.currentTimeMillis() - lastTime) / 1000.0} seconds")

      // Render to the render target, save file, then scale and draw to screen
      drawer.isolatedWithTarget(rt) {
        drawer.ortho(rt)
        drawer.clear(ColorRGBa.BLACK)
        composite.draw(drawer)
      }

      val targetFile = File("screenshots/$progName/${timestamp()}-seed-$seed.png")
      targetFile.parentFile?.let { file ->
        if (!file.exists()) {
          file.mkdirs()
        }
      }

      drawer.scale(width.toDouble() / rt.width, TransformTarget.MODEL)
      drawer.image(rt.colorBuffer(0))

      // Uncomment to capture frame
      rt.colorBuffer(0).saveToFile(targetFile, async = false)
    }
  }
}

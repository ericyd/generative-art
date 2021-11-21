/**
 *
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
import org.openrndr.extra.noise.fbm
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.extras.color.palettes.colorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import util.PackCompleteResult
import util.QuadTree
import util.packFixedCirclesControlled
import util.timestamp
import java.io.File
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 1000
  }

  program {
    val scaleAmount = 5
    // I'm sure there's a cleaner way to write this with generics, oh well
    val scale = { v: Double -> v * scaleAmount.toDouble() }
    val scaleInt = { v: Int -> v * scaleAmount }
    val w = scaleInt(width)
    val h = scaleInt(height)
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    var seed = 930527176 // random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    var rng = Random(seed.toLong())
    println("seed = $seed")

    val nBodies = 1500

    val spectrum = colorSequence(
      0.25 to
        ColorRGBa.fromHex("0B8EDA"), // blue
      0.5 to
        ColorRGBa.fromHex("D091F2"), // purple
      0.75 to
        ColorRGBa.fromHex("F8AB54"), // orange
    )

    // Define some sick FX
    val blur = ApproximateGaussianBlur().apply {
      window = scaleInt(10)
      sigma = scale(5.0)
      spread = scale(1.75)
      gain = scale(0.8)
    }

    // Other cool blends include ColorDodge and Lighten - both subtly different but similar effects
    val blendFilter = Add()

    val rt = renderTarget(w, h, multisample = BufferMultisample.Disabled) {
      colorBuffer()
      depthBuffer()
    }

    val sizeFn = { point: Vector2, rng: Random ->
      val hash = point.hashCode().toString().slice(2..4).toInt()

      val targetRadius = when {
        hash > 990 -> random(scale(33.0), scale(66.0), rng)
        hash > 960 -> random(scale(3.0), scale(20.0), rng)
        else -> random(scale(0.33), scale(3.0), rng)
      }

      MovingBody(point, radius = targetRadius)
    }

    val generateBodies = { rng: Random, seed: Int ->
      val shaper = random(scale(70.0), scale(100.0), rng)
      List(nBodies) {
        val y = random(0.0, h.toDouble(), rng)
        val xBase = random(w * 0.42, w * 0.58, rng)
        val xOffset = (sin(y / shaper + sqrt(shaper)) + fbm(seed, y / shaper, ::simplex)) * (shaper * 0.35)
        Vector2(xBase + xOffset, y)
      }.map { sizeFn(it, rng) }
    }
    var packed = PackCompleteResult(generateBodies(rng, seed))
    val qtreeBase = QuadTree(Rectangle(w * -0.1, h * -0.1, w * 1.1, h * 1.1), 10)

    val VERBOSE = true

    val DEBUG = false

    extend {
      // get that rng
      rng = Random(seed.toLong())

      // Using `incremental = false` will not return bodies until the packing is complete
      var lastTime = System.currentTimeMillis()
      packed = packFixedCirclesControlled(
        bodies = packed.bodies,
        incremental = DEBUG,
        qtreeBase = qtreeBase,
        rng = rng,
        verbose = VERBOSE
      )
      if (VERBOSE)
        println("Circle packing complete. Took ${(System.currentTimeMillis() - lastTime) / 1000.0} seconds")
      lastTime = System.currentTimeMillis()

      val composite = compose {
        // A layer for the non-blurred things
        layer {
          draw {
            for ((index, body) in packed.bodies.withIndex()) {
              drawer.strokeWeight = scale(0.33)
              drawer.stroke = ColorRGBa.WHITE
              drawer.fill = null
              drawer.circle(Circle(body.position, body.radius))
            }
          }
        }

        if (packed.isComplete && !DEBUG) {
          // And a layer for the blurred things 😎
          layer {
            blend(blendFilter)
            draw {
              for ((index, body) in packed.bodies.withIndex()) {
                if (body.radius < scale(1.0)) continue

                drawer.strokeWeight = scale(1.0)
                drawer.stroke = ColorRGBa.WHITE.opacify(0.15)
                drawer.fill = null
                drawer.circle(Circle(body.position, body.radius))
              }
            }
            post(blur)
          }
        }
      }
      if (VERBOSE)
        println("Composing layers complete. Took ${(System.currentTimeMillis() - lastTime) / 1000.0} seconds")

      // Render to the render target, save file, then scale and draw to screen
      drawer.isolatedWithTarget(rt) {
        this.clear(ColorRGBa.BLACK)
        this.ortho(rt)
        composite.draw(this)
      }

      drawer.scale(width.toDouble() / rt.width, TransformTarget.MODEL)
      drawer.image(rt.colorBuffer(0))

      // Capture frame, save to file
      if (packed.isComplete && !DEBUG) {
        val targetFile = File("screenshots/$progName/${timestamp()}-seed-$seed.jpg")
        targetFile.parentFile?.let { file ->
          if (!file.exists()) {
            file.mkdirs()
          }
        }
        rt.colorBuffer(0).saveToFile(targetFile, async = false)
      }
      if (!DEBUG) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        println("seed = $seed")
        rng = Random(seed.toLong())
        packed = PackCompleteResult(generateBodies(rng, seed))
      }
    }
  }
}

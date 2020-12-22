/**
 * Algorithm in a nutshell:
 *
 * 1. Create a circle
 * 2. Subdivide the circle into two sets of points - one with larger offset than the other
 * 3. For each set of points, use a fractal subdivision algorithm to create a noisy line based on the circle
 * 4. Draw the noisy lines on top of the normal circle, with additive blending and blur effects to give it a nice "electrified" look
 * 5. Draw stars by placing Simplex blobs on a circle with gaussian offset facing inwards.
 */
package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.TransformTarget
import org.openrndr.draw.isolated
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.Layer
import org.openrndr.extra.compositor.blend
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blend.Add
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.extras.color.palettes.colorSequence
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import shape.FractalizedLine
import shape.SimplexBlob
import util.timestamp
import java.io.File
import kotlin.math.atan2
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
    val scale = 3.0
    val w = width * scale
    val h = height * scale
    val center = Vector2(w / 2.0, h / 2.0)
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    seed = 732708981
    println("seed = $seed")

    val mainCircle = Circle(center, hypot(w, h) / 6.0)

    // This is a cool synthwave-inspired gradient
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
      window = 50
      sigma = 40.0
      spread = 20.0
      gain = 1.905
    }

    // Other cool blends include ColorDodge and Lighten - both subtly different but similar effects
    val blendFilter = Add()

    // This is out hi-res render target which we draw to, before scaling it for the screen "preview"
    val rt = renderTarget(w.toInt(), h.toInt(), multisample = BufferMultisample.Disabled) { // multisample requires some weird copying to another color buffer
      colorBuffer()
      depthBuffer()
    }

    fun drawLines(layer: Layer, weight: Double, mainCircle: Circle, lines1: List<List<Vector2>>, lines2: List<List<Vector2>>) {
      layer.draw {
        drawer.fill = null
        drawer.isolated {
          stroke = spectrum.index(0.0)
          strokeWeight = weight
          circle(mainCircle)
        }

        lines1.forEachIndexed { index, list ->
          drawer.isolated {
            stroke = spectrum.index(simplex(seed, index.toDouble()) * 0.5 + 0.5)
            strokeWeight = weight
            lineStrip(list)
          }
        }

        lines2.forEachIndexed { index, list ->
          drawer.isolated {
            stroke = spectrum.index(simplex(seed, (index + lines1.size).toDouble()) * 0.5 + 0.5)
            strokeWeight = weight
            lineStrip(list)
          }
        }
      }
    }

    extend {
      // get that rng
      val rng = Random(seed.toLong())
      val electricLines1 = List(5) {
        val points = mainCircle.scaled(random(0.94, 1.06, rng)).contour.equidistantPositions(45)
        FractalizedLine(points, rng).gaussianSubdivide(7, 0.8).points
      }

      val electricLines2 = List(5) {
        val points = mainCircle.scaled(random(0.94, 1.06, rng)).contour.equidistantPositions(15)
        FractalizedLine(points, rng).gaussianSubdivide(7, 0.55).points
      }

      val composite = compose {
        // A layer for the non-blurred things
        layer {
          drawLines(this, 1.0, mainCircle, electricLines1, electricLines2)
        }

        // And a layer for the blurred things 😎
        layer {
          blend(blendFilter)
          drawLines(this, 1.0, mainCircle, electricLines1, electricLines2)
          post(blur)
        }
      }

      val stars = List(20000) {
        val spread = hypot(w, h) / 1.75
        var pos = Vector2.gaussian(center, Vector2(spread / 6.0), rng)
        // This puts the star positions on a circle with high concentration on the outside and low concentration towards center.
        // To reverse the concentration, simply ` + PI` to the angle
        val angle = atan2(pos.y - center.y, pos.x - center.x)
        pos -= Vector2(cos(angle), sin(angle)) * spread

        SimplexBlob(
          pos,
          seed = random(0.0, Int.MAX_VALUE.toDouble(), rng).toInt(),
          radius = random(2.0, 7.0, rng),
          noiseScale = random(0.5, 0.9, rng),
          moreConvexPlz = true
        ).contour()
      }

      // Render to the render target, save file, then scale and draw to screen
      drawer.isolatedWithTarget(rt) {
        drawer.ortho(rt)
        drawer.clear(ColorRGBa.BLACK)
        composite.draw(drawer)
        drawer.fill = spectrum.index(simplex(seed, seed.toDouble()) * 0.5 + 0.5).opacify(0.35)
        drawer.stroke = null
        drawer.contours(stars)
      }

      drawer.scale(width.toDouble() / rt.width, TransformTarget.MODEL)
      drawer.image(rt.colorBuffer(0))

      // Change to `true` to capture screenshot
      if (true) {
        val targetFile = File("screenshots/$progName/${timestamp()}-seed-$seed.png")
        targetFile.parentFile?.let { file ->
          if (!file.exists()) {
            file.mkdirs()
          }
        }
        rt.colorBuffer(0).saveToFile(targetFile, async = false)
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
      }
    }
  }
}

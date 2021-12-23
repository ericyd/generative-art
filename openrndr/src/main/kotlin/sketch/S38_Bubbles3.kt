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
import org.openrndr.extra.fx.blend.Subtract
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.random
import org.openrndr.extra.shadestyles.LinearGradient
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import shape.FractalizedLine
import util.QuadTree
import util.packFixedCirclesControlled
import util.timestamp
import java.io.File
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 1000
  }

  program {
    val scaleAmount = 5
    fun scale(v: Double): Double { return v * scaleAmount.toDouble() }
    fun scale(v: Int): Int { return v * scaleAmount }
    val w = scale(width)
    val h = scale(height)
    val center = Vector2(w * 0.5, h * 0.5)
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    val nBodies = 1500

    val rt = renderTarget(w, h, multisample = BufferMultisample.Disabled) {
      colorBuffer()
      depthBuffer()
    }

    // Define some sick FX
    val blur = ApproximateGaussianBlur().apply {
      window = scale(10)
      sigma = scale(5.0)
      spread = scale(1.75)
      gain = scale(0.8)
    }

    // Other cool blends include ColorDodge and Lighten - both subtly different but similar effects
    val blendFilter = Subtract()

    val sizeFn = { point: Vector2, rng: Random ->
      val hash = point.hashCode().toString().slice(2..4).toInt()

      val targetRadius = when {
        hash > 994 -> random(scale(50.0), scale(150.0), rng)
        hash > 950 -> random(scale(3.0), scale(40.0), rng)
        else -> random(scale(0.33), scale(3.0), rng)
      }

      MovingBody(point, radius = targetRadius)
    }

    val generateBodies = { rng: Random, baseLine: List<Vector2> ->
      List(nBodies) {
        val basePoint = baseLine[random(0.0, baseLine.size.toDouble(), rng).toInt()]
        Vector2.gaussian(basePoint, Vector2(w * 0.05), rng)
      }.map { sizeFn(it, rng) }
    }

    val qtreeBase = QuadTree(Rectangle(w * -0.3, h * -0.3, w * 1.3, h * 1.3), 10)

    val VERBOSE = true

    val DEBUG = false

    extend {
      // get that rng
      var rng = Random(seed.toLong())

      val baseLineOrientation = random(0.0, PI, rng)
      val baseLineOffset = Vector2(cos(baseLineOrientation + PI/2.0), sin(baseLineOrientation + PI/2.0)) * (w * 0.15)
      val baseLineEndpoint = Vector2(cos(baseLineOrientation), sin(baseLineOrientation)) * center.length
      val baseLine1 = FractalizedLine(
        listOf(
          center + baseLineOffset + baseLineEndpoint,
          center + baseLineOffset - baseLineEndpoint
        ),
        rng
      ).perpendicularSubdivide(6, 0.35).points

      val baseLine2 = FractalizedLine(
        listOf(
          center - baseLineOffset + baseLineEndpoint,
          center - baseLineOffset - baseLineEndpoint
        ),
        rng
      ).perpendicularSubdivide(6, 0.35).points

      var lastTime = System.currentTimeMillis()
      // Using `incremental = false` will not return bodies until the packing is complete
      val packed1 = packFixedCirclesControlled(
        bodies = generateBodies(rng, baseLine1),
        incremental = DEBUG,
        qtreeBase = qtreeBase,
        rng = rng,
        verbose = VERBOSE
      )
      if (VERBOSE)
        println("Circle packing complete. Took ${(System.currentTimeMillis() - lastTime) / 1000.0} seconds")

      lastTime = System.currentTimeMillis()
      val packed2 = packFixedCirclesControlled(
        bodies = generateBodies(rng, baseLine2),
        incremental = DEBUG,
        qtreeBase = qtreeBase,
        rng = rng,
        verbose = VERBOSE
      )
      if (VERBOSE)
        println("Circle packing complete. Took ${(System.currentTimeMillis() - lastTime) / 1000.0} seconds")

      val circles = packed1.bodies.map {
        Circle(it.position, it.radius)
      } + List(scale(40)) {
        // add in some random circles scattered throughout viewport
        Circle(
          Vector2(
            random(w * -0.1, w * 1.1, rng),
            random(h * -0.1, h * 1.1, rng)
          ),
          random(scale(0.33), scale(3.0), rng)
        )
      }

      val composite = compose {
        // Background gradient
        layer {
          draw {
            drawer.shadeStyle = LinearGradient(
              color0 = ColorRGBa.fromHex("DF8364"),
              color1 = ColorRGBa.fromHex("E6CA65"),
              rotation = random(0.0, 360.0, rng)
            )
            drawer.rectangle(0.0, 0.0, w.toDouble(), h.toDouble())
          }
        }

        // Bubble layer 1! (non-blurred)
        layer {
          draw {
            drawer.strokeWeight = scale(1.3)
            drawer.stroke = ColorRGBa.fromHex("3a3a3a").opacify(0.40)
            drawer.fill = null
            for (body in packed2.bodies.map { Circle(it.position, it.radius) }) {
              if (body.radius < scale(1.0)) continue
              drawer.circle(Circle(body.center, body.radius))
            }
          }
        }

        // Bubble layer 1! (blurred)
        layer {
          blend(blendFilter)
          draw {
            drawer.strokeWeight = scale(1.3)
            drawer.stroke = ColorRGBa.fromHex("3a3a3a").opacify(0.30)
            drawer.fill = null
            for (body in packed2.bodies.map { Circle(it.position, it.radius) }) {
              if (body.radius < scale(1.0)) continue
              drawer.circle(Circle(body.center, body.radius))
            }
          }
          post(blur)
        }

        // line textures
        layer {
          draw {
            drawer.strokeWeight = scale(0.2)
            drawer.stroke = ColorRGBa.BLACK.opacify(0.3)
            for (body in circles) {
              // map from radius range to number of lines
              val numberOfLines = map(scale(0.33), scale(150.0), 2.0, 200.0, body.radius).toInt()
              for (i in 0..numberOfLines) {
                val angle = map(0.0, numberOfLines.toDouble(), 0.0, PI, i.toDouble())
                val start = body.center + Vector2(cos(angle), sin(angle)) * body.radius
                val end = start + Vector2(0.0, random(scale(20.0), scale(80.0), rng))
                drawer.lineSegment(start, end)
              }
            }
          }
        }

        // Bubble layer 2!
        layer {
          draw {
            drawer.strokeWeight = scale(1.3)
            drawer.stroke = ColorRGBa.WHITE
            drawer.fill = null
            for (body in circles) {
              drawer.circle(Circle(body.center, body.radius))
            }
          }
        }
      }

      // Render to the render target, save file, then scale and draw to screen
      drawer.isolatedWithTarget(rt) {
        this.clear(ColorRGBa.BLACK)
        this.ortho(rt)
        composite.draw(this)
      }

      drawer.scale(width.toDouble() / rt.width, TransformTarget.MODEL)
      drawer.image(rt.colorBuffer(0))

      // Capture frame, save to file
      if (packed1.isComplete && !DEBUG) {
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
      }
    }
  }
}

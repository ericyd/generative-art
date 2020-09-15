/**
 * Goal:
 * Explore shaderStyle with basic vertices
 */
package sketch.test

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import kotlin.random.Random

fun main() = application {
  configure {
    width = 750
    height = 750
  }

  program {
    backgroundColor = ColorRGBa.WHITE
    //
    val seed = random(1.0, Long.MAX_VALUE.toDouble()).toLong()
    val rand = Random(seed)
    val points = List(30) {
      Vector2(random(0.0, width.toDouble(), rand), random(0.0, height.toDouble(), rand))
      // Vector2(map(0.0, 30.0, 0.0, width.toDouble(), it.toDouble()), height / 2.0)
    }
    //
    // extend {
    //   drawer.fill = ColorRGBa.BLACK
    //   drawer.shadeStyle = shadeStyle {
    //     fragmentTransform = """
    //       x_position = va_position * 2.0;
    //     """.trimIndent()
    //   }
    //   drawer.points(points)
    // }

    // -- create offscreen render target
    val offscreen = renderTarget(width, height) {
      colorBuffer()
      depthBuffer()
    }
    // -- create blur filter
    // val blur = GaussianBloom()
    // val blur = Bloom()

    // This works well - requires circles that are blurred with points drawn over them
    val blur = ApproximateGaussianBlur()

    // val blur = HashBloom()

    // -- create colorbuffer to hold blur results
    val blurred = colorBuffer(width, height)

    extend {
      // -- draw to offscreen buffer
      drawer.isolatedWithTarget(offscreen) {
        clear(ColorRGBa.WHITE)
        fill = ColorRGBa.BLACK
        stroke = null
        // circle(Math.cos(seconds / 20.0) * 100.0 + width / 2, Math.sin(seconds / 20.0) * 100.0 + height / 2.0, 100.0 + 100.0 * Math.cos(seconds / 20.0 * 2.0))
        // points(points)
        circles(points, 10.0)
      }
      // -- set blur parameters
      // blur.blendFactor = 01.500

      // Gaussian bloom
      // blur.window = 5
      // blur.sigma = 3.0
      // blur.gain = 3.0

      // ApproximateGaussianBlur
      blur.window = 25
      blur.sigma = 15.0

      // Bloom
      // blur.blendFactor = 00.250
      // blur.brightness = 1.50
      // blur.padding = 20
      // blur.downsampleRate = 150
      // blur.downsamples = 3
      // // blur.blur = gBlur

      // -- blur offscreen's color buffer into blurred
      blur.apply(offscreen.colorBuffer(0), blurred)
      drawer.image(blurred)
      drawer.fill = ColorRGBa.BLACK
      drawer.points(points)
    }
  }
}

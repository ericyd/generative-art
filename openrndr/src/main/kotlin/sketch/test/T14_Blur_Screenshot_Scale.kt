/**
 * Create demo of how to save a "high res" version of an image with blur and compositor
 */
package sketch.test

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.TransformTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.compositor.blend
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blend.Add
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.shape.Circle
import util.timestamp
import java.io.File

fun main() = application {
  configure {
    width = 500
    height = 500
  }

  program {
    extend(Screenshots()) {
      scale = 1.0 // <-- increasing causes screenshot to change from original image
      multisample = BufferMultisample.SampleCount(8)
    }

    val blur = ApproximateGaussianBlur().apply {
      window = 25
      sigma = 15.0
      spread = 5.0
      gain = 3.0
    }

    val rndrTarget = renderTarget(750, 750, multisample = BufferMultisample.Disabled) {
      colorBuffer()
      depthBuffer()
    }

    val c = Circle(width / 2.0, height / 2.0, width / 4.0)

    extend {
      val composite = compose {
        layer {
          draw {
            drawer.fill = ColorRGBa.PINK.opacify(0.5)
            drawer.circle(c)
          }
        }
        layer {
          blend(Add())
          draw {
            drawer.fill = null
            drawer.stroke = ColorRGBa.PINK
            drawer.circle(c)
          }
          post(blur)
        }
      }

      drawer.isolatedWithTarget(rndrTarget) {
        // image(rt.colorBuffer(0))
        composite.draw(drawer)
      }

      val printTarget = renderTarget(rndrTarget.width, rndrTarget.height, rndrTarget.contentScale, rndrTarget.multisample, rndrTarget.session) {
        colorBuffer()
      }

      drawer.isolatedWithTarget(printTarget) {
        drawer.ortho(rndrTarget)
        image(rndrTarget.colorBuffer(0))
      }
      printTarget.colorBuffer(0).saveToFile(File("screenshot-${timestamp()}.png"), async = false)
      drawer.scale(width.toDouble() / rndrTarget.width, TransformTarget.MODEL)
      drawer.image(rndrTarget.colorBuffer(0))
    }
  }
}

/**
 * Goal:
 * Demo for issue with scaled screenshots with blur
 */
package sketch.test

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.compositor.blend
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blend.Add
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.shape.Circle

fun main() = application {
  configure {
    width = 500
    height = 500
  }

  program {
    extend(Screenshots()) {
      scale = 3.0 // <-- increasing causes screenshot to change from original image
      multisample = BufferMultisample.SampleCount(8)
    }

    val blur = ApproximateGaussianBlur().apply {
      window = 25
      sigma = 15.0
      spread = 5.0
      gain = 3.0
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

      composite.draw(drawer)
    }
  }
}

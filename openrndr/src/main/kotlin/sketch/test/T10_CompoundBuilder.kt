/**
 * I have a feeling this isn't working right
 */
package sketch.test

import extensions.CustomScreenshots
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.contour
import org.openrndr.shape.drawComposition
import org.openrndr.shape.intersection
import util.timestamp

fun main() = application {
  configure {
    width = 750
    height = 950
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    val screenshots = extend(CustomScreenshots()) {
      quitAfterScreenshot = false
      scale = 1.0
      name = "screenshots/$progName/${timestamp()}.png"
    }

    val rect1 = contour {
      moveTo(width * 0.5, height * 0.5)
      lineTo(width * 0.5, height * 0.65)
      lineTo(width * 0.65, height * 0.65)
      lineTo(width * 0.65, height * 0.5)
      lineTo(width * 0.5, height * 0.5)
      close()
    }
    val rect = Rectangle(width * 0.5, height * 0.5, width * 0.15, height * 0.15)
    val cir = Circle(width * 0.4, height * 0.5, 100.0)
    val composition = drawComposition {
      fill = ColorRGBa.PINK
      stroke = ColorRGBa.PINK
      // either:
      shape(intersection(rect.shape, cir.shape))
      // or:
      // shape(intersection(rect1.clockwise, cir.shape))
    }
    extend {
      drawer.composition(composition)
      // drawer.stroke = ColorRGBa.PINK
      // drawer.fill = ColorRGBa.PINK

      // val shapes = compound {
      //   intersection {
      //     shape(cir.shape)
      //     shape(rect.clockwise)
      //   }
      // }
      // drawer.shapes(shapes)
    }
  }
}

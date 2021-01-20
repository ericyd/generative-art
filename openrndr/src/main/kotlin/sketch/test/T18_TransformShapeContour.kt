/**
 * I am silly...
 */
package sketch.test

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.translate
import org.openrndr.shape.Ellipse
import org.openrndr.shape.contour
import kotlin.math.PI

fun main() = application {
  configure {
    width = 500
    height = 500
  }

  program {
    val ellipse1 = Ellipse(Vector2(width * 0.65, height * 0.5), width * 0.1, height * 0.05).contour
    val angle = PI * 0.1
    val ellipse2Center = Vector2(width * 0.35, height * 0.5)
    val ellipse2 = Ellipse(ellipse2Center, width * 0.1, height * 0.05)
      .contour
      .transform(Matrix44.translate(-ellipse2Center.x, -ellipse2Center.y, 0.0))
      .transform(Matrix44.rotateZ(Math.toDegrees(angle)))
      .transform(Matrix44.translate(ellipse2Center.x, ellipse2Center.y, 0.0))
    extend {
      drawer.fill = ColorRGBa.PINK
      drawer.stroke = null
      drawer.contour(ellipse1)
      drawer.contour(ellipse2)
    }
  }
}

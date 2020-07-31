import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.valueLinear
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.contour
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
  configure {
    width = 1024
    height = 1024
  }

  program {
    extend {
      val lineLength = 1000
      val scale = 100
      drawer.stroke = ColorRGBa.PINK
      drawer.fill = null
      for (y in 16 until height step 32) {
        for (x in 16 until width step 32) {
          val c = contour {
            moveTo(Vector2(x.toDouble(), y.toDouble()))
            List(lineLength) {
              // `cursor` points to the end point of the previous command - AMAZING!!
              val noise = valueLinear(100, cursor.x / scale, cursor.y / scale)
              val pos = map(-1.0, 1.0, 0.0, 2.0 * PI, noise)
              lineTo(cursor + Vector2(cos(pos), sin(pos)))
            }
          }

          drawer.contour(c)
        }
      }
    }
  }
}

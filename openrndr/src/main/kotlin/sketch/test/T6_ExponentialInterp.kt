// Exponential interpolation
package sketch.test

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.map
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

fun main() = application {
  configure {
    width = 750
    height = 750
  }

  program {
    backgroundColor = ColorRGBa.WHITE
    val center = Vector2(width / 2.0, height / 2.0)
    val halfDiagonal = hypot(width / 2.0, height / 2.0)

    val points = (1 until halfDiagonal.toInt() step 10).flatMap { initRadius ->
      // radius is an exponential interpolation
      val radius = map(
        1.0, Math.pow(halfDiagonal, 4.0),
        halfDiagonal, 1.0,
        Math.pow(initRadius.toDouble(), 4.0)
      )
      val nPoints = ceil(radius * 0.70).toInt()
      (0 until nPoints step 2).map { degrees ->
        val angle = map(0.0, nPoints.toDouble(), 0.0, 2.0 * PI, degrees.toDouble())
        Vector2(
          cos(angle) * radius + center.x,
          sin(angle) * radius + center.y
        )
      }
    }

    extend {
      drawer.fill = ColorRGBa.BLACK
      drawer.stroke = null

      drawer.circles(points, 4.0)
    }
  }
}

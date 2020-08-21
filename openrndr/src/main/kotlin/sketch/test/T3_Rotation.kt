/**
 * Goal:
 * Make sure I know basic math
 */
package sketch.test

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.hsl
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
  configure {
    width = 750
    height = 750
  }

  program {
    backgroundColor = ColorRGBa.WHITE

    val colors = listOf(
      hsl(261.0, 0.45, 0.43), // purple
      hsl(238.0, 0.67, 0.36), // dark blue
      hsl(194.0, 0.70, 0.85), // light blue
      hsl(10.0, 0.40, 0.15), // dark brown
      hsl(255.0, 0.46, 0.86), // light purple
      hsl(173.0, 0.66, 0.975), // smokey white
      hsl(29.0, 0.93, 0.83) // orange/salmon
    )

    val center = Vector2(width / 2.0, height / 2.0)

    val circles: List<Pair<ColorRGBa, Circle>> = colors.mapIndexed { index, colorHSLa ->
      val angle = map(0.0, colors.size.toDouble(), 0.0, 2.0 * PI, index.toDouble())
      Pair(colorHSLa.toRGBa(), Circle(center + Vector2(cos(angle) * width / 4.0, sin(angle) * width / 4.0), 10.0))
    }

    extend {
      circles.forEach { (color, circle) ->
        drawer.fill = color
        drawer.circle(circle)
      }
    }
  }
}

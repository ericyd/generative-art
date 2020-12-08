/**
 * I am silly...
 */
package sketch.test

import force.MovingBody
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.shape.contour
import shape.Hexagon
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
  configure {
    width = 500
    height = 500
  }

  program {

    val bodies = List(3) { MovingBody(Vector2.ONE) }
    bodies.forEach { primary ->
      println(
        """
        body: $primary
        Number of == bodies: ${bodies.filter { it == primary }.size}
        Number of equals bodies: ${bodies.filter { it.equals(primary) }.size}
        Equal bodies: ${bodies.filter { it == primary }}
        """.trimIndent()
      )
    }

    val hexagons = (0..5).map {
      Hexagon(
        Vector2(width * 0.5, height * 0.5) + Vector2(cos(it.toDouble()), sin(it.toDouble())) * 100.0,
        pointRadius = 50.0,
        rotation = it.toDouble()
      ).contour
    }
    extend {
      drawer.fill = ColorRGBa.PINK
      drawer.stroke = null
      drawer.contours(hexagons)
    }
  }
}

/**
 * I am silly...
 */
package sketch.test

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import util.packCirclesControlled

fun main() = application {
  configure {
    width = 500
    height = 500
  }

  program {
    var bodies = packCirclesControlled(200, Vector2(width * 0.5, height * 0.5), 10.0, incremental = true)
    extend {
      drawer.fill = null
      drawer.stroke = ColorRGBa.PINK

      bodies = packCirclesControlled(bodies = bodies, incremental = true)

      drawer.circles(bodies.map { Circle(it.position, it.radius) })
    }
  }
}

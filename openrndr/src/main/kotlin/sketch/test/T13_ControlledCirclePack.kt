/**
 * Based on
 * http://www.codeplastic.com/2017/09/09/controlled-circle-packing-with-processing/
 */
package sketch.test

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import util.PackCompleteResult
import util.generateMovingBodies
import util.packCirclesControlled

fun main() = application {
  configure {
    width = 500
    height = 500
  }

  program {
    var packed = PackCompleteResult(generateMovingBodies(200, Vector2(width * 0.5, height * 0.5), 10.0))
    extend {
      drawer.fill = null
      drawer.stroke = ColorRGBa.PINK

      packed = packCirclesControlled(bodies = packed.bodies, incremental = true)

      drawer.circles(packed.bodies.map { Circle(it.position, it.radius) })
    }
  }
}

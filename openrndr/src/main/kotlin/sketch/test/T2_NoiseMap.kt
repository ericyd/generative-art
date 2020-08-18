/**
 * Goal:
 * Explore how to map a set of evenly spaced points (e.g. lerp)
 * onto non-even space, ideally using noise or something similar
 *
 * Basically, given:
 * 1     2     3     4     5     6
 *
 * Return:
 * 1 2      3      4   5         6
 */
package sketch.test

import color.Palette
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.hsl
import org.openrndr.math.Vector2
import kotlin.random.Random

fun main() = application {
  configure {
    width = 750
    height = 750
  }

  program {
    backgroundColor = ColorRGBa.BLACK

    val colors = listOf(
      hsl(250.0, 0.5, 0.5),
      hsl(275.0, 0.5, 0.5),
      hsl(2.0, 0.25, 0.4),
      hsl(300.0, 0.65, 0.6),
      hsl(25.0, 0.5, 0.6)
    )

    val seed = 100
    val rand = Random(seed)

    val palette = Palette(colors.map { it.toRGBa() }, 0.0, width.toDouble(), rand)

    extend {
      /**
       * Either of the below methods works. You can either draw rectangles, or draw lines and figure out where they fit
       */
      // palette.forEach { color, x, w ->
      //   drawer.fill = color
      //   drawer.rectangle(x, 0.0, w, height.toDouble())
      // }

      drawer.strokeWeight = 2.0
      (0 until width step 4).forEach { x ->
        var color = palette.colorAt(x.toDouble())
        drawer.stroke = color
        // drawer.stroke = ColorRGBa.WHITE
        drawer.lineSegment(Vector2(x.toDouble(), 0.0), Vector2(x.toDouble(), height.toDouble()))
      }
    }
  }
}

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
import org.openrndr.extra.noise.random
import kotlin.random.Random

fun main() = application {
  configure {
    width = 750
    height = 750
  }

  program {
    backgroundColor = ColorRGBa.BLACK

    val colors = listOf(
      hsl(261.0, 0.45, 0.43), // purple
      hsl(238.0, 0.67, 0.36), // dark blue
      hsl(194.0, 0.70, 0.85), // light blue
      hsl(10.0, 0.40, 0.15), // dark brown
      hsl(255.0, 0.46, 0.86), // light purple
      hsl(173.0, 0.66, 0.975), // smokey white
      hsl(29.0, 0.93, 0.83) // orange/salmon
    )

    val seed = random(1.0, 10000.0).toInt()
    println("seed: $seed")
    val rand = Random(seed)

    val palette = Palette(colors.map { it.toRGBa() }, 0.0, width.toDouble(), rand)

    extend {
      /**
       * Either of the below methods works. You can either draw rectangles, or draw lines and figure out where they fit
       */
      palette.forEach { color, x, w ->
        drawer.fill = color
        drawer.rectangle(x, 0.0, w, height.toDouble())
      }

      // drawer.strokeWeight = 2.0
      // (0 until width step 4).forEach { x ->
      //   var color = palette.colorAt(x.toDouble())
      //   drawer.stroke = color
      //   // drawer.stroke = ColorRGBa.WHITE
      //   drawer.lineSegment(Vector2(x.toDouble(), 0.0), Vector2(x.toDouble(), height.toDouble()))
      // }
    }
  }
}

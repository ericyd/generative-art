/**
 * I've never had any intuition about dot products, so here are some examples
 */
package sketch.test

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.LineSegment

fun main() = application {
  configure {
    width = 800
    height = 800
  }

  program {
    backgroundColor = ColorRGBa.WHITE

    // find /System/Library/Fonts | grep ttf
    val font = loadFont("/System/Library/Fonts/Supplemental/Arial.ttf", 12.0)

    extend {
      drawer.fill = ColorRGBa.BLACK
      drawer.stroke = ColorRGBa.BLACK
      drawer.fontMap = font

      val s = LineSegment(Vector2(width * 0.4, height * 0.5), Vector2(width * 0.6, height * 0.5))
      val a = s.start
      val b = s.end

      drawer.lineSegment(s)

      val p = LineSegment(a, (b - a).perpendicular(YPolarity.CW_NEGATIVE_Y) + a)

      drawer.lineSegment(p)

      drawer.text("YPolarity.CW_NEGATIVE_Y", width * 0.5, height * 0.7)
    }
  }
}

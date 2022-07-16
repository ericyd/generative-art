package sketch.test

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shadestyles.radialGradient
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.math.PI
import kotlin.math.hypot

fun main() = application {
  configure {
    width = 750
    height = 750
  }

  program {
    backgroundColor = ColorRGBa.WHITE

    // that's no moon...
    val moon = Circle(
      width * 0.5,
      height * 0.5,
      // height * 0.5
      hypot(width * 0.5, height * 0.5)
    )

    // val moonFade = radialGradient(ColorRGBa.WHITE, ColorRGBa.TRANSPARENT, exponent = 8.0)
    val moonFade = radialGradient(
      ColorRGBa.TRANSPARENT,
      ColorRGBa.BLACK,
      offset=Vector2.ZERO,
      // length = 0.850, exponent = 3.5
      length = 0.999050, exponent = 4.5
      // offset = Vector2(0.50),
      // rotation = PI / 4.0,
      // length = 01.25, // longer length makes the gradient shrink... right....
      // exponent = 10.50
    )

    extend {
      drawer.shadeStyle = moonFade
      drawer.circle(moon)
      drawer.fill = ColorRGBa.WHITE
      drawer.shadeStyle = null
      drawer.circle(Circle(width * 0.5,
        height * 0.5,width * 0.05))
    }
  }
}

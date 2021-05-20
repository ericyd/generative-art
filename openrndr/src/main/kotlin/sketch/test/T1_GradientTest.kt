package sketch.test

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shadestyles.radialGradient
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.math.PI

fun main() = application {
  configure {
    width = 750
    height = 750
  }

  program {
    backgroundColor = ColorRGBa.BLACK

    // that's no moon...
    val moon = Circle(width * 0.5, height * 0.5, height * 0.5)
    // val moonFade = radialGradient(ColorRGBa.WHITE, ColorRGBa.TRANSPARENT, exponent = 8.0)
    val moonFade = radialGradient(
      ColorRGBa.WHITE,
      ColorRGBa.TRANSPARENT,
      offset = Vector2(0.50),
      rotation = PI / 4.0,
      length = 01.25, // longer length makes the gradient shrink... right....
    )

    extend {
      drawer.shadeStyle = moonFade
      drawer.circle(moon)
    }
  }
}

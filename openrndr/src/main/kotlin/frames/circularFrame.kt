package frames

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.math.Vector2
import kotlin.math.hypot
import kotlin.math.min

fun circularFrame(width: Int, height: Int, drawer: Drawer, strokeColor: ColorRGBa = ColorRGBa.WHITE, centerOverride: Vector2? = null) {
  val center = centerOverride ?: Vector2(width / 2.0, height / 2.0)
  val halfDiagonal = hypot(width * 0.5, height * 0.5)
  val radius = min(width, height).toDouble() * 0.5 + halfDiagonal
  drawer.isolated {
    drawer.fill = null
    drawer.strokeWeight = halfDiagonal
    drawer.stroke = strokeColor
    drawer.circle(center, radius)
  }
}

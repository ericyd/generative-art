package shape

import org.openrndr.math.Vector2
import kotlin.math.cos
import kotlin.math.sin

// SO FTW https://stackoverflow.com/a/2259502
fun rotatePoint(x: Double, y: Double, rotationRadians: Double, about: Vector2): Vector2 {
  val sin = sin(rotationRadians)
  val cos = cos(rotationRadians)

  // translate point back to origin
  val x1 = x - about.x
  val y1 = y - about.y

  // rotate point
  val x2 = x1 * cos - y1 * sin
  val y2 = x1 * sin + y1 * cos

  // translate point back
  return Vector2(x2 + about.x, y2 + about.y)
}

fun rotatePoint(point: Vector2, rotationRadians: Double, about: Vector2): Vector2 =
  rotatePoint(point.x, point.y, rotationRadians, about)

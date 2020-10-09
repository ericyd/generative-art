package noise

import org.openrndr.math.map
import kotlin.math.PI

fun mapToRadians(left: Double, right: Double, n: Double): Double =
  map(left, right, 0.0, 2.0 * PI, n)

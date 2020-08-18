package force

import org.openrndr.math.Vector2
import kotlin.math.hypot

fun unitVector(v: Vector2): Vector2 = v / hypot(v.x, v.y)

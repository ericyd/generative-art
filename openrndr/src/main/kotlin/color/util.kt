package color

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.random
import kotlin.random.Random

fun randomColor(palette: List<ColorRGBa>, rand: Random = Random.Default) =
  palette.get(random(0.0, palette.size.toDouble(), rand).toInt())

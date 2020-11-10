// Exponential interpolation
package sketch.test

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import shape.SimplexBlob
import kotlin.math.hypot
import kotlin.random.Random

fun main() = application {
  configure {
    width = 750
    height = 750
  }

  program {
    backgroundColor = ColorRGBa.WHITE
    val center = Vector2(width / 2.0, height / 2.0)
    val halfDiagonal = hypot(width / 2.0, height / 2.0)

    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    seed = 102920192
    val rng = Random(seed.toLong())

    var blob = SimplexBlob(
      origin = Vector2(width / 2.0, height / 2.0),
      seed = seed,
      radius = 150.0,
      noiseScale = 0.8,
      ridgediness = 10.0,
      rotation = 1.5
    )

    extend {
      drawer.fill = ColorRGBa.BLACK.opacify(0.1)
      drawer.stroke = ColorRGBa.BLACK

      drawer.contour(blob.shape())
    }
  }
}

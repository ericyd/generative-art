package util

import org.openrndr.math.Vector2
import org.openrndr.math.map

class MixableNoise(
  /**
   * primary noise scale
   */
  val scale: Double,
  /**
   * primary noise scale
   */
  val influenceRange: Pair<Double, Double>,
  /**
   * the noise function for the mixable noise
   */
  val noiseFn: (Vector2) -> Vector2,
  /**
   * a function that determines how the noiseFn modulates spatially
   */
  val influenceNoiseFn: (Vector2) -> Double,
  /**
   * the range over which the ratioNoiseFn varies
   */
  val influenceNoiseFnRange: Pair<Double, Double> = -1.0 to 1.0,
  /**
   * scale for the influenceNoiseFn. Defaults to `scale` value
   */
  influenceScale: Double? = null
) {
  private val influenceScale = influenceScale ?: scale
  init {
    if (influenceRange.first < 0.0 || influenceRange.second > 1.0) {
      throw Exception("influence values must be in range [0.0, 1.0]. Got: $influenceRange")
    }
  }

  private fun influence(point: Vector2): Double = map(
    influenceNoiseFnRange.first, influenceNoiseFnRange.second,
    influenceRange.first, influenceRange.second,
    influenceNoiseFn(point)
  )

  // do not normalize here!
  fun eval(point: Vector2): Vector2 = noiseFn(point / scale) * influence(point / influenceScale)

  override fun toString(): String =
    """
    MixableNoise(
      scale = $scale,
      influenceScale = $influenceScale,
      influenceRange = $influenceRange,
      influenceNoiseFnRange = $influenceNoiseFnRange
    )
    """.trimIndent()
}

class MixNoise(val noises: List<MixableNoise>) {

  fun mix(point: Vector2): Vector2 {
    val result = noises.fold(Vector2.ZERO) { sum, mixable ->
      sum + mixable.eval(point)
    }

    return result.normalized
  }
}

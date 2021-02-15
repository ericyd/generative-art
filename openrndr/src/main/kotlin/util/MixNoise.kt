package util

import org.openrndr.math.Vector2
import org.openrndr.math.map

/**
 * @param scale primary noise scale
 * @param influence the relative range of influence the noise function will play in the final mix. Both values must be in range [0.0, 1.0]
 * @param noiseFn the noise function for the mixable noise
 * @param ratioNoiseFn a function that determines how the noiseFn modulates spatially
 * @param ratioNoiseFnRange the range over which the ratioNoiseFn varies
 */
class MixableNoise(val scale: Double, val influence: Pair<Double, Double>, val noiseFn: (Vector2) -> Vector2, val ratioNoiseFn: (Vector2) -> Double, val ratioNoiseFnRange: Pair<Double, Double> = -1.0 to 1.0) {
  init {
    if (influence.first < 0.0 || influence.second > 1.0) {
      throw Exception("influence values must be in range [0.0, 1.0]. Got: $influence")
    }
  }

  fun ratio(point: Vector2): Double = map(
    ratioNoiseFnRange.first, ratioNoiseFnRange.second,
    influence.first, influence.second,
    ratioNoiseFn(point)
  )

  fun eval(point: Vector2): Vector2 = noiseFn(point / scale) * ratio(point)
}

class MixNoise(val noises: List<MixableNoise>) {

  fun mix(point: Vector2): Vector2 {
    val result = noises.fold(Vector2.ZERO) { sum, mixable ->
      sum + mixable.eval(point)
    }

    return result.normalized
  }
}

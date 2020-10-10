package noise

import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import kotlin.math.PI
import kotlin.random.Random

/**
 * Maps a Double from arbitrary range [left, right] to range [0.0, 2π]
 */
fun mapToRadians(left: Double, right: Double, n: Double): Double =
  map(left, right, 0.0, 2.0 * PI, n)

/**
 * Takes a lambda of type (S) -> T
 * and returns a memoized form of that lambda.
 * This is particularly useful for situations
 * where the lambda returns random values, and
 * you want the random value to be the same for
 * any given input.
 */
fun <S : Any, T : Any> memoize(fn: (S) -> T): (S) -> T {
  val map = mutableMapOf<S, T>()
  return { v: S -> map.getOrPut(v) { fn(v) } }
}

/**
 * Returns a memoized random function that accepts a Vector2
 * and always returns the same value for the same input
 * @return Double in range [0.0, 1.0]
 */
fun memoizedValueRandom(rand: Random): (Vector2) -> Double =
  memoize<Vector2, Double> { v -> random(0.0, 1.0, rand) }

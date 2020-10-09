package noise

import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.mix
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.random.Random

/*
Resources:
  https://adrianb.io/2014/08/09/perlinnoise.html
    - this is a really deep/hard interpretation
  https://thebookofshaders.com/11/
    - this is pretty approachable, going to start here
*/

// simulating GLSL vector methods
fun Vector2.floor() = Vector2(floor(this.x), floor(this.y))
fun Vector2.fract() = Vector2(this.x - floor(this.x), this.y - floor(this.y))

fun Vector3.floor() = Vector3(floor(this.x), floor(this.y), floor(this.z))
fun Vector3.fract() = Vector3(this.x - floor(this.x), this.y - floor(this.y), this.z - floor(this.z))

/**
 * Returns a memoized random function that accepts a Double
 * and always returns the same value for the same input
 * @return Double in [0.0, 1.0]
 */
fun memoizedValueRandom(rand: Random, _init: Double): (Double) -> Double {
  val map = mutableMapOf<Double, Double>()
  return { v -> map.getOrPut(v) { random(0.0, 1.0, rand) } }
}

// is this the best way to overload this function, with "init"?
// only difference is return type, not input type. Hmm...🤔
fun memoizedValueRandom(rand: Random, _init: Vector2): (Vector2) -> Double {
  val map = mutableMapOf<Vector2, Double>()
  return { v -> map.getOrPut(v) { random(0.0, 1.0, rand) } }
}

fun memoizedValueRandom(rand: Random, _init: Vector3): (Vector3) -> Double {
  val map = mutableMapOf<Vector3, Double>()
  return { v -> map.getOrPut(v) { random(0.0, 1.0, rand) } }
}

fun <T : Vector2> memoizedGradientRandom(rand: Random, _init: T): (T) -> T {
  val map = mutableMapOf<T, T>()
  return { v -> map.getOrPut(v) { Vector2(random(-1.0, 1.0, rand), random(-1.0, 1.0, rand)).normalized as T } }
}

fun fade(t: Double): Double =
  t * t * t * (t * (t * 6 - 15) + 10) // 6t^5 - 15t^4 + 10t^3 --> more sharp; "Perlin's improved noise curve"
// t * t * (t * -2.0 + 3.0) // 3t^2 - 2t^3 --> more rolling; "Cubic Hermine Curve"

fun fade(v: Vector2): Vector2 = Vector2(fade(v.x), fade(v.y))
fun fade(v: Vector3): Vector3 = Vector3(fade(v.x), fade(v.y), fade(v.z))

/**
 * Custom noise function.
 * Basically copied from https://thebookofshaders.com/11/
 */
fun valueNoise2D(vec: Vector2, memRandom: (Vector2) -> Double): Double {
  // get integer and fractional components of input
  val i = vec.floor()
  val fract = vec.fract()

  // Four corners in 2D of a tile
  val a = memRandom(i)
  val b = memRandom(i + Vector2(1.0, 0.0))
  val c = memRandom(i + Vector2(0.0, 1.0))
  val d = memRandom(i + Vector2(1.0, 1.0))

  // Smooth Interpolation
  val u = fade(fract)

  // Mix 4 corners percentages
  // this is actually very important and precise!
  return mix(a, b, u.x) +
    (c - a) * u.y * (1.0 - u.x) +
    (d - b) * u.x * u.y
}

/**
 * Calculate a net "effect" of the vector
 * @return Double in range [0.0, <vec.length>]
 */
fun vecEffect(vec: Vector2): Double =
  vec.length * atan2(vec.y, vec.x) / 2.0 / PI

/**
 * Custom noise function
 * Important notes
 *  - creates some occassional artifacts around the border
 *  - does NOT tile well...
 * Of course, gotta name it something 😛
 */
fun yanceyNoiseV1(vec: Vector2, memRandom: (Vector2) -> Vector2, normalizerIshThingy: Boolean = false): Double {
  // get integer and fractional components of input
  val i1 = vec.floor()
  val i2 = i1 + Vector2(1.0, 0.0)
  val i3 = i1 + Vector2(0.0, 1.0)
  val i4 = i1 + Vector2(1.0, 1.0)

  val fract = vec.fract()

  // Four corners in 2D of a tile
  // each corner is a "pseudo random gradient" - that is, a Vector2 pointing in a random direction
  val grad1 = memRandom(i1)
  val grad2 = memRandom(i2)
  val grad3 = memRandom(i3)
  val grad4 = memRandom(i4)

  val dist1 = (fract - i1)
  val dist2 = (fract - i2)
  val dist3 = (fract - i3)
  val dist4 = (fract - i4)

  var weighted1 = grad1 / dist1.length
  var weighted2 = grad2 / dist2.length
  var weighted3 = grad3 / dist3.length
  var weighted4 = grad4 / dist4.length

  // hmmm, I like this idea, normalizing by the longest weighted vector
  // but is there a better way?????
  if (normalizerIshThingy) {
    val maxLength = listOf(weighted1.length, weighted2.length, weighted3.length, weighted4.length).max()!!
    weighted1 = weighted1.normalized * (weighted1.length / maxLength)
    weighted2 = weighted2.normalized * (weighted2.length / maxLength)
    weighted3 = weighted3.normalized * (weighted3.length / maxLength)
    weighted4 = weighted4.normalized * (weighted4.length / maxLength)
  }

  // Smooth Interpolation
  val u = fade(fract)

  //  The final mix...
  // HARDER THAN YOU'D THINK!
  val x1 = mix(weighted1.dot(dist1), weighted2.dot(dist2), u.x)
  val x2 = mix(weighted3.dot(dist3), weighted4.dot(dist4), u.x)
  return mix(x1, x2, u.y)
}

/**
 * Trying some custom spins on gradient noise
 *
 * I really haven't found "the one"... but will keep trying
 *
 * Took a "good" version and spun it out into "yanceyNoiseV1" above,
 * but leaving this to continue playing with variations
 */
fun gradientNoise2D(vec: Vector2, memRandom: (Vector2) -> Vector2): Double {
  // get integer and fractional components of input
  val i1 = vec.floor()
  val i2 = i1 + Vector2(1.0, 0.0)
  val i3 = i1 + Vector2(0.0, 1.0)
  val i4 = i1 + Vector2(1.0, 1.0)

  val fract = vec.fract()

  // Four corners in 2D of a tile
  // each corner is a "pseudo random gradient" - that is, a Vector2 pointing in a random direction
  val grad1 = memRandom(i1)
  val grad2 = memRandom(i2)
  val grad3 = memRandom(i3)
  val grad4 = memRandom(i4)

  val dist1 = (fract - i1)
  val dist2 = (fract - i2)
  val dist3 = (fract - i3)
  val dist4 = (fract - i4)

  // calculate "influence vectors" - dot product of psuedo-random gradients and the distance from corner to point
  val inf1 = grad1.dot(dist1)
  val inf2 = grad2.dot(dist2)
  val inf3 = grad3.dot(dist3)
  val inf4 = grad4.dot(dist4)

  var weighted1 = grad1 / dist1.length
  var weighted2 = grad2 / dist2.length
  var weighted3 = grad3 / dist3.length
  var weighted4 = grad4 / dist4.length

  // var weighted1 = inf1 / dist1.length
  // var weighted2 = inf2 / dist2.length
  // var weighted3 = inf3 / dist3.length
  // var weighted4 = inf4 / dist4.length

  // hmmm, I like this, normalizing by the longest weighted vector
  // TODO: but is there a better way?????
  val maxLength = listOf(weighted1.length, weighted2.length, weighted3.length, weighted4.length).max()!!
  weighted1 = weighted1.normalized * (weighted1.length / maxLength)
  weighted2 = weighted2.normalized * (weighted2.length / maxLength)
  weighted3 = weighted3.normalized * (weighted3.length / maxLength)
  weighted4 = weighted4.normalized * (weighted4.length / maxLength)

  // Smooth Interpolation
  val u = fade(fract)

  // Mix 4 corners percentages
  // return (u.mix(i1 - i2, grad1.x) +
  //   u.mix(i2 - i4, 1.0 - grad2.x) +
  //   u.mix(i4 - i3, grad3.y) +
  //   u.mix(i3 - i1, 1.0 - grad4.y)).length

  // val w = vecEffect(u.mix(u - i1, grad1))
  // val x = vecEffect(u.mix(u - i2, grad2))
  // val y = vecEffect(u.mix(u - i3, grad3))
  // val z = vecEffect(u.mix(u - i4, grad4))

  // val w = vecEffect(u - grad1)
  // val x = vecEffect(u - grad2)
  // val y = vecEffect(u - grad3)
  // val z = vecEffect(u - grad4)

  // return (w + x + y + z)

  // return vecEffect(grad1.mix(grad2, u.x) + grad3.mix(-grad1, u.y) * grad3.mix(-grad1, 1.0 - u.x) + grad4.mix(-grad2, u.y * u.x))

  // return vecEffect(grad1.mix(grad2, u.x)) +
  //   vecEffect((grad3 - grad1) * u.y * (1.0 - u.x)) +
  //   vecEffect((grad4 - grad2) * u.x * u.y)

  // return vecEffect(grad1.mix(grad2, u.x) +
  //   (grad3 - grad1) * u.y * (1.0 - u.x) +
  //   (grad4 - grad2) * u.x * u.y)

  // return vecEffect(grad1 * u.x + grad1 * u.y +
  //   grad2 * (1.0 - u.x) + grad2 * u.y +
  //   grad3 * u.x + grad3 * (1.0 - u.y) +
  //   grad4 * (1.0 - u.x) + grad4 * (1.0 - u.y))

  // return (weighted1.x * u.x + weighted1.y * u.y +
  //   weighted2.x * (1.0 - u.x) + weighted2.y * u.y +
  //   weighted3.x * u.x + weighted3.y * (1.0 - u.y) +
  //   weighted4.x * (1.0 - u.x) + weighted4.y * (1.0 - u.y))

  // x1 = lerp(    grad (aaa, xf  , yf  , zf),           // The gradient function calculates the dot product between a pseudorandom
  //   grad (baa, xf-1, yf  , zf),             // gradient vector and the vector from the input coordinate to the 8
  //   u);                                     // surrounding points in its unit cube.
  // x2 = lerp(    grad (aba, xf  , yf-1, zf),           // This is all then lerped together as a sort of weighted average based on the faded (u,v,w)
  //   grad (bba, xf-1, yf-1, zf),             // values we made earlier.
  //   u);
  // y1 = lerp(x1, x2, v);
  //
  // x1 = lerp(    grad (aab, xf  , yf  , zf-1),
  //   grad (bab, xf-1, yf  , zf-1),
  //   u);
  // x2 = lerp(    grad (abb, xf  , yf-1, zf-1),
  //   grad (bbb, xf-1, yf-1, zf-1),
  //   u);
  // y2 = lerp (x1, x2, v);
  //
  // return (lerp (y1, y2, w)+1)/2;

  // val x1 = grad1.mix(grad2, u.x)
  // val x2 = grad3.mix(grad4, u.x)
  // return x1.mix(x2, u.y).dot(u)

  // val x1 = weighted1.mix(weighted2, u.x)
  // val x2 = weighted3.mix(weighted4, u.x)
  // return x1.mix(x2, u.y).dot(u)

  // val x1 = mix(inf1, inf2, u.x)
  // val x2 = mix(inf3, inf4, u.x)
  // return mix(x1, x2, u.y)

  val x1 = mix(weighted1.dot(dist1), weighted2.dot(dist2), u.x)
  val x2 = mix(weighted3.dot(dist3), weighted4.dot(dist4), u.x)
  return mix(x1, x2, u.y)

  // return (weighted1 + weighted2 +
  // weighted3 + weighted4) / 4.0

  // val res= (grad1 * dist1.length + grad2 * dist2.length + grad3 * dist3.length + grad4 * dist4.length) / 4.0
  // return (res.x + res.y) / 2.0

  // based on https://adrianb.io/2014/08/09/perlinnoise.html
  // val x1 = grad1.mix(grad2,u.x)
  // val x2 = grad3.mix(grad4,u.x)
  // val average = x1.mix(x2,u.y)
  // return average.length

  // val x1 = mix(grad1.dot(fract - i1), grad2.dot(fract - i2), u.x)
  // val x2 = mix(grad3.dot(fract - i3), grad4.dot(fract - i4), u.x)
  // return mix(x1, x2, u.y)

  // return (inf1 * dist1.length +
  //   inf2 * dist2.length +
  //   inf3 * dist3.length +
  //   inf4 * dist4.length) / 4.0

  // val inf1 = (grad1.mix(dist1, u.x) + grad1.mix(dist1, u.y))
  // val inf2 = (grad2.mix(dist2, 1.0 - u.x) + grad2.mix(dist2, u.y))
  // val inf3 = (grad3.mix(dist3, u.x) + grad3.mix(dist3, 1.0 - u.y))
  // val inf4 = (grad4.mix(dist4, 1.0 - u.x) + grad4.mix(dist4, 1.0 - u.y))

  // return (inf1.dot(inf2) + inf3.dot(inf4)) / 2.0
  // return (inf1 + inf2 + inf3 + inf4) / 4.0

  // val x1 = mix(inf1.x, inf2.x, u.x)
  // val x2 = mix(inf3.x, inf4.x, 1.0-u.x)
  // val y1 = mix(inf1.y, inf2.y, u.y)
  // val y2 = mix(inf3.y, inf4.y, 1.0-u.y)
  // val mix1 = mix(x1, x2, u.x)
  // val mix2 = mix(y1, y2, u.y)
  // return mix(mix1, mix2, u.x * u.y)

  // return inf1 * u.x + inf1 * u.y +
  //   inf2 * (1.0 - u.x) + inf2 * u.y +
  //   inf3 * u.x + inf3 * (1.0 - u.y) +
  //   inf4 * (1.0 - u.x) + inf4 * (1.0 - u.y)
}

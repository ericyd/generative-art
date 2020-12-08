package noise

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.mix
import util.bilinearInterp
import java.lang.Math.pow
import java.lang.Math.sin
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor

/*
Resources:
  https://adrianb.io/2014/08/09/perlinnoise.html
    - this is a really deep/hard interpretation
  https://thebookofshaders.com/11/
    - this is pretty approachable, going to start here
  https://en.wikipedia.org/wiki/Bilinear_interpolation
    - From the book of shaders:
      "This technique is all about interpolating random values, which is why it's called value noise."
    - From Wikipedia:
      "In mathematics, bilinear interpolation is an extension of linear interpolation for interpolating
       functions of two variables (e.g., x and y) on a rectilinear 2D grid."
    - Formula for Unit Square
      f(x,y) ≈ f(0,0)(1−x)(1−y) + f(1,0)x(1−y) + f(0,1)(1−x)y + f(1,1)xy
  https://en.wikipedia.org/wiki/Bicubic_interpolation
    - From Wikipedia:
      "In image processing, bicubic interpolation is often chosen over bilinear or nearest-neighbor interpolation
       in image resampling, when speed is not an issue. In contrast to bilinear interpolation, which only takes
       4 pixels (2×2) into account, bicubic interpolation considers 16 pixels (4×4). Images resampled with
       bicubic interpolation are smoother and have fewer interpolation artifacts."
    - From Eric: this looks fucking hard as hell, this will have to be a future project.
                 Just glad to know what this is for now and build a library of things to learn in the future.
*/

// simulating GLSL vector methods
fun Vector2.floor() = Vector2(floor(this.x), floor(this.y))
fun Vector2.fract() = Vector2(this.x - floor(this.x), this.y - floor(this.y))

fun Vector3.floor() = Vector3(floor(this.x), floor(this.y), floor(this.z))
fun Vector3.fract() = Vector3(this.x - floor(this.x), this.y - floor(this.y), this.z - floor(this.z))

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
  // To learn more about this, see "Bilinear Interpolation"
  // https://en.wikipedia.org/wiki/Bilinear_interpolation#Unit_square
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

// this is effectively just a different way of doing value noise, since this is a pseudo-random result
fun weirdTrigShit(vec: Vector2): Double {
  // return cos(atan2(vec.y, vec.x) * atan2(vec.y, 1.0 / vec.x) +
  //   sin(pow(cos(vec.x), 2.0) - pow(sin(vec.y), 2.0) * vec.squaredLength))

  return 0.7 - cos(
    atan2(vec.y, vec.x) * atan2(vec.perpendicular().x, 1.0 / vec.perpendicular().y) +
      pow(sin(pow(cos(vec.x), 2.0) - pow(sin(vec.y), 2.0) * vec.squaredLength), 2.0)
  )
}

/**
 * Custom value noise function "generator".
 * Designed as generator to accomodate curl
 * Important notes
 *  - Tiles properly, thanks to Bilinear Interpolation!!!
 *  - Simple "value noise" implementation,
 *    where "value" is determined by the memoized function passed in.
 * Of course, gotta name it something 😛
 */
fun yanceyNoiseGenerator(memoized: (Vector2) -> Double): (Vector2) -> Double {
  return { vec: Vector2 ->
    // get integer and fractional components of input
    val i1 = vec.floor()
    val i2 = i1 + Vector2(1.0, 0.0)
    val i3 = i1 + Vector2(0.0, 1.0)
    val i4 = i1 + Vector2(1.0, 1.0)
    val fract = vec.fract()

    // Four corners in 2D of a tile
    val a = memoized(i1)
    val b = memoized(i2)
    val c = memoized(i3)
    val d = memoized(i4)

    // Smooth Interpolation
    val u = fade(fract)

    bilinearInterp(a, b, c, d, u)
  }
}

/**
 * Custom noise function
 * Important notes
 *  - creates some occassional artifacts around the border
 *  - does NOT tile well...
 * Of course, gotta name it something 😛
 */
fun yanceyNoiseV3(vec: Vector2, memo: (Vector2) -> Vector2, normalizerIshThingy: Boolean = false): Double {
  // get integer and fractional components of input
  val i1 = vec.floor()
  val i2 = i1 + Vector2(1.0, 0.0)
  val i3 = i1 + Vector2(0.0, 1.0)
  val i4 = i1 + Vector2(1.0, 1.0)

  val fract = vec.fract()

  // Four corners in 2D of a tile
  // each corner is a "pseudo random gradient" - that is, a Vector2 pointing in a random direction
  val grad1 = memo(i1)
  val grad2 = memo(i2)
  val grad3 = memo(i3)
  val grad4 = memo(i4)

  val dist1 = (fract - Vector2(0.5, 0.5) - i1)
  val dist2 = (fract - Vector2(0.5, 0.5) - i2)
  val dist3 = (fract - Vector2(0.5, 0.5) - i3)
  val dist4 = (fract - Vector2(0.5, 0.5) - i4)

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
  return bilinearInterp(
    // vecEffect(grad1),
    // vecEffect(grad2),
    // vecEffect(grad3),
    // vecEffect(grad4),
    weighted1.dot(dist1),
    weighted2.dot(dist2),
    weighted3.dot(dist3),
    weighted4.dot(dist4),
    u
  )
}

package noise

import org.openrndr.extra.noise.perlin
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2

/*
References
http://petewerner.blogspot.com/2015/02/intro-to-curl-noise.html
https://www.cs.ubc.ca/~rbridson/docs/bridson-siggraph2007-curlnoise.pdf
*/

/**
 * Implementation of curl noise.
 * Straight from slide 29 of this:
 * https://raw.githubusercontent.com/petewerner/misc/master/Curl%20Noise%20Slides.pdf
 */
fun curl(field: (Int, Double, Double) -> Double, seed: Int, x: Double, y: Double): Vector2 {
  // epsilon is the length of the differential (might be using wrong terminology there)
  // I think there is some room for experimentation here... I'm not sure what the "right" epsilon is
  val eps = 0.50

  // a is the partial derivative of our field at point (x, y) in y direction
  // ∂ / ∂y
  // The slides describe this as "∂x1/∂y" but personally I understand it better as ∂/∂y
  //
  // More readably, this is
  //    val a1 = field(seed, x, y + eps)
  //    val a2 = field(seed, x, y - eps)
  //    val a = (a1 - a2) / (2.0 * eps)
  // but this is simplified for MAXIMUM SPEED 😂
  val a = (field(seed, x, y + eps) - field(seed, x, y - eps)) / (2.0 * eps)

  // b is the partial derivative of our field at point (x, y) in x direction
  // ∂ / ∂x
  // The slides describe this as "∂y1/∂x" but personally I understand it better as ∂/∂x
  //
  // Expanded:
  //    val b1 = field(seed, x + eps, y)
  //    val b2 = field(seed, x - eps, y)
  //    val b = (b1 - b2) / (2.0 * eps)
  val b = (field(seed, x + eps, y) - field(seed, x - eps, y)) / (2.0 * eps)

  return Vector2(a, -b)
}

/**
 * a curl overload for value noise functions that don't require a seed
 * See above for more "annotated" function
 */
fun curl(field: (Vector2) -> Double, x: Double, y: Double): Vector2 {
  val eps = 0.50
  val a = (field(Vector2(x, y + eps)) - field(Vector2(x, y - eps))) / (2.0 * eps)
  val b = (field(Vector2(x + eps, y)) - field(Vector2(x - eps, y))) / (2.0 * eps)
  return Vector2(a, -b)
}

/**
 * curl of perlin
 */
fun perlinCurl(seed: Int, x: Double, y: Double): Vector2 =
  curl(::perlin, seed, x, y)

fun perlinCurl(seed: Int, vec: Vector2): Vector2 =
  curl(::perlin, seed, vec.x, vec.y)

fun simplexCurl(seed: Int, vec: Vector2): Vector2 =
  curl(::simplex, seed, vec.x, vec.y)

fun yanceyCurl(vec: Vector2, memoized: (Vector2) -> Double): Vector2 =
  curl(yanceyNoiseGenerator(memoized), vec.x, vec.y)

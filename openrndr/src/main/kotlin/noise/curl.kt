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
 *
 * epsilon is the length of the differential (might be using wrong terminology there)
 * I think there is some room for experimentation here... I'm not sure what the "right" epsilonilon is
 */
fun curl(field: (Int, Double, Double) -> Double, seed: Int, x: Double, y: Double, epsilon: Double = 0.5): Vector2 {
  // a is the partial derivative of our field at point (x, y) in y direction
  // ∂ / ∂y
  // The slides describe this as "∂x1/∂y" but personally I understand it better as ∂/∂y
  //
  // More readably, this is
  //    val a1 = field(seed, x, y + epsilon)
  //    val a2 = field(seed, x, y - epsilon)
  //    val a = (a1 - a2) / (2.0 * epsilon)
  // but this is simplified for MAXIMUM SPEED 😂
  val a = (field(seed, x, y + epsilon) - field(seed, x, y - epsilon)) / (2.0 * epsilon)

  // b is the partial derivative of our field at point (x, y) in x direction
  // ∂ / ∂x
  // The slides describe this as "∂y1/∂x" but personally I understand it better as ∂/∂x
  //
  // Expanded:
  //    val b1 = field(seed, x + epsilon, y)
  //    val b2 = field(seed, x - epsilon, y)
  //    val b = (b1 - b2) / (2.0 * epsilon)
  val b = (field(seed, x + epsilon, y) - field(seed, x - epsilon, y)) / (2.0 * epsilon)

  return Vector2(a, -b)
}

fun curl(field: (Int, Double, Double) -> Double, seed: Int, vec: Vector2, epsilon: Double = 1.0): Vector2 =
  curl(field, seed, vec.x, vec.y, epsilon)

/**
 * a curl overload for value noise functions that don't require a seed
 * See above for more "annotated" function
 */
fun curl(field: (Vector2) -> Double, x: Double, y: Double, epsilon: Double = 0.5): Vector2 {
  val a = (field(Vector2(x, y + epsilon)) - field(Vector2(x, y - epsilon))) / (2.0 * epsilon)
  val b = (field(Vector2(x + epsilon, y)) - field(Vector2(x - epsilon, y))) / (2.0 * epsilon)
  return Vector2(a, -b)
}

/**
 * curl is based on the first derivative of a vector field.
 * What about the second derivative?
 * Enter: curlOfCurl
 * Note: Not really sure if this implementation is correct
 */
fun curlOfCurl(field: (Int, Double, Double) -> Double, seed: Int, x: Double, y: Double, epsilon: Double = 1.0): Vector2 {
  val n0 = field(seed, x, y)

  // a is the partial second derivative of our field at point (x, y) in y direction
  // ∂^2 / ∂y^2
  //
  // More readably, this is
  //    val n1 = field(seed, x, y + epsilon)
  //    val n2 = field(seed, x, y - epsilon)
  //    val a1 = (n1 - n0) / (2.0 * epsilon)
  //    val a2 = (n0 - n2) / (2.0 * epsilon)
  //    val a = (a1 - a2) / (2.0 * epsilon)
  // but this is simplified for MAXIMUM SPEED 😂
  val a = ((field(seed, x, y + epsilon) - n0) / (2.0 * epsilon) - (n0 - field(seed, x, y - epsilon)) / (2.0 * epsilon)) / (2.0 * epsilon)

  // b is the partial derivative of our field at point (x, y) in x direction
  // ∂ / ∂x
  // The slides describe this as "∂y1/∂x" but personally I understand it better as ∂/∂x
  //
  // Expanded:
  //    val n1 = field(seed, x + epsilon, y)
  //    val n2 = field(seed, x - epsilon, y)
  //    val b1 = (n1 - n0) / (2.0 * epsilon)
  //    val b2 = (n0 - n2) / (2.0 * epsilon)
  //    val b = (b1 - b2) / (2.0 * epsilon)
  val b = ((field(seed, x + epsilon, y) - n0) / (2.0 * epsilon) - (n0 - field(seed, x - epsilon, y)) / (2.0 * epsilon)) / (2.0 * epsilon)

  // I don't understand why b is negative in the original implementation, going to skip it
  return Vector2(a, b)
}

fun curlOfCurl(field: (Int, Double, Double) -> Double, seed: Int, vec: Vector2, epsilon: Double = 1.0): Vector2 =
  curlOfCurl(field, seed, vec.x, vec.y, epsilon)

/**
 * curl of perlin
 */
fun perlinCurl(seed: Int, x: Double, y: Double, epsilon: Double = 0.5): Vector2 =
  curl(::perlin, seed, x, y, epsilon)

fun perlinCurl(seed: Int, vec: Vector2, epsilon: Double = 0.5): Vector2 =
  curl(::perlin, seed, vec.x, vec.y, epsilon)

fun simplexCurl(seed: Int, vec: Vector2, epsilon: Double = 0.5): Vector2 =
  curl(::simplex, seed, vec.x, vec.y, epsilon)

fun yanceyCurl(vec: Vector2, memoized: (Vector2) -> Double): Vector2 =
  curl(yanceyNoiseGenerator(memoized), vec.x, vec.y)

fun perlinCurlOfCurl(seed: Int, vec: Vector2, epsilon: Double = 1.0): Vector2 =
  curlOfCurl(::perlin, seed, vec.x, vec.y, epsilon)

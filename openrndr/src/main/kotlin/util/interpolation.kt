package util

import org.openrndr.math.Vector2

/**
 * Bilinear interpolation of four points, assumed to be on the unit square
 * f(x,y) ≈ f(0,0)(1−x)(1−y) + f(1,0)x(1−y) + f(0,1)(1−x)y + f(1,1)xy
 * where
 *   a = f(0,0)
 *   b = f(1,0)
 *   c = f(0,1)
 *   d = f(1,1)
 *
 * Effectively "mixes" 4 points together based on the coordinate input `pos`
 * Reference:
 *   https://en.wikipedia.org/wiki/Bilinear_interpolation#Unit_square
 */
fun bilinearInterp(a: Double, b: Double, c: Double, d: Double, pos: Vector2): Double =
  a * (1.0 - pos.x) * (1.0 - pos.y) +
    b * pos.x * (1.0 - pos.y) +
    c * (1.0 - pos.x) * pos.y +
    d * pos.x * pos.y

package shape

import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.ShapeContour
import util.rotatePoint
import java.lang.Math.pow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A disformed blobby thing
 * based on nannou/examples/util/blob.rs
 *
 * @param origin The center of the blob
 * @param radius Radius of blob (blobs are based on circles)
 * @param aspectRatio Ratio of width:height, e.g. 1.5 means 1.5w:1.0h
 * @param noiseScale Truly a "scale". Should be in range [0.0, 1.0]
 * @param rotation Rotation in radians [0.0, 2π]
 * @param resolution Number of points in the circle
 * @param seed A seed for Simplex noise
 * @param moreConvexPlz Makes the resulting blob more convex and less concave
 * @param fuzziness Adds some high-frequency noise to the blob that is inconsistent among spatially proximate blobs,
 *   which is to say that if a blob is moving through space and blobs A and B are next to each other,
 *   the fuzziness between them will be different.
 * @param ridgediness Well, identical to fuzziness, but I didn't mean it to be
 */
class SimplexBlob(
  var origin: Vector2 = Vector2.ZERO,
  var radius: Double = 100.0,
  var aspectRatio: Double = 1.0,
  var noiseScale: Double = 1.0,
  val resolution: Int = 360,
  var rotation: Double = 0.0,
  var seed: Int = 1,
  val moreConvexPlz: Boolean = false,
  var fuzziness: Double = 0.0,
  // sometimes you just gotta name it like it feels lol
  // This is the amount of "ridges" you get
  var ridgediness: Double = 0.0
) {
  // As with the Nannou version, this is
  // **heavily** borrowed from https://observablehq.com/@makio135/generating-svgs/10
  fun points(): List<Vector2> =
    (0 until resolution).map { i ->
      val angle = map(0.0, resolution.toDouble(), 0.0, 2.0 * PI, i.toDouble())
      // remove this `+ rotation` if using the `rotate` method below
      + rotation
      val cos = cos(angle)
      val sin = sin(angle)

      var angularNoise = simplex(seed, cos, sin)
      if (moreConvexPlz) {
        angularNoise = angularNoise * 0.5 + 0.5
      }

      // apply the noise for this angle to
      val r = 1.0 + angularNoise * (pow(noiseScale, 3.0))
      var pt = Vector2(
        cos * r * (radius * aspectRatio) + origin.x,
        sin * r * (radius / aspectRatio) + origin.y
      )

      // rotate point around origin
      // This is currently being skipped in favor of simply adding rotation to the original angle
      // val rotated = rotate(x, y)

      // pointNoise is used to create fuzziness and ridgediness
      // Should be changed to simplex(seed, rotated) if using `rotate`
      val pointNoise = simplex(seed, i.toDouble() / resolution * radius * noiseScale)

      // add fuzziness. If fuzziness is 0, no fuzziness is applied
      pt += Vector2(
        cos(pointNoise) * fuzziness,
        sin(pointNoise) * fuzziness
      )

      // add ridgediness. If ridgediness is 0, no ridgediness is applied
      pt += Vector2(
        cos * pointNoise * ridgediness,
        sin * pointNoise * ridgediness
      )

      pt
    }

  fun contour(): ShapeContour = ShapeContour.fromPoints(points(), closed = true)

  // SO FTW https://stackoverflow.com/a/2259502
  private fun rotate(x: Double, y: Double): Vector2 =
    rotatePoint(x, y, rotation, origin)

  companion object {
    // generate a small blob, nice for pointillism effects
    fun pointBlob(
      pos: Vector2,
      rng: Random = Random.Default,
      radiusRange: Pair<Double, Double> = 1.0 to 4.0,
      noiseRange: Pair<Double, Double> = 0.5 to 0.9
    ) =
      SimplexBlob(
        pos,
        seed = random(0.0, Int.MAX_VALUE.toDouble(), rng).toInt(),
        radius = random(radiusRange.first, radiusRange.second, rng),
        noiseScale = random(noiseRange.first, noiseRange.second, rng),
        moreConvexPlz = true
      ).contour()
  }
}

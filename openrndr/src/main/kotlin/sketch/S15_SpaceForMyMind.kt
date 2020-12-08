/**
 * " If I have space,
 *   space for my body.
 *   Then I can have space,
 *   space for my mind. "
 * - Scott Pemberton Trio
 *
 * This is a composite drawing and therefore doesn't have any particular "algorithm" to follow --
 * there is a distinct algorithm for each piece! (planet, craters, atmosphere, stars, and rings)
 */
package sketch

import extensions.CustomScreenshots
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.draw.isolated
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.Ellipse
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.drawComposition
import shape.HatchedShapePacked
import shape.SimplexBlob
import shape.intersects
import util.RadialConcentrationGradient
import util.rotatePoint
import util.timestamp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1450
    height = 750
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    val screenshots = extend(CustomScreenshots()) {
      quitAfterScreenshot = false
      scale = 3.0
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      captureEveryFrame = true
    }

    val bg = ColorRGBa.BLACK
    val fgBase = ColorRGBa(0.99, 0.99, 0.99, 1.0)
    val fg = fgBase.opacify(0.15)
    backgroundColor = bg

    // this is kind of a "base dimension" for shape sizing
    val base = 1000
    val includeCrossHatch = true
    val primaryAngleRange = (PI * 1.1)..(PI * 1.85)

    extend {
      // get that rng
      val rng = Random(seed.toLong())

      val planetCircle = Circle(width * 0.85, height * 0.8, base * 0.5)
      val planetGradient = RadialConcentrationGradient(Vector2(0.65, 0.65), reverse = true)
      // Short.MAX_VALUE // 32767
      // Int.MAX_VALUE // 2147483647
      // set `maxFailedAttempts = Short.MAX_VALUE.toInt() * 4` for much higher density
      println("starting planet hatching")
      val planetHatches = HatchedShapePacked(
        planetCircle.contour, rng = rng, maxFailedAttempts = Short.MAX_VALUE.toInt() * 4,
        includeCrossHatch = includeCrossHatch, primaryAngleRange = primaryAngleRange
      )
        .hatchedShape(
          radiusRange = 0.1..10.0,
          gradient = planetGradient,
          hatchLength = 15.0,
          strokeWeight = 0.1,
          strokeColor = fg
        ).second

      println("rendering planet hatches")
      drawer.composition(planetHatches)

      // generate list of hatches that fill in random "craters" on the planet
      println("starting crater hatching")
      val craters = List(random(3.0, 5.0, rng).toInt()) {
        // place craters in upper left quadrant of planet (where they will be visible
        val origin = Vector2(
          random(planetCircle.center.x - planetCircle.radius, planetCircle.center.x, rng),
          random(planetCircle.center.y - planetCircle.radius, planetCircle.center.y, rng)
        )

        val radius = random(base * 0.1, base * 0.35)

        val crater = SimplexBlob(
          origin, radius,
          aspectRatio = random(0.8, 1.2, rng), noiseScale = random(0.3, 0.7, rng), seed = seed, moreConvexPlz = true
        )

        val gradient = RadialConcentrationGradient(Vector2(0.5, 0.5), reverse = true)

        HatchedShapePacked(
          crater.contour(), rng = rng, maxFailedAttempts = Short.MAX_VALUE.toInt(),
          includeCrossHatch = includeCrossHatch, primaryAngleRange = primaryAngleRange
        )
          .hatchedShape(
            radiusRange = 0.5..20.0,
            gradient = gradient,
            hatchLength = 15.0,
            strokeWeight = 0.1,
            strokeColor = fg,
            intersectionContours = listOf(planetCircle.contour),
          ).second
      }

      println("rendering craters")
      craters.forEach { drawer.composition(it) }

      // the hatches that are emanating from the edge of the planet
      //
      // This is the LEAST DRY thing I've ever written, it's a copy from HatchedShapePacked.packCircles
      //
      // Should consider making a `packCircles` method (name needs work) with different packing strategies (random, radial, gaussian, etc)
      val atmosphereCircle = planetCircle.scaled(1.4)
      val atmosophereGradient = RadialConcentrationGradient(
        Vector2(0.5, 0.5),
        0.37,
        0.48,
        reverse = true
      )
      val atmosphereCirclePack = mutableListOf<Circle>()
      val maxFailedAttempts = Short.MAX_VALUE.toInt() * 4
      val radiusRange = 1.38..20.0
      var failedAttempts = 0
      println("starting the atmosphere circle pack")
      while (failedAttempts < maxFailedAttempts) {
        val angle = random(PI * 0.5, PI * 2.0, rng)
        val distance = random(planetCircle.radius * 0.95, atmosphereCircle.radius, rng)
        val position = Vector2(cos(angle) * distance, sin(angle) * distance) + atmosphereCircle.center
        // endInclusive and start are "reversed" here, because a gradient's lowest concentration maps to 0.0,
        // and that actually correlates to where we want the atmosphereCirclePack to be **most** spaced out.
        // That means we need low concentration to map to high radius, hence the reverse.
        val radius = map(
          0.0, 1.0,
          radiusRange.endInclusive, radiusRange.start,
          atmosophereGradient.assess(atmosphereCircle.contour.bounds, position)
        )
        val circle = Circle(position, radius)

        if (atmosphereCirclePack.any { it.intersects(circle) }) {
          failedAttempts++
          continue
        }

        // this is better for some circle packing but it makes this take **forever** and I'm impatient
        // failedAttempts = 0
        atmosphereCirclePack.add(circle)
      }
      val atmosphere = HatchedShapePacked(
        planetCircle.scaled(1.2).contour, rng = rng, includeCrossHatch = false
      )
        .hatchedShape(
          hatchLength = 5.0,
          primaryAngle = PI * 1.15,
          strokeWeight = 0.1,
          strokeColor = fg,
          differenceContours = listOf(planetCircle.contour),
          circlePack = atmosphereCirclePack,
        ).second

      println("rendering atmosphere")
      drawer.composition(atmosphere)

      fun generateStar(): ShapeContour {
        val y = abs(gaussian(0.0, height / 2.0, rng))
        val xDeviationFactor = map(0.0, height.toDouble(), 3.0, 12.0, y)
        val x = abs(gaussian(0.0, width / xDeviationFactor, rng))

        // Blob needs a few values randomized. Seed, radius, and noiseScale should all be slightly randomized
        val blobSeed = random(0.0, Int.MAX_VALUE.toDouble(), rng).toInt()
        val blobRadius = random(0.1, 2.0, rng)
        val noiseScale = random(0.5, 0.9, rng)

        return SimplexBlob(origin = Vector2(x, y), seed = blobSeed, radius = blobRadius, noiseScale = noiseScale, moreConvexPlz = true)
          .contour()
      }

      println("starting to generate stars")
      val stars = List(3000) { generateStar() }

      println("rendering stars")
      drawer.isolated {
        fill = fg.opacify(0.65)
        stroke = null
        stars.chunked(500) { contours(it) }
      }

      println("creating rings")
      val rings = drawComposition {
        fill = null
        strokeWeight = 0.5
        drawer.lineCap = LineCap.BUTT
        val nRings = 150
        for (i in 0..nRings) {
          // val opacity = gaussian(0.09, 0.05, rng)
          val opacity = simplex(seed, i.toDouble() / 7.5) * 0.175 + 0.175
          stroke = fgBase.opacify(opacity)
          val xRadius = map(0.0, nRings.toDouble(), planetCircle.radius * 1.4, planetCircle.radius * 1.9, i.toDouble())
          val yRadius = xRadius / 3.0
          Ellipse(planetCircle.center, xRadius, yRadius)
            .contour
            // `exploded` breaks the ellipse into 4 segments
            .exploded
            // The first 2 segments are "behind" the planet, and need to be cut (`difference`-d)
            // Indices 0 and 3 are the two that end up being visible
            .flatMapIndexed { index, segment ->
              if (index < 2) {
                org.openrndr.shape.difference(segment, planetCircle.contour).contours
              } else {
                listOf(segment)
              }
            }
            // convert segments (clipped pieces of the ellipse) into lists of points, then rotate and draw
            .forEach { segment ->
              val points = segment
                .equidistantPositions(200)
                .map { rotatePoint(it, PI * 0.1, planetCircle.center) }
              lineStrip(points)
            }
        }
      }

      println("rendering rings")
      drawer.composition(rings)

      // set seed for next iteration
      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      }
    }
  }
}

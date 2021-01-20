/**
 * Create new works here, then move to parent package when complete
 */
package sketch

import noise.simplexCurl
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.TransformTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.simplex
import org.openrndr.extras.color.palettes.colorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import shape.SimplexBlob
import util.RadialConcentrationGradient
import util.packCirclesOnGradient
import util.timestamp
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 1000
  }

  program {
    val scale = 3.0
    val w = width * scale
    val h = height * scale
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    // This is out hi-res render target which we draw to, before scaling it for the screen "preview"
    val rt = renderTarget(w.toInt(), h.toInt(), multisample = BufferMultisample.Disabled) { // multisample requires some weird copying to another color buffer
      colorBuffer()
      depthBuffer()
    }
    var singleScreenshot = false

    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed.toLong()))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    seed = 99178672
    // seed = 1357110203
    println("seed = $seed")

    val mostlyWhiteSpectrum = colorSequence(
      0.25 to
        ColorRGBa.fromHex("F2EFED"), // blue
      0.5 to
        ColorRGBa.fromHex("FCFAEA"), // purple
      0.75 to
        ColorRGBa.fromHex("EDEFF7"), // orange
    )

    // standard point-blobs to make things a bit more dynamic
    fun pointBlob(pos: Vector2, rng: Random) =
      SimplexBlob(
        pos,
        seed = random(0.0, Int.MAX_VALUE.toDouble(), rng).toInt(),
        radius = random(1.0, 4.0, rng),
        noiseScale = random(0.5, 0.9, rng),
        moreConvexPlz = true
      ).contour()

    // Planets have a predictable pattern to them. They each calculate the angle to the sun,
    // and place the gradient on that angle to create the illusion of illumination.
    // Then they pack circles on the gradient with a heavy concentration towards the outside,
    // filter out points that overlap other planets or are outside of themself,
    // and then convert the points to a "point-blob"
    fun planetPoints(planet: ShapeContour, sun: Vector2, avoidPlanets: List<ShapeContour>, rng: Random, minRadius: Double = 0.150, maxRadius: Double = 0.525): List<ShapeContour> {
      val angleToSun = atan2(planet.shape.bounds.center.y - sun.y, planet.shape.bounds.center.x - sun.x)
      return packCirclesOnGradient(
        1.0..100.0,
        planet.shape.bounds,
        RadialConcentrationGradient(
          Vector2(0.5, 0.5) + Vector2(cos(angleToSun), sin(angleToSun)) * 0.05,
          minRadius,
          maxRadius
        ),
        rng = rng,
        clamp = true
      ).filter { planet.contains(it.center) && avoidPlanets.none { p -> p.contains(it.center) } }
        .map { pointBlob(it.center, rng) }
    }

    fun generateStormLines(planet: Circle, rng: Random): List<ShapeContour> =
      planet.contour.equidistantPositions(50).flatMap { circumfrencePoint ->
        val startRadius = random(0.0, 1.0, rng)
        contour {
          moveTo((circumfrencePoint - planet.center) * startRadius + planet.center)
          List(300) {
            lineTo(cursor + simplexCurl(seed, cursor / 300.0, 0.01))
          }
        }
          .segments
          .filterIndexed { index, segment ->
            index != 0 && index % 15 == 0 && planet.contains(segment.start)
          }
          .map { pointBlob(it.start, rng) }
      }

    extend {
      val rng = Random(seed.toLong()) // seeded rng makes everything repeatable and delicious

      // Generate the bodies in our system
      val sunOrigin = Vector2(random(0.0, w, rng), random(0.0, h * 0.25, rng))
      val planetFar = Circle(Vector2.gaussian(Vector2(w * 0.5, h * 0.35), Vector2(w * 0.175, h * 0.05), rng), random(w * 0.05, w * 0.1, rng))
      val planetMiddle = Circle(Vector2.gaussian(Vector2(w * 0.5, h * 0.55), Vector2(w * 0.175, h * 0.05), rng), random(w * 0.08, w * 0.14, rng))
      val planetClose = Circle(Vector2.gaussian(Vector2(w * 0.5, h * 0.85), Vector2(w * 0.125, h * 0.05), rng), random(w * 0.2, w * 0.3, rng))

      // This could be declare as 1 val combined with sunExterior, but this way is a bit more readable
      val sunInterior = List(3000) {
        val pos = Vector2.gaussian(sunOrigin, Vector2(hypot(w, h) * 0.01), rng)
        pointBlob(pos, rng)
      }

      // Add a larger spread to the sun. Doesn't have great density at center, otherwise could use *only* this
      val sunExterior = packCirclesOnGradient(
        0.01..200.0,
        Rectangle(0.0, 0.0, w, h),
        RadialConcentrationGradient(
          Vector2(sunOrigin.x / w, sunOrigin.y / h),
          0.025,
          1.0,
          reverse = true
        ),
        maxFailedAttempts = Short.MAX_VALUE.toInt() * 4,
        rng = rng,
        clamp = true
      ).filter { !planetFar.contains(it.center) && !planetClose.contains(it.center) && !planetMiddle.contains(it.center) }
        .map { pointBlob(it.center, rng) }

      // Render to the render target, then scale and draw to screen
      drawer.isolatedWithTarget(rt) {
        drawer.ortho(rt)
        drawer.clear(ColorRGBa.BLACK)
        drawer.stroke = null
        // The sun, the source of all life
        for (c in sunInterior + sunExterior) {
          drawer.fill = mostlyWhiteSpectrum.index(simplex(seed, seed.toDouble()) * 0.5 + 0.5).opacify(0.85)
          drawer.contour(c)
        }
        // Generate points for all three planets
        for (c in planetPoints(planetFar.contour, sunOrigin, listOf(planetClose.contour, planetMiddle.contour), rng)) {
          drawer.fill = mostlyWhiteSpectrum.index(simplex(seed, seed.toDouble()) * 0.5 + 0.5).opacify(0.85)
          drawer.contour(c)
        }
        for (c in planetPoints(planetMiddle.contour, sunOrigin, listOf(planetClose.contour), rng)) {
          drawer.fill = mostlyWhiteSpectrum.index(simplex(seed, seed.toDouble()) * 0.5 + 0.5).opacify(0.85)
          drawer.contour(c)
        }
        for (c in planetPoints(planetClose.contour, sunOrigin, listOf(), rng, 0.0, 0.5)) {
          drawer.fill = mostlyWhiteSpectrum.index(simplex(seed, seed.toDouble()) * 0.5 + 0.5).opacify(0.85)
          drawer.contour(c)
        }
        // swirly "storm" lines on the nearest planet
        for (c in generateStormLines(planetClose, rng)) {
          drawer.fill = mostlyWhiteSpectrum.index(simplex(seed, seed.toDouble()) * 0.5 + 0.5).opacify(0.85)
          drawer.contour(c)
        }
      }

      drawer.scale(width.toDouble() / rt.width, TransformTarget.MODEL)
      drawer.image(rt.colorBuffer(0))

      // Change to `true` to capture screenshot
      if (false || singleScreenshot) {
        singleScreenshot = false
        val targetFile = File("screenshots/$progName/${timestamp()}-seed-$seed.png")
        targetFile.parentFile?.let { file ->
          if (!file.exists()) {
            file.mkdirs()
          }
        }
        rt.colorBuffer(0).saveToFile(targetFile, async = false)
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
      }
    }
  }
}

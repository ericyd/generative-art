/**
 * An experiment in generative cross-hatching.
 * Directly inspired by
 *   https://www.instagram.com/inkylinesplots/
 *   https://www.instagram.com/kirbyufo/
 *
 * Algorithm in a nutshell
 *  1. Draw evenly spaced lines in one direction (e.g. horizontal)
 *    a. Iteratively build the line by placing points at intervals defined by a noise function
 *  2. Repeat with the other direction (e.g. vertical)
 *  3. For the points in the "horizontal" lines, apply "hatches" in a perpendicular direction.
 *    The center point of the hatch corresponds to the points of the line.
 *    The angle and length are slightly randomized.
 *  4. Repeat with "vertical" lines - the hatches are approximately perpendicular to the first set, creating cross-hatch
 *  5. Add any additional shapes or things; in this case, a moon
 */
package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.valueHermite
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 700
  }

  program {
    // In future, consider implementing own screenshot mechanism that could run after each draw loop
    // screenshot: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-extensions/src/main/kotlin/org/openrndr/extensions/Screenshots.kt
    // timestamp: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-core/src/main/kotlin/org/openrndr/utils/NamedTimestamp.kt
    extend(Screenshots()) {
      quitAfterScreenshot = true
      scale = 2.0
    }

    val shade = 0.05
    val bg = rgb(shade, shade, shade)
    backgroundColor = bg
    // val seed = random(1.0, 2147483647.0).toInt()
    val seed = 1788300904
    val rand = Random(seed)
    println("seed: $seed")

    // that's no moon...
    val moon = Circle(width * 0.7, height * 0.58, height * 0.4)
    val moonFade = linearGradient(ColorRGBa.WHITE, ColorRGBa.TRANSPARENT, rotation = 45.0)

    val shadow = Circle(moon.center - Vector2(height / 8.0), moon.radius)
    val shadowFade = linearGradient(bg, ColorRGBa.TRANSPARENT, rotation = 45.0)

    val perspective = -PI * 0.666
    val perpendicular = perspective + PI / 2
    val hatchLength = 50.0
    val noiseScale = 300.0

    fun pointsAndOpacityList(angle: Double): List<Pair<Vector2, Double>> {
      val nLines = 80
      return List(nLines) { n ->
        // Set initial points for "lines"
        // For one angle, we want to move our lines down, and the other angle we move across
        var x: Double
        var y: Double
        if (angle == perpendicular) {
          x = map(0.0, nLines.toDouble(), -width * 0.1, width * 1.1, n.toDouble())
          y = 0.0
        } else {
          y = map(0.0, nLines.toDouble(), -height * 0.1, height * 1.1, n.toDouble())
          x = 0.0
        }

        val nPointsPerLine = 1000
        List(nPointsPerLine) {
          // this one is nice with noiseScale = 300
          val noise = valueHermite(seed, x / noiseScale, y / noiseScale).absoluteValue
          // val noise = valueQuintic(seed, x / noiseScale, y / noiseScale).absoluteValue
          // val noise = fbm(seed, x / noiseScale, y / noiseScale, ::valueHermite, octaves = 2, lacunarity = 0.2, gain = 0.2).absoluteValue
          val offset = map(0.0, 1.0, 02.1, 25.0, noise)
          // alternative offset calculation
          // val offset = exp(simplex(seed, cursor.x / noiseScale, cursor.y / noiseScale).absoluteValue * 5.0)

          // move the "cursor"
          // For one angle, we want to move our lines down, and the other angle we move across
          // I think this could use some refinement 😳
          if (angle == perpendicular) {
            y += offset
          } else {
            x += offset
          }

          // set final attributes
          val centerOfHatch = Vector2(x, y)
          // when noise is high, hatches will be loosely spaced and should have lower opacity
          val opacity = 1.0 - noise
          Pair(centerOfHatch, opacity)
        }
      }.flatten()
    }

    // hatches are generated by traversing an invisible line and placing points with spacing defined by a noise function.
    // Those center points are used to create hatches, which are placed perpendicular to the "invisible line"
    // The variations in the noise function provide closer or further spaced hatches.
    val hatches: List<Pair<LineSegment, Double>> = pointsAndOpacityList(perspective)
      .map { (center, opacity) ->
        val length = random(hatchLength * 0.7, hatchLength * 1.3, rand)
        val angle = random(perspective * 0.97, perspective * 1.03, rand)
        val start = center + Vector2(cos(angle) * length, sin(angle) * length)
        val end = center + Vector2(cos(angle + PI) * length, sin(angle + PI) * length)
        Pair(LineSegment(start, end), opacity)
      }

    val crossHatches: List<Pair<LineSegment, Double>> = pointsAndOpacityList(perpendicular)
      .map { (center, opacity) ->
        val length = random(hatchLength * 0.7, hatchLength * 1.3, rand)
        val angle = random(perpendicular * 0.97, perpendicular * 1.03, rand)
        val start = center + Vector2(cos(angle) * length, sin(angle) * length)
        val end = center + Vector2(cos(angle + PI) * length, sin(angle + PI) * length)
        Pair(LineSegment(start, end), opacity)
      }

    // stars are small cross hatches in white
    val starSize = 2.5
    val nStars = 300
    val stars = List(nStars) {
      val center = Vector2(random(0.0, width.toDouble(), rand), random(0.0, height.toDouble(), rand))
      Pair(
        LineSegment(center - Vector2(starSize, 0.0), center + Vector2(starSize, 0.0)),
        LineSegment(center - Vector2(0.0, starSize), center + Vector2(0.0, starSize))
      )
    }

    extend {
      drawer.stroke = ColorRGBa(1.0, 1.0, 0.90, random(0.8, 1.0, rand))
      stars.forEach { (horiz, vert) -> drawer.lineSegments(listOf(horiz, vert)) }

      // Not sure why applying a black stroke to the moon improves the edges, but it does
      drawer.stroke = bg
      drawer.strokeWeight = 0.75
      drawer.shadeStyle = moonFade
      drawer.circle(moon.center + Vector2(20.0), moon.radius)
      drawer.fill = ColorRGBa.WHITE
      drawer.circle(moon)

      hatches.forEach { (segment, opacity) ->
        drawer.stroke = bg.opacify(opacity)
        drawer.strokeWeight = opacity + 0.5
        drawer.lineSegment(segment)
      }

      crossHatches.forEach { (segment, opacity) ->
        drawer.stroke = bg.opacify(opacity)
        drawer.strokeWeight = opacity + 0.5
        drawer.lineSegment(segment)
      }

      drawer.stroke = null
      drawer.fill = bg
      drawer.circle(shadow)
      drawer.shadeStyle = shadowFade
      drawer.circle(shadow.center + Vector2(20.0), shadow.radius)

      // outline the moon... ???
      // drawer.fill = null
      // val strokeWeight = 300.0
      // drawer.strokeWeight = strokeWeight
      // drawer.circle(Circle(moon.center, moon.radius + strokeWeight))
    }
  }
}

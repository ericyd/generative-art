/**
 * Goal:
 * Create a basic strange attractor
 * https://www.stsci.edu/~lbradley/seminar/attractors.html
 * https://en.wikipedia.org/wiki/Multiscroll_attractor
 *
 * Blur implementation taken straight from
 * https://github.com/openrndr/openrndr-guide/blob/127562ba673f60a798af93c4e735e10a386bac32/docs/06_Advanced_drawing/C01_Filters_and_post_processing.md
 */
package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.noise.random
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.lookAt
import org.openrndr.math.transforms.project
import org.openrndr.math.transforms.scale
import kotlin.random.Random

fun main() = application {
  configure {
    width = 750
    height = 750
  }

  program {
    // In future, consider implementing own screenshot mechanism that could run after each draw loop
    // screenshot: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-extensions/src/main/kotlin/org/openrndr/extensions/Screenshots.kt
    // timestamp: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-core/src/main/kotlin/org/openrndr/utils/NamedTimestamp.kt
    extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 2.0
    }
    backgroundColor = ColorRGBa.WHITE

    val nLines = 50

    // Good ones:

    // Chen System
    // val a = 30.0
    // val b = 20.0
    // val c = 3.0

    // Chen System
    // val a = 30.0
    // val b = 45.0
    // val c = 4.0
    // val zoom = 0.01

    // Lu Chen system
    val a = 16.0
    val b = 50.0
    val c = 10.0
    val u = -5.2

    // Lorentz
    // val a = 1.70
    // val b = 20.0
    // val c = 20.90

    val zoom = 0.05
    val camera = Vector3(
      width * zoom * 03.6,
      height * zoom * 06.9,
      (width + height) / 2.0 * zoom * 3.0
    )

    fun nextPoint(point: Vector3): Vector3 {
      val dt = 0.0005

      // Lorentz
      //     val a = 10.0
      //     val b = 28.0
      //     val c = 8.0 / 3.0
      // val differential = Vector3(
      //   a * (point.y - point.x),
      //   b * point.x - point.y - point.x * point.z,
      //   point.x * point.y - c * point.z
      // )

      // Chen system
      //     val a = 40.0
      //     val b = 28.0
      //     val c = 3.0
      // val differential = Vector3(
      //   a * (point.y - point.x),
      //   (c - a) * point.x - point.x * point.z + c * point.y,
      //   point.x * point.y - b * point.z
      // )

      // Lu Chen system
      //     val a = 36.0
      //     val b = 20.0
      //     val c = 3.0
      //     val u = -15.15
      val differential = Vector3(
        a * (point.y - point.x),
        point.x - point.x * point.z + c * point.y + u,
        point.x * point.y - b * point.z
      )

      // Henon
      // This doesn't work...
      // val differential = Vector3(
      //   point.x * cos(a) - (point.y - (point.x * point.x)) * sin(a),
      //   point.x * sin(a) + (point.y - (point.x * point.x)) * cos(a),
      //   point.z
      // )

      val result = point + (differential * dt)
      // println("point: $point, result: $differential")
      return result
    }

    fun project3D(vec: Vector3, camera: Vector3): Vector3 {
      val projection = lookAt(camera, Vector3.ZERO)
      return project(vec, projection, Matrix44.scale(Vector3(1.0 / camera.length)), width, height)
    }

    fun project2D(vec: Vector3, camera: Vector3): Vector2 =
      project3D(vec, camera).xy

    val seed = random(1.0, Long.MAX_VALUE.toDouble()).toLong()
    val rand = Random(seed)
    val xRange = width / 2.0
    val yRange = height / 2.0
    var points = List(nLines) {
      Vector3(random(-xRange, xRange, rand), random(-yRange, yRange, rand), 1.0)
      // Vector3(random(0.0, width.toDouble(), rand), random(0.0, height.toDouble(), rand), 1.0)
      // Vector3(width / 2.0, height / 2.0, 0.0)
      // Vector2(map(0.0, 30.0, 0.0, width.toDouble(), it.toDouble()), height / 2.0)
    }
    var lines: List<MutableList<Vector3>> = points.map {
      mutableListOf(it)
    }
    // -- create offscreen render target
    val offscreen = renderTarget(width, height) {
      colorBuffer()
      depthBuffer()
    }
    // -- create blur filter
    val blur = ApproximateGaussianBlur()

    // -- create colorbuffer to hold blur results
    val blurred = colorBuffer(width, height)

    extend {
      lines.forEach { l ->
        l.add(nextPoint(l.last()))
      }
      val points2D = lines.flatMap { line ->
        line.map { p -> project2D(p, camera) }
      }

      // -- draw to offscreen buffer
      drawer.isolatedWithTarget(offscreen) {
        clear(ColorRGBa.WHITE)
        fill = ColorRGBa.BLACK
        // fill = ColorRGBa(0.0 / 255.0, 23 / 255.0, 41 / 255.0, 0.50)
        stroke = null
        points2D.chunked(500) { drawer.circles(it, 25.0) }
      }
      // -- set blur parameters
      // This is approximately the amount of focus
      blur.window = 180
      // roughly translates to amount of spread
      blur.sigma = 10.0

      // -- blur offscreen's color buffer into blurred
      blur.apply(offscreen.colorBuffer(0), blurred)
      drawer.image(blurred)

      // drawer.fill = ColorRGBa.BLACK
      drawer.fill = ColorRGBa(240 / 255.0, 0 / 255.0, 136 / 255.0, 1.0)
      points2D.chunked(500) { drawer.points(it) }
    }
  }
}

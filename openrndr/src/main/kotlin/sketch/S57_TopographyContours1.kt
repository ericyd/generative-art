package sketch

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import datagen.TopographyResult
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.color.palettes.ColorSequence
import org.openrndr.extra.fx.edges.Contour
import org.openrndr.extra.noise.fbm
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.*
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.translate
import org.openrndr.shape.*
import util.grid
import util.rotatePoint
import util.timestamp
import java.io.File
import kotlin.math.*
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 1000
  }

  program {
    val fileContent = File("screenshots/datagen/Topography1-2023-11-30T15.45.31-seed-523618026.json").readText()
    val gson = Gson()
    // The TypeToken type must match the file's structure
    val typeToken = object : TypeToken<TopographyResult>() {}
    val data: TopographyResult = gson.fromJson(fileContent, typeToken.type)
    // !! Row-Major !!
    val getIndex = { x: Int, y: Int -> x * data.nCols + y }

    // I think it would actually be more performant to use a Triple and be a little more imperative.
    fun contourLine(vertices: List<Vector3>, threshold: Double): LineSegment? {
      // If all points are above or all are below, then there are no intersections
      val below = vertices.filter { it.z < threshold }
      val above = vertices.filter { it.z >= threshold }

      // no intersections
      if (above.isEmpty() || below.isEmpty()) {
        return null
      }

      // We have a contour line, let's find it
      val minority = if (above.size < below.size) above.toList() else below.toList()
      val majority = if (above.size > below.size) above.toList() else below.toList()

      // the percentage of the distance along the edge at which the point crosses
      var howFar = (threshold - majority[0].z) / (minority[0].z - majority[0].z)
      val start = Vector2(
        howFar * minority[0].x + (1.0 - howFar) * majority[0].x,
        howFar * minority[0].y + (1.0 - howFar) * majority[0].y
      )

      // the percentage of the distance along the edge at which the point crosses
      howFar = (threshold - majority[1].z) / (minority[0].z - majority[1].z)
      val end = Vector2(
        howFar * minority[0].x + (1.0 - howFar) * majority[1].x,
        howFar * minority[0].y + (1.0 - howFar) * majority[1].y
      )

      return LineSegment(start, end)
    }

    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      contentScale = 3.0
      captureEveryFrame = false
      name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
    }

    val bg = ColorRGBa.WHITE
    backgroundColor = bg

    extend {
      val rng = Random(seed)
      drawer.stroke = ColorRGBa.BLACK
      drawer.strokeWeight = 1.0

      // this works but it's hard to follow
//      for ((index, point) in data.points.withIndex()) {
//        val y = index % width
//        val x = (index - y) / width
//        val output = point
//        val shade = map(0.8, 1.0, 0.0, 1.0, output)
//        if (y == 0) {
//          println("x: $x, y: $y, index: ${getIndex(x, y)}, val: $output, shade: $shade")
//        }
//        drawer.stroke = null
//        drawer.fill = ColorRGBa.WHITE.shade(shade)
//        drawer.rectangle(Rectangle(x.toDouble(), y.toDouble(), stepSize.toDouble(), stepSize.toDouble()))
//      }

      val stepSize = 10
      grid(0, width, 0, height, stepSize) { x: Int, y: Int ->
        val output = data.points[getIndex(x, y)]
        println("x: $x, y: $y, index: ${getIndex(x, y)}, val: $output")
//        val shade = map(0.0, data.initialHeight, 0.0, 1.0, output)
        val shade = map(50.0, 70.0, 0.0, 1.0, output)
        drawer.stroke = null
        drawer.fill = ColorRGBa.WHITE.shade(shade)
        drawer.rectangle(Rectangle(x.toDouble(), y.toDouble(), stepSize.toDouble(), stepSize.toDouble()))
      }

      val stepSizeDouble = stepSize.toDouble()
/*
      for (xInt in 0 until width step stepSize) {
        val x = xInt.toDouble()
        for (yInt in 0 until height step stepSize) {
          // we are making 2 triangles out of a "square",
          // so introduce a simple var to keep track of which direction the triangle is facing.
          // if this looks decent, we can get a more reasonable strategy.
          for (i in 0 until 2) {
            val y = yInt.toDouble()
            // I think this might be why things aren't lining up perfectly -- probably need a "z" function

            val points = if (i == 0) {
              listOf(
                Vector3(x, y, z(x, y)),
                Vector3(x - stepSizeDouble, y, z(x - stepSizeDouble, y)),
                Vector3(
                  x - stepSizeDouble,
                  y - stepSizeDouble,
                  z(x - stepSizeDouble, y - stepSizeDouble)
                ),
              )
            } else {
              listOf(
                Vector3(x, y, z(x, y)),
                Vector3(
                  x - stepSizeDouble,
                  y - stepSizeDouble,
                  z(x - stepSizeDouble, y - stepSizeDouble)
                ),
                Vector3(x, y - stepSizeDouble, z(x, y - stepSizeDouble)),
              )
            }

            drawer.strokeWeight = 1.0
            for (i in 0..20) {
              val threshold = map(0.0, 20.0, -1.5, 1.5, i.toDouble())
              val line = contourLine(points, threshold)
              if (line != null) {
                drawer.lineSegment(line)
              }
            }

            // draw the triangles
//            drawer.strokeWeight = 0.25
//            drawer.stroke = ColorRGBa.GRAY
//            for (i in 0..2) {
//              if (i == 2) {
//                drawer.lineSegment(points[2].xy, points[0].xy)
//              } else {
//                drawer.lineSegment(points[i].xy, points[i + 1].xy)
//              }
//            }
          }
        }
      }

 */

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
      }
    }
  }
}

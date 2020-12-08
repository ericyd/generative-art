/**
 * Algorithm in a nutshell:
 * 1. TODO: Write algorithm in a nutshell
 */
package sketch.test

import extensions.CustomScreenshots
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Segment
import org.openrndr.shape.SegmentPoint
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.compound
import org.openrndr.shape.contour
import util.timestamp
import java.util.Collections.rotate
import java.util.Optional
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
  configure {
    width = 750
    height = 950
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    // seed = 1088846048
    seed = 690157030

    println(
      """
        seed = $seed
      """.trimIndent()
    )

    val screenshots = extend(CustomScreenshots()) {
      quitAfterScreenshot = false
      scale = 3.0
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
      captureEveryFrame = false
    }

    val bg = ColorRGBa.WHITE
    backgroundColor = bg

    val leaf1 = contour {
      moveTo(width * 0.25, height * 0.25)
      curveTo(Vector2(width * 0.25, height * 0.75), Vector2(width * 0.5, height * 0.5), Vector2(width * 0.75, height * 0.75))
      curveTo(Vector2(width * 0.75, height * 0.25), Vector2(width * 0.5, height * 0.25), Vector2(width * 0.25, height * 0.25))
      // lineTo(Vector2(width * 0.25, height * 0.5))
      // lineTo(Vector2(width * 0.75, height * 0.75))
      // lineTo(Vector2(width * 0.75, height * 0.5))
      // lineTo(Vector2(width * 0.25, height * 0.25))
      close()
    }

    val leaves = listOf(leaf1)

    // Credit: https://www.codeproject.com/Articles/5252711/Magic-Formula-of-the-Intersection-Point-of-Two-Lin
    fun wedgeProduct(a: Vector2, b: Vector2): Double =
      a.x * b.y - a.y * b.x

    // This works great for straight lines, but not curves
    fun intersection(a: Segment, b: LineSegment): Vector2 {
      val r_0 = a.end - a.start
      val r_1 = b.end - b.start
      val permissableError = 0.9
      val acceptable = abs(wedgeProduct(r_1, r_0)) / (r_1.length * r_0.length) > permissableError
      val result = (r_0 * wedgeProduct(b.end, b.start) - r_1 * wedgeProduct(a.end, a.start)) /
        wedgeProduct(r_1, r_0)
      if (acceptable) {
        println(
          """
          not acceptable!
          error: ${abs(wedgeProduct(r_1, r_0)) / (r_1.length * r_0.length)}
          result: $result
          """.trimIndent()
        )
      } else {
        println(
          """
          acceptable!
          error: ${abs(wedgeProduct(r_1, r_0)) / (r_1.length * r_0.length)}
          result: $result
          """.trimIndent()
        )
      }
      return result
    }

    /**
     * Based on ShapeContour.nearest
     * https://github.com/openrndr/openrndr/blob/e82f42e0a8679dda42348e8247a2b0f7416a1678/openrndr-core/src/main/kotlin/org/openrndr/shape/Shape.kt#L1199-L1209
     * @param shape the shape on which we want to find the nearest point
     * @param point the point we want to match to the contour
     * @param angle specifies the angle the matching segment must match to be considered valid
     */
    fun nearest(shape: ShapeContour, point: Vector2, angle: Double, acceptableError: Double = 0.1): Optional<Vector2> {
      val n = shape.segments
        .map { it.nearest(point) }
        .filter {
          var linearAngle = atan2(it.position.y - point.y, it.position.x - point.x)
          while (linearAngle < 0.0) {
            linearAngle += PI
          }
          println(
            """
              linearAngle: $linearAngle
              angle: $angle
              diff: ${abs(linearAngle - angle)}
            """.trimIndent()
          )
          abs(linearAngle - angle) < acceptableError ||
            abs(linearAngle + PI - angle) < acceptableError ||
            abs(linearAngle - PI - angle) < acceptableError
        }
        .minBy { it.position.distanceTo(point) }
        ?: return Optional.empty()
      return Optional.of(n.position)
    }

    fun nearestSegmentPoint(shape: ShapeContour, point: Vector2, angle: Double, acceptableError: Double = 0.1): Optional<SegmentPoint> {
      val n = shape.segments
        .map { it.nearest(point) }
        // .filter {
        //   var linearAngle = atan2(it.position.y - point.y, it.position.x - point.x)
        //   while (linearAngle < 0.0) {
        //     linearAngle += PI
        //   }
        //   println("""
        //       linearAngle: $linearAngle
        //       angle: $angle
        //       diff: ${abs(linearAngle - angle)}
        //     """.trimIndent())
        //   abs(linearAngle - angle) < acceptableError ||
        //     abs(linearAngle + PI - angle) < acceptableError ||
        //     abs(linearAngle - PI - angle) < acceptableError
        // }
        .minBy { it.position.distanceTo(point) }
        ?: return Optional.empty()
      return Optional.of(n)
    }

    fun lineSegmentForHatch(shape: ShapeContour, start: Vector2, angle: Double, length: Double): Optional<LineSegment> {
      val end = Vector2(start.x + cos(angle) * length, start.y + sin(angle) * length)
      val containsStart = shape.contains(start)
      val containsEnd = shape.contains(end)
      return if (containsStart && containsEnd) {
        Optional.of(LineSegment(start, end))
      } else if (containsStart && !containsEnd) {
        // clip end
        val nearestEnd = nearest(shape, end, angle)
        if (nearestEnd.isPresent) {
          Optional.of(LineSegment(start, nearestEnd.get()))
        } else {
          Optional.empty()
        }
      } else if (containsEnd && !containsStart) {
        // clip start
        val nearestStart = nearest(shape, start, angle)
        if (nearestStart.isPresent) {
          Optional.of(LineSegment(nearestStart.get(), end))
        } else {
          Optional.empty()
        }
      } else {
        // clip both
        val nearestEnd = nearest(shape, end, angle)
        val nearestStart = nearest(shape, start, angle)
        if (nearestStart.isPresent && nearestEnd.isPresent) {
          Optional.of(LineSegment(nearestStart.get(), nearestEnd.get()))
        } else {
          Optional.empty()
        }
      }
    }

    fun clipLineSegmentToShape(shape: ShapeContour, line: LineSegment): Optional<LineSegment> {
      val angle = atan2(line.end.y - line.start.y, line.end.x - line.start.x)
      val containsStart = shape.contains(line.start)
      val containsEnd = shape.contains(line.end)
      return if (containsStart && containsEnd) {
        println("contains both")
        Optional.of(line)
      } else if (containsStart && !containsEnd) {
        println("contains only start")
        // clip end
        val nearestEnd = nearest(shape, line.end, angle)
        if (nearestEnd.isPresent) {
          Optional.of(LineSegment(line.start, nearestEnd.get()))
        } else {
          Optional.empty()
        }
      } else if (containsEnd && !containsStart) {
        println("contains only end")
        // clip start
        val nearestStart = nearest(shape, line.start, angle)
        if (nearestStart.isPresent) {
          Optional.of(LineSegment(nearestStart.get(), line.end))
        } else {
          Optional.empty()
        }
      } else {
        println("contains neither")
        // clip both
        val nearestEnd = nearest(shape, line.end, angle)
        val nearestStart = nearest(shape, line.start, angle)
        if (nearestStart.isPresent && nearestEnd.isPresent) {
          Optional.of(LineSegment(nearestStart.get(), nearestEnd.get()))
        } else {
          Optional.empty()
        }
      }
    }

    fun clipLineSegmentToShape2(shape: ShapeContour, line: LineSegment): Optional<LineSegment> {
      val angle = atan2(line.end.y - line.start.y, line.end.x - line.start.x)
      val containsStart = shape.contains(line.start)
      val containsEnd = shape.contains(line.end)
      return if (containsStart && containsEnd) {
        println("contains both")
        Optional.of(line)
      } else if (containsStart && !containsEnd) {
        println("contains only start")
        // clip end
        val nearestEnd = nearestSegmentPoint(shape, line.end, angle)
        if (nearestEnd.isPresent) {
          val newEnd = intersection(nearestEnd.get().segment, line)
          Optional.of(LineSegment(line.start, newEnd))
        } else {
          Optional.empty()
        }
      } else if (containsEnd && !containsStart) {
        println("contains only end")
        // clip start
        val nearestStart = nearestSegmentPoint(shape, line.start, angle)
        if (nearestStart.isPresent) {
          val newStart = intersection(nearestStart.get().segment, line)
          Optional.of(LineSegment(newStart, line.end))
        } else {
          Optional.empty()
        }
      } else {
        println("contains neither")
        // clip both
        val nearestEnd = nearestSegmentPoint(shape, line.end, angle)
        val nearestStart = nearestSegmentPoint(shape, line.start, angle)
        val newStart = intersection(nearestStart.get().segment, line)
        val newEnd = intersection(nearestEnd.get().segment, line)
        if (nearestStart.isPresent && nearestEnd.isPresent) {
          Optional.of(LineSegment(newStart, newEnd))
        } else {
          Optional.empty()
        }
      }
    }

    fun clipLineSegmentToShape3(shape: ShapeContour, line: LineSegment): Optional<LineSegment> {
      val angle = atan2(line.end.y - line.start.y, line.end.x - line.start.x)
      val containsStart = shape.contains(line.start)
      val containsEnd = shape.contains(line.end)
      return if (containsStart && containsEnd) {
        println("contains both")
        Optional.of(line)
      } else if (containsStart && !containsEnd) {
        println("contains only start")
        // clip end
        val newEnd = shape.clockwise.nearest(line.end)
        Optional.of(LineSegment(line.start, newEnd.contour.position(newEnd.contourT)))
      } else if (containsEnd && !containsStart) {
        println("contains only end")
        // clip start
        val newStart = shape.clockwise.nearest(line.start)
        Optional.of(LineSegment(newStart.contour.position(newStart.contourT), line.end))
      } else {
        println("contains neither")
        // clip both
        val newEnd = shape.clockwise.nearest(line.end)
        val newStart = shape.clockwise.nearest(line.start)
        // Optional.of(LineSegment(newStart.position, newEnd.position))
        Optional.of(LineSegment(newStart.contour.position(newStart.contourT), newEnd.contour.position(newEnd.contourT)))
      }
    }

    // val hatches = leaves.flatMap { leaf ->
    //   val spacing = 8
    //   val angle = PI / 2.0
    //   val length = hypot(leaf.bounds.width, leaf.bounds.height)
    //   (leaf.bounds.y.toInt() until leaf.bounds.y.toInt() + leaf.bounds.height.toInt() step spacing).map { startY ->
    //     val start = Vector2(leaf.bounds.x, startY.toDouble())
    //     lineSegmentForHatch(leaf, start, angle, length)
    //   } + (leaf.bounds.x.toInt() until leaf.bounds.x.toInt() + leaf.bounds.width.toInt() step spacing).map { startX ->
    //     val start = Vector2(startX.toDouble(), leaf.bounds.y)
    //     lineSegmentForHatch(leaf, start, angle, length)
    //   }
    // }.filter { it.isPresent }.map { it.get() }
    //
    // val startPoints = leaves.flatMap { leaf ->
    //   val spacing = 8
    //   (leaf.bounds.y.toInt() until leaf.bounds.y.toInt() + leaf.bounds.height.toInt() step spacing).map { startY ->
    //     Vector2(leaf.bounds.x, startY.toDouble())
    //   } + (leaf.bounds.x.toInt() until leaf.bounds.x.toInt() + leaf.bounds.width.toInt() step spacing).map { startX ->
    //     Vector2(startX.toDouble(), leaf.bounds.y)
    //   }
    // }

    // val rawHatches = leaves.flatMap { leaf ->
    //   val spacing = 8
    //   val angle = PI / 2.0
    //   val length = hypot(leaf.bounds.width, leaf.bounds.height)
    //   (leaf.bounds.y.toInt() until leaf.bounds.y.toInt() + leaf.bounds.height.toInt() step spacing).map { startY ->
    //     val start = Vector2(leaf.bounds.x, startY.toDouble())
    //     val end = Vector2(start.x + cos(angle) * length, start.y + sin(angle) * length)
    //     LineSegment(start, end)
    //   } + (leaf.bounds.x.toInt() until leaf.bounds.x.toInt() + leaf.bounds.width.toInt() step spacing).map { startX ->
    //     val start = Vector2(startX.toDouble(), leaf.bounds.y)
    //     val end = Vector2(start.x + cos(angle) * length, start.y + sin(angle) * length)
    //     LineSegment(start, end)
    //   }
    // }

    val rawHatch1 = LineSegment(
      Vector2(leaves[0].bounds.x + leaves[0].bounds.width / 2.0, leaves[0].bounds.y),
      Vector2(leaves[0].bounds.x + leaves[0].bounds.width / 2.0, leaves[0].bounds.y + leaves[0].bounds.height)
    )

    val clippedHatch1 = clipLineSegmentToShape2(leaves[0], rawHatch1)

    val clippedHatch2 = clipLineSegmentToShape3(leaves[0], rawHatch1)

    val r = Rectangle(Vector2(width / 2.0, 0.0), 1.0, height.toDouble())

    // r.contour
    rotate(listOf(r), 45)

    val s1 = compound {
      intersection {
        shape(r.contour)
        shape(leaves[0].clockwise)
      }
    }

    extend {
      // get that rng
      // val rng = Random(seed.toLong())

      drawer.fill = null
      drawer.stroke = ColorRGBa.BLACK
      drawer.contours(leaves)

      // val leafCircles = leaf1.segments.map { it.start }
      var leafCircles = leaf1.clockwise.triangulation.map { it.x1 }
      drawer.circles(leafCircles, 5.0)

      drawer.stroke = ColorRGBa.GREEN
      leafCircles = leaf1.clockwise.triangulation.map { it.x2 }
      drawer.circles(leafCircles, 5.0)

      drawer.stroke = ColorRGBa.PINK
      leafCircles = leaf1.clockwise.triangulation.map { it.x3 }
      drawer.circles(leafCircles, 5.0)

      // drawer.lineSegments(hatches)

      // drawer.circles(startPoints, 5.0)
      //
      // drawer.lineSegments(rawHatches)

      drawer.lineSegment(rawHatch1)

      drawer.stroke = ColorRGBa.RED
      drawer.strokeWeight = 3.0
      drawer.lineSegment(clippedHatch1.get())

      drawer.stroke = ColorRGBa.BLUE
      drawer.lineSegment(clippedHatch2.get())

      // val offscreen = renderTarget(width, height) {
      //   colorBuffer()
      //   depthBuffer()
      // }
      // drawer.isolatedWithTarget(offscreen) {
      //   drawer.stroke = null
      //   drawer.fill = ColorRGBa.BLACK
      //   drawer.shapes(s1)
      // }
      //
      // drawer.rotate(45.0, offscreen)
      drawer.stroke = null
      drawer.fill = ColorRGBa.BLACK
      drawer.shapes(s1)

      // set seed for next iteration
      // seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
      // screenshots.name = "screenshots/S9_trial/S9_DisintegratingColumn-${progRef.namedTimestamp("png", "screenshots")}-seed-$seed.png"
    }
  }
}

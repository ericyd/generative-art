package sketch.test

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

fun main() = application {
  configure {
    width = 800
    height = 800
  }

  program {
    backgroundColor = ColorRGBa.WHITE

    // find /System/Library/Fonts | grep ttf
    val font = loadFont("/System/Library/Fonts/Supplemental/Arial.ttf", 12.0)

    fun orientation(s: Segment): Double = atan2(s.end.y - s.start.y, s.end.x - s.start.x)

    // use atan2 b/c cross product doesn't work so well for this implementation
    // get the difference in orientation between the segment and the next segment
    // fun averageCurvature(segments: List<Segment>): Double {
    //   val diffs = segments.mapIndexedNotNull { index, segment ->
    //     if (index == segments.size - 1) {
    //       null
    //     } else {
    //       orientation(segments[index + 1]) - orientation(segment)
    //     }
    //   }
    //   return diffs.sum() / diffs.size.toDouble()
    // }

    fun averageCurvature(segments: List<Segment>): Double {
      val diffs = segments.mapIndexedNotNull { index, segment ->
        if (index == segments.size - 1) {
          null
        } else {
          val diff = orientation(segments[index + 1]) - orientation(segment)
          // HOLY HELL... Kotlin's `atan2` returns in range [-PI, PI], NOT [0, 2PI]
          // my intuition here is good but my check is wrong... or something
          // when crossing the 0 radian threshold, the difference will be large even though the angular difference is small
          val newDiff = when {
            abs(diff) > PI && orientation(segments[index + 1]) > 0.0 -> orientation(segments[index + 1]) - (orientation(segment) + 2.0 * PI)
            abs(diff) > PI && orientation(segments[index]) > 0.0 -> (orientation(segments[index + 1]) + 2.0 * PI) - orientation(segment)
            else -> diff
          }
          println("newDiff: $newDiff")
          newDiff
        }
      }
      return diffs.sum() / diffs.size.toDouble()
      // segments.map { it.derivative()}
    }

    val ss = listOf(
      Segment(start = Vector2(x = 443.78, y = 542.16), end = Vector2(x = 442.92, y = 541.58)),
      Segment(start = Vector2(x = 442.92, y = 541.58), end = Vector2(x = 441.91, y = 541.02)),
      Segment(start = Vector2(x = 441.91, y = 541.02), end = Vector2(x = 440.74, y = 540.45)),
      Segment(start = Vector2(x = 440.74, y = 540.45), end = Vector2(x = 439.43, y = 539.87)),
      Segment(start = Vector2(x = 439.43, y = 539.87), end = Vector2(x = 437.95, y = 539.23)),
      Segment(start = Vector2(x = 437.95, y = 539.23), end = Vector2(x = 436.33, y = 538.53)),
      Segment(start = Vector2(x = 436.33, y = 538.53), end = Vector2(x = 434.56, y = 537.75)),
      Segment(start = Vector2(x = 434.56, y = 537.75), end = Vector2(x = 432.65, y = 536.88)),
      Segment(start = Vector2(x = 432.65, y = 536.88), end = Vector2(x = 430.63, y = 536.02)),
      Segment(start = Vector2(x = 430.63, y = 536.02), end = Vector2(x = 428.60, y = 535.08)),
      Segment(start = Vector2(x = 428.60, y = 535.08), end = Vector2(x = 426.66, y = 534.26)),
      Segment(start = Vector2(x = 426.66, y = 534.26), end = Vector2(x = 424.80, y = 533.58)),
      Segment(start = Vector2(x = 424.80, y = 533.58), end = Vector2(x = 423.04, y = 533.04)),
      Segment(start = Vector2(x = 423.04, y = 533.04), end = Vector2(x = 421.36, y = 532.63)),
      Segment(start = Vector2(x = 421.36, y = 532.63), end = Vector2(x = 419.78, y = 532.37)),
      Segment(start = Vector2(x = 419.78, y = 532.37), end = Vector2(x = 418.30, y = 532.25)),
      Segment(start = Vector2(x = 418.30, y = 532.25), end = Vector2(x = 416.90, y = 532.28)),
      Segment(start = Vector2(x = 416.90, y = 532.28), end = Vector2(x = 415.60, y = 532.47)),
      Segment(start = Vector2(x = 415.60, y = 532.47), end = Vector2(x = 414.36, y = 532.58)),
      Segment(start = Vector2(x = 414.36, y = 532.58), end = Vector2(x = 413.18, y = 532.61))
    )

    println(averageCurvature(ss))
    ss.forEachIndexed { index, segment ->
      if (index < ss.size - 1) {
        val diff = orientation(ss[index + 1]) - orientation(segment)
        if (abs(diff) > 1.0) {
          println(
            """
            segment: $segment
            other: ${ss[index + 1]}
            diff: $diff
            orientation(segment): ${orientation(segment)}
            orientation(other): ${orientation(ss[index + 1])}
            """.trimIndent()
          )
        }
      }
    }

    extend {
      drawer.fill = ColorRGBa.BLACK
      drawer.stroke = ColorRGBa.BLACK
      drawer.fontMap = font

      // val radius = width * 0.1
      // val center1 = Vector2(width * 0.35, height * 0.35)
      // val c1 = contour {
      //   moveTo(Vector2(cos(0.0), sin(0.0)) * radius + center1)
      //   for (degree in 0 until 270) {
      //     lineTo(Vector2(cos(Math.toRadians(degree.toDouble())), sin(Math.toRadians(degree.toDouble()))) * radius + center1)
      //   }
      // }
      //
      // val center2 = Vector2(width * 0.65, height * 0.65)
      // val c2 = contour {
      //   moveTo(Vector2(cos(PI), sin(PI)) * radius + center2)
      //   for (degree in -540 until -270) {
      //     lineTo(Vector2(cos(Math.toRadians(degree.toDouble())), sin(Math.toRadians(degree.toDouble()))) * radius + center2)
      //   }
      // }
      //
      // drawer.contours(listOf(c1, c2))
      // drawer.text("Curvature: ${averageCurvature(c1.segments)}", center1 + Vector2(0.0, height * 0.2))
      // drawer.text("Curvature: ${averageCurvature(c2.segments)}", center2 + Vector2(0.0, height * 0.2))

      // val c = FractalizedLine(listOf(Vector2(width * 0.5, height * 0.25), Vector2(width * 0.5, height * 0.5), Vector2(width * 0.5, height * 0.75))).perpendicularSubdivide(10, 0.05)
      // drawer.contour(c.contour)
      // println(averageCurvature(c.segments))

      // curvature: 0.2981785685268713, min: 559, max: 579, inf.length: 11.65426271865717
      drawer.segments(ss)
    }
  }
}

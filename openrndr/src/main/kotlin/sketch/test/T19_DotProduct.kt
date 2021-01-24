/**
 * I've never had any intuition about dot products, so here are some examples
 */
package sketch.test

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.math.Vector2
import org.openrndr.panel.elements.round
import org.openrndr.shape.LineSegment
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
  configure {
    width = 800
    height = 800
  }

  program {
    backgroundColor = ColorRGBa.WHITE
    val font = loadFont("/System/Library/Fonts/Supplemental/Tahoma.ttf", 12.0)

    extend {
      drawer.fill = ColorRGBa.BLACK
      drawer.stroke = ColorRGBa.BLACK

      drawer.fontMap = font

      val length = width * 0.05
      val angleA = PI * 0.125
      val vector = { angle: Double, vertex: Vector2, len: Double -> Vector2(cos(angle), sin(angle)) * len + vertex }

      val anglePositionMap = mapOf(
        PI * 0.125 to Vector2(width * 0.2, height * 0.3),
        PI * 0.625 to Vector2(width * 0.4, height * 0.3),
        PI * 0.875 to Vector2(width * 0.6, height * 0.3),
        PI * 1.125 to Vector2(width * 0.8, height * 0.3),
        PI * 1.375 to Vector2(width * 0.2, height * 0.7),
        PI * 1.625 to Vector2(width * 0.4, height * 0.7),
        PI * 1.875 to Vector2(width * 0.6, height * 0.7),
        PI * 1.99 to Vector2(width * 0.8, height * 0.7),
      )

      for ((angleB, vertex) in anglePositionMap) {
        val vectorA = vector(angleA, vertex, length)
        val vectorB = vector(angleB, vertex, length)
        drawer.lineSegment(LineSegment(vertex, vectorA))
        drawer.lineSegment(LineSegment(vertex, vectorB))
        drawer.text("a", vector(angleA, vertex, length * 1.1))
        drawer.text("b", vector(angleB + PI * 0.1, vertex, length * 1.1))

        // For this demonstration I think it makes more sense to consider the vectors from the origin rather than the "vertex"
        // drawer.text("a • b = ${vectorA.dot(vectorB).round(2)}", vertex + Vector2(0.0, height * 0.1))
        // drawer.text("a x️ b = ${vectorA.cross(vectorB).round(2)}", vertex + Vector2(0.0, height * 0.15))
        drawer.text("a • b = ${vector(angleA, Vector2.ZERO, length).dot(vector(angleB, Vector2.ZERO, length)).round(2)}", vertex + Vector2(0.0, height * 0.1))
        drawer.text("a x️ b = ${vector(angleA, Vector2.ZERO, length).cross(vector(angleB, Vector2.ZERO, length)).round(2)}", vertex + Vector2(0.0, height * 0.15))
      }
    }
  }
}

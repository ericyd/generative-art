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
    // find /System/Library/Fonts | grep ttf
    val font = loadFont("/System/Library/Fonts/Supplemental/Arial.ttf", 12.0)

    extend {
      drawer.fill = ColorRGBa.BLACK
      drawer.stroke = ColorRGBa.BLACK

      drawer.fontMap = font

      val length = width * 0.05
      val angleA = PI * 0.125
      val vector = { angle: Double, vertex: Vector2, len: Double -> Vector2(cos(angle), sin(angle)) * len + vertex }

      val anglePositionMap = mapOf(
        PI * 0.12 to Vector2(width * 0.05, height * 0.1),
        PI * 0.25 to Vector2(width * 0.25, height * 0.1),
        PI * 0.38 to Vector2(width * 0.45, height * 0.1),
        PI * 0.50 to Vector2(width * 0.65, height * 0.1),
        PI * 0.64 to Vector2(width * 0.85, height * 0.1),
        PI * 0.77 to Vector2(width * 0.05, height * 0.4),
        PI * 0.90 to Vector2(width * 0.25, height * 0.4),
        PI * 1.00 to Vector2(width * 0.45, height * 0.4),
        PI * 1.12 to Vector2(width * 0.65, height * 0.4),
        PI * 1.25 to Vector2(width * 0.85, height * 0.4),
        PI * 1.38 to Vector2(width * 0.05, height * 0.7),
        PI * 1.50 to Vector2(width * 0.25, height * 0.7),
        PI * 1.64 to Vector2(width * 0.45, height * 0.7),
        PI * 1.77 to Vector2(width * 0.65, height * 0.7),
        PI * 1.99 to Vector2(width * 0.85, height * 0.7),
      )

      for ((angleB, vertex) in anglePositionMap) {
        drawer.lineSegment(LineSegment(vertex, vector(angleA, vertex, length)))
        drawer.lineSegment(LineSegment(vertex, vector(angleB, vertex, length)))
        drawer.text("a", vector(angleA, vertex, length * 1.1))
        drawer.text("b", vector(angleB + PI * 0.075, vertex, length * 1.1))

        // For this demonstration I think it makes more sense to consider the vectors from the origin rather than the "vertex"
        // because the dot product is the sum of the product of the components (x,y), so it only makes sense to compare in relation to the origin
        drawer.text("a • b = ${vector(angleA, Vector2.ZERO, length).normalized.dot(vector(angleB, Vector2.ZERO, length).normalized).round(2)}", vertex + Vector2(0.0, height * 0.1))
        drawer.text("abs(a • b) = ${abs(vector(angleA, Vector2.ZERO, length).normalized.dot(vector(angleB, Vector2.ZERO, length).normalized).round(2))}", vertex + Vector2(0.0, height * 0.125))
        drawer.text("a x️ b = ${vector(angleA, Vector2.ZERO, length).normalized.cross(vector(angleB, Vector2.ZERO, length).normalized).round(2)}", vertex + Vector2(0.0, height * 0.15))
        drawer.text("abs(a x️ b) = ${abs(vector(angleA, Vector2.ZERO, length).normalized.cross(vector(angleB, Vector2.ZERO, length).normalized).round(2))}", vertex + Vector2(0.0, height * 0.175))
      }

      drawer.text("all vector calculations are performed on normalized vector quantities", width * 0.25, height * 0.05)
    }
  }
}

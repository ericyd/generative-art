package sketch

import org.openrndr.application
import org.openrndr.color.ColorHSVa
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.Drawer
import org.openrndr.draw.TransformTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.noise.random
import org.openrndr.extra.color.palettes.ColorSequence
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contains
import shape.Hexagon
import util.QTreeNode
import util.QuadTree
import util.QuadTreeNode
import util.grid
import util.timestamp
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 1000
  }

  program {
    val scaleAmount = 3
    fun scale(v: Double): Double { return v * scaleAmount.toDouble() }
    fun scale(v: Int): Int { return v * scaleAmount }
    val w = scale(width)
    val h = scale(height)
    val center = Vector2(w / 2.0, h / 2.0)
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    val rt = renderTarget(w, h, multisample = BufferMultisample.Disabled) {
      colorBuffer()
      depthBuffer()
    }

    class Walker(val start: Vector2, val baseAngle: Double, val rng: Random, val boundsRect: Rectangle, val velocity: Double = 5.0) {
      fun walkNoOverlap(length: Int, padding: Double, existingPoints: QuadTree, boundsColor: ShapeContour, drawer: Drawer, color: ColorHSVa) {
        val baseColor = color
          .shiftHue(random(-2.5, 2.5, rng))

        val saturate = random(0.2, 0.6, rng)
        val shade = random(0.0, 0.5, rng)

        var angle = genAngle(baseAngle)
        var cursor = start
        existingPoints.add(QTreeNode(cursor))
        for (i in 0 until length) {
          angle = genAngle(angle)
          val point = Vector2(
            cursor.x + cos(angle) * velocity,
            cursor.y + sin(angle) * velocity
          )

          val nearPointsOverlap = existingPoints
            .query<QuadTreeNode>(Rectangle.fromCenter(point, velocity * 2.0, velocity * 2.0))
            .any { it.position.distanceTo(point) < padding }
          if (nearPointsOverlap || !boundsRect.contains(point)) {
            continue
          }

          if (i > 1) {
            // drawer.stroke = baseColor.toRGBa()
            drawer.stroke = if (boundsColor.contains(point)) {
              baseColor.toRGBa()
            } else {
              baseColor
                .saturate(saturate)
                .shade(shade)
                .toRGBa()
            }
            drawer.lineSegment(cursor, point)
          }
          cursor = point
          existingPoints.add(QTreeNode(cursor))
        }
      }

      // generate a new angle based on the previous angle
      private fun genAngle(previousAngle: Double): Double {
        // encourage straight lines
        if (random(0.0, 1.0, rng) < 0.8) {
          return previousAngle
        }
        val chance = random(0.0, 1.0, rng)
        return if (chance < 1.0 / 6.0) { PI * 3.0 / 2.0 }
        else if (chance >= 1.0 / 6.0 && chance < 2.0 / 6.0) { PI / 2.0 }
        else if (chance >= 2.0 / 6.0 && chance < 3.0 / 6.0) { baseAngle }
        else if (chance >= 3.0 / 6.0 && chance < 4.0 / 6.0) { PI - baseAngle }
        else if (chance >= 4.0 / 6.0 && chance < 5.0 / 6.0) { PI + baseAngle }
        else { PI * 2.0 - baseAngle }
      }
    }

    extend {
      // get that rng
      val rng = Random(seed.toLong())

      val colors = listOf(
        ColorRGBa.BLACK,
        ColorRGBa.fromHex("91D71F"),
        ColorRGBa.fromHex("B0FF48"),
        ColorRGBa.fromHex("1B6C00"),
        ColorRGBa.BLACK,
        ColorRGBa.fromHex("4FC937"),
        ColorRGBa.fromHex("5F8253"),
      )
      val shatterStrokeGradient = ColorSequence(
        colors.mapIndexed { index, colorRGBa ->
          Pair(index.toDouble() / (colors.size.toDouble() - 1.0), colorRGBa)
        }
      )

      // Render to the render target, then scale and draw to screen
      drawer.isolatedWithTarget(rt) {
        drawer.ortho(rt)
        drawer.clear(ColorRGBa.BLACK)
        drawer.stroke = ColorRGBa.WHITE
        drawer.strokeWeight = scale(1.05)

        /**
         * Background "walker" pattern
         */
        val angle = PI / 9.0
        val boundsRect = Rectangle(0.0, 0.0, w.toDouble(), h.toDouble())
        val boundsColor = Hexagon(center, w * 0.33, PI / 6.0).contour
        val existingPoints = QuadTree(boundsRect, 10)
        val velocity = scale(3.629) // scale(random(3.0, 4.0, rng))
        val padding = scale(2.629) // velocity - scale(1.0)
        val stepSize = scale(5) // velocity.toInt() + padding.toInt()
        val jitter = { n: Double -> random(n - scale(1.0), n + scale(1.0), rng) }

        grid(0, w, 0, h, stepSize) { i: Int, j: Int ->
          // start at center and build outwards, alternating between NE/SE/SW/NW quadrants.
          // This avoids biasing towards a corner
          val xOffset = if ((i / stepSize) % 2 == 0) { i / 2.0 } else { i / -2.0 }
          val x = center.x + xOffset
          val yOffset = if ((j / stepSize) % 2 == 0) { j / 2.0 } else { j / -2.0 }
          val y = center.y + yOffset
          if (j == 0) println("$i of $w")
          val start = Vector2(jitter(x), jitter(y))
          val walker = Walker(start, angle, rng, boundsRect, velocity)
          val baseColor = shatterStrokeGradient.index(random(0.0, 1.0, rng)).toHSVa()
          walker.walkNoOverlap(
            scale(random(250.0, 500.0, rng).toInt()),
            padding,
            existingPoints,
            boundsColor,
            drawer,
            baseColor
          )
        }
      }

      // /**
      //  * Shadow around edges
      //  */
      // drawer.isolatedWithTarget(rt) {
      //   drawer.ortho(rt)
      //   drawer.shadeStyle = RadialGradient(ColorRGBa.TRANSPARENT, ColorRGBa.BLACK, offset=Vector2.ZERO,
      //     // length = 0.70, exponent = 2.5
      //     // length = 0.850, exponent = 3.5
      //     // length = 0.99, exponent = 4.5
      //     length = 1.05, exponent = 2.5
      //   )
      //   drawer.circle(Circle(center, hypot(w * 0.5, h * 0.5)))
      // }

      drawer.scale(width.toDouble() / rt.width, TransformTarget.MODEL)
      drawer.image(rt.colorBuffer(0))

      // `true` == capture screenshot
      if (true) {
        val targetFile = File("screenshots/$progName/${timestamp()}-seed-$seed.jpg")
        targetFile.parentFile?.let { file ->
          if (!file.exists()) {
            file.mkdirs()
          }
        }
        rt.colorBuffer(0).saveToFile(targetFile, async = false)
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        println("seed = $seed")
      }
    }
  }
}

// Another theme logo for Alex 😁
package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.LineCap
import org.openrndr.draw.LineJoin
import org.openrndr.draw.TransformTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.noise.random
import org.openrndr.extras.color.presets.ORANGE_RED
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import util.QTreeNode
import util.QuadTree
import util.QuadTreeNode
import util.grid
import util.timestamp
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 1000
  }

  program {
    val scaleAmount = 1
    fun scale(v: Double): Double { return v * scaleAmount.toDouble() }
    fun scale(v: Int): Int { return v * scaleAmount }
    val w = scale(width)
    val h = scale(height)
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    val rt = renderTarget(w, h, multisample = BufferMultisample.Disabled) {
      colorBuffer()
      depthBuffer()
    }

    class Walker(val start: Vector2, val baseAngle: Double, val rng: Random, val velocity: Double = 5.0) {
      fun walkNoOverlap(length: Int, padding: Double, existingPoints: QuadTree, bounds: Rectangle): List<Vector2> {
        var angle = genAngle(baseAngle)
        var cursor = start
        existingPoints.add(QTreeNode(cursor))
        // it is, of course, valid to include the start point in the line.
        // However, it seems to result in unintentional overlaps, and doesn't dramatically improve the look
        // val list = mutableListOf(cursor)
        val list = mutableListOf<Vector2>()
        for (i in 0 until length) {
          angle = genAngle(angle)
          val point = Vector2(
            cursor.x + cos(angle) * velocity,
            cursor.y + sin(angle) * velocity
          )

          val nearPointsOverlap = existingPoints
            .query<QuadTreeNode>(Rectangle.fromCenter(point, velocity * 2.0, velocity * 2.0))
            .any { it.position.distanceTo(point) < padding }
          if (nearPointsOverlap || !bounds.contains(point)) {
            continue
          }

          cursor = point
          existingPoints.add(QTreeNode(cursor))
          list.add(cursor)
        }
        return list
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

      val angle = PI / 6.0
      val bounds = Rectangle(w * 0.1, h * 0.1, w * 0.8, h * 0.8)
      val existingPoints = QuadTree(bounds, 10)
      val velocity = scale(50.0) // random(scale(30.0), scale(50.0), rng)
      val padding = velocity - scale(5.0)
      val jitter = { n: Double -> random(n - scale(1.0), n + scale(1.0), rng) }

      // From https://github.com/catppuccin/catppuccin/blob/62f97fcac8484c74e0bb0894edab4eca999c09f5/README.md?plain=1#L54-L68
      val palette = listOf(
        ColorRGBa.fromHex("F2CDCD"), // Flamingo
        ColorRGBa.fromHex("DDB6F2"), // Mauve
        ColorRGBa.fromHex("F5C2E7"), // Pink
        ColorRGBa.fromHex("E8A2AF"), // Maroon
        ColorRGBa.fromHex("F28FAD"), // Red
        ColorRGBa.fromHex("F8BD96"), // Peach
        ColorRGBa.fromHex("FAE3B0"), // Yellow
        ColorRGBa.fromHex("ABE9B3"), // Green
        ColorRGBa.fromHex("B5E8E0"), // Teal
        ColorRGBa.fromHex("96CDFB"), // Blue
        ColorRGBa.fromHex("89DCEB"), // Sky
      )

      // Render to the render target, then scale and draw to screen
      drawer.isolatedWithTarget(rt) {
        drawer.ortho(rt)
        drawer.clear(ColorRGBa.fromHex("161320"))
        drawer.strokeWeight = padding - scale(8.5)
        drawer.lineCap = LineCap.SQUARE
        drawer.lineJoin = LineJoin.BEVEL

        // Background "walker" pattern
        grid((w * 0.1).toInt(), (w * 0.8).toInt(), (h * 0.1).toInt(), (h * 0.8).toInt(), velocity.toInt() + padding.toInt()) { i: Int, j: Int ->
          // start at center to avoid biasing towards a corner
          val x = if (i % 2 == 0) { w / 2.0 + i / 2.0 } else { w / 2.0 - i / 2.0 }
          val y = if (j % 2 == 0) { h / 2.0 + j / 2.0 } else { h / 2.0 - j / 2.0 }
          if (j == 0) println("$i of $w")
          val start = Vector2(jitter(x), jitter(y))
          val walker = Walker(start, angle, rng, velocity)
          // TODO: would it be interesting to have randomized length?
          val line = walker.walkNoOverlap(scale(400), padding, existingPoints, bounds)

          val colorIndex = floor(random(0.0, palette.size.toDouble(), rng)).toInt()
          drawer.stroke = palette[colorIndex]
          drawer.lineStrip(line)
        }
      }

      drawer.scale(width.toDouble() / rt.width, TransformTarget.MODEL)
      drawer.image(rt.colorBuffer(0))

      // Change to `true` to capture screenshot
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

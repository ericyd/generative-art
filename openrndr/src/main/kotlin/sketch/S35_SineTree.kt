/**
 * Create new works here, then move to parent package when complete
 */
package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.draw.isolated
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.color.palettes.ColorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Rectangle
import util.QTreeNode
import util.QuadTree
import util.QuadTreeNode
import util.rotatePoint
import util.timestamp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 800
    height = 800
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 4.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }

    backgroundColor = ColorRGBa.WHITE

    val spectrum = ColorSequence(
      listOf(
        0.0 to ColorRGBa.fromHex("0D1D2B"),
        0.2 to ColorRGBa.fromHex("171E38"),
        0.4 to ColorRGBa.fromHex("26233D"),
        0.6 to ColorRGBa.fromHex("403854"),
        0.8 to ColorRGBa.fromHex("31425E"),
        1.0 to ColorRGBa.fromHex("4B6172")
      )
    )

    data class Config(
      val drawer: Drawer,
      val origin: Vector2,
      val angle: Double,
      val length: Int,
      val depth: Int,
      val strokeWeightMax: Double,
      val qtree: QuadTree,
      val rng: Random,
      val period: Double,
      val amplitude: Double,
    )

    fun nextPoint(point: Vector2, angle: Double, origin: Vector2, period: Double, amplitude: Double): Vector2 {
      var newPoint = point + Vector2(cos(angle), sin(angle))
      newPoint = rotatePoint(newPoint, -angle, origin)
      newPoint += Vector2(0.0, sin(newPoint.x / period) * amplitude)
      newPoint = rotatePoint(newPoint, angle, origin)
      return newPoint
    }

    val boundingRect = { point: Vector2 -> Rectangle.fromCenter(point, 5.0, 5.0) }

    fun drawSine(config: Config) {
      val (drawer, origin, angle, length, depth, strokeWeightMax, qtree, rng, period, amplitude) = config
      var cursor = origin
      val strokeWeightMin = strokeWeightMax / 10.0
      val points = mutableListOf<QTreeNode>()
      val configs = mutableListOf<Config>()

      // calculate points that do not intersect
      for (i in 0..length) {
        val next = nextPoint(cursor, angle, origin, period, amplitude)
        if (i > 5 && qtree.query<QuadTreeNode>(boundingRect(next)).size > 0) {
          break
        }
        drawer.lineSegment(cursor, next)
        cursor = next
        points.add(QTreeNode(cursor))
      }

      // draw points, with strokeWeight scaled from thick to thin
      drawer.isolated {
        cursor = origin
        drawer.lineCap = LineCap.ROUND
        for ((i, point) in points.withIndex()) {
          // calculate color
          val macroDivision = depth / 5.0
          val microDivision = i.toDouble() / points.size / 5.0
          drawer.stroke = spectrum.index(macroDivision + microDivision)

          // interpolate stroke weight
          val strokeWeight = map(0.0, 1.0, strokeWeightMax, strokeWeightMin, i.toDouble() / points.size)
          drawer.strokeWeight = strokeWeight

          drawer.lineSegment(cursor, point.position)

          cursor = point.position

          // create child branches if the gods allow it
          val shouldSpawn = depth < 5 && random(0.0, 1.0, rng) < 0.08
          if (shouldSpawn) {
            var offsetAngle = PI * random(0.15, 0.45, rng)
            if (random(0.0, 1.0, rng) < 0.5) {
              offsetAngle = -offsetAngle
            }
            val newAngle = angle + offsetAngle
            val remainingLength = length - i
            configs.add(
              Config(
                drawer = drawer,
                origin = cursor,
                angle = newAngle,
                length = (remainingLength * random(0.6, 0.85, rng)).toInt(),
                depth = depth + 1,
                strokeWeightMax = strokeWeight,
                qtree = qtree,
                rng = rng,
                period = random(period * 0.5, period * 0.75, rng),
                amplitude = random(amplitude * 0.6, amplitude * 0.95, rng)
              )
            )
          }
        }
      }

      // prevent querying/conflicting with the current line
      qtree.addAll(points)

      // prevent "child" lines from cutting off the parent line (i.e. draw the parent before any children are drawn)
      for (config in configs) {
        drawSine(config)
      }
    }

    extend {
      val rng = Random(seed)
      val qtree = QuadTree(Rectangle(0.0, 0.0, width.toDouble(), height.toDouble()), 10)

      drawSine(
        Config(
          drawer = drawer,
          origin = Vector2(width * 0.5, 0.0),
          angle = random(PI * 0.4, PI * 0.6, rng),
          length = (width * random(1.0, 1.4, rng)).toInt(),
          depth = 0,
          strokeWeightMax = 20.0,
          qtree = qtree,
          rng = rng,
          period = random(50.0, 80.0, rng),
          amplitude = random(1.5, 1.90, rng)
        )
      )

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
      }
    }
  }
}

package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.random
import org.openrndr.extras.color.palettes.ColorSequence
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.contour
import util.timestamp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 600
    height = 800
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 5.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }

    val bleed = 0.0
    val bounds = Rectangle(width * -bleed, height * -bleed, width * (1.0 + bleed), height * (1.0 + bleed))

    val spectrum = ColorSequence(listOf(
      0.1 to ColorRGBa.fromHex("E1EAFA"),
      0.3 to ColorRGBa.fromHex("D6DEE3"),
      0.5 to ColorRGBa.fromHex("DCECEF"),
      0.6 to ColorRGBa.fromHex("ECCFAA"),
      0.7 to ColorRGBa.fromHex("A4E5CD"),
      0.9 to ColorRGBa.fromHex("7192BC"),
      1.0 to ColorRGBa.fromHex("DB8062"),
    ))

    fun color(value: Double, rng: Random): ColorRGBa {
      val deviation = if (value < 0.5) abs(value) else 1.0 - value
      val index = gaussian(value, deviation, rng)
      val base = spectrum.index(index)
      return base.shade(random(0.5, 1.1, rng))
    }

    backgroundColor = ColorRGBa.fromHex("FCFCFD")

    fun drawTiledSquare(drawer: Drawer, rng: Random, tileHeight: Double, tileWidth: Double, center: Vector2, maxWidth: Double) {
      val numberOfLayers = maxWidth / tileHeight / 2.0

      for (edgeNumber in 1 until ceil(numberOfLayers + 1.0).toInt()) {
        val distance = (edgeNumber.toDouble() / numberOfLayers) * maxWidth

        // fit tiles to edge length
        val nTiles = ceil(distance / tileWidth)
        val tileWidth = distance / nTiles

        val halfEdge = distance * 0.5

        for (angle in listOf(0.0, PI * 0.5, PI, PI * 1.5)) {
          val baseColor = color(random(0.0, 1.0, rng), rng)

          val perpendicularToEdge = Vector2(cos(angle), sin(angle))
          val quarterOffset = angle - PI * 0.5
          val parallelToEdge = Vector2(cos(quarterOffset), sin(quarterOffset))
          val edgeCenterPoint = perpendicularToEdge * halfEdge
          val edgeCenterToCorner = parallelToEdge * halfEdge

          var pos = center + edgeCenterPoint - edgeCenterToCorner

          List(nTiles.toInt()) {
            drawer.fill = baseColor.shade(random(0.85, 1.15, rng))
            drawer.stroke = baseColor.shade(0.5)
            drawer.contour(contour {
              moveTo(pos)
              lineTo(cursor + parallelToEdge * tileWidth)
              lineTo(cursor + perpendicularToEdge * -tileHeight)
              lineTo(cursor + parallelToEdge * -tileWidth)
              close()
            })
            pos += parallelToEdge * tileWidth
          }
        }
      }
    }

    extend {
      val rng = Random(seed)

      val gridDimension = 20.0
      val cellSize = round(width / gridDimension)
      val maxCellsPerSquare = gridDimension / 2.0
      val xDim = gridDimension - 1.0
      val yDim = round(height / cellSize) - 1.0

      // More "regular"
      // val tileHeight = cellSize / 4.0
      // val tileWidth = cellSize / 1.5
      // More "irregular"
      val tileHeight = 10.0
      val tileWidth = 15.0

      val rects = mutableListOf<Rectangle>()
      var tries = 0

      while (tries < 30000) {
        val x = round(random(0.0, xDim, rng)) * cellSize
        val y = round(random(0.0, yDim, rng)) * cellSize
        val size = round(random(1.0, maxCellsPerSquare, rng)) * cellSize
        val center = Vector2(x + size / 2.0, y + size / 2.0)

        val rect = Rectangle(x, y, size, size)
        if (rects.none { it.intersects(rect.scale(0.95)) }) {
          drawTiledSquare(drawer, rng, tileHeight, tileWidth, center, size)
          rects.add(rect)
          tries = 0
        }

        tries += 1
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
      }
    }
  }
}

package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.random
import org.openrndr.extra.color.palettes.ColorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Rectangle
import org.openrndr.shape.contour
import util.timestamp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
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

    val bleed = 0.4
    val bounds = Rectangle(width * -bleed, height * -bleed, width * (1.0 + bleed), height * (1.0 + bleed))

    val spectrum = ColorSequence(
      listOf(
        0.1 to ColorRGBa.fromHex("DCECEF"),
        0.3 to ColorRGBa.fromHex("E1EAFA"),
        0.5 to ColorRGBa.fromHex("7192BC"),
        0.6 to ColorRGBa.fromHex("D6DEE3"),
        0.7 to ColorRGBa.fromHex("A4E5CD"),
        0.9 to ColorRGBa.fromHex("ECCFAA"),
        1.0 to ColorRGBa.fromHex("DB8062"),
      )
    )

    fun color(value: Double, rng: Random): ColorRGBa {
      // this isn't "right" but it looks pretty nice anyway
      val colorMin = map(bounds.y, bounds.y + bounds.height, 1.0, -0.3, value)
      val colorMax = colorMin + 0.2
      var colorIndex = random(colorMin, colorMax, rng)
      val deviation = if (colorIndex < 0.5) abs(colorIndex) else 1.0 - colorIndex
      colorIndex = gaussian(colorIndex, deviation, rng)
      val base = spectrum.index(colorIndex)
      return base.shade(random(0.5, 1.1, rng))
    }

    backgroundColor = ColorRGBa.fromHex("FCFCFD")

    fun randomPointInRect(rect: Rectangle, rng: Random): Vector2 {
      return Vector2(
        random(rect.x, rect.x + rect.width, rng),
        random(rect.y, rect.y + rect.height, rng)
      )
    }

    fun drawTiledCircle(drawer: Drawer, rng: Random, tileHeight: Double, tileWidth: Double, center: Vector2, radius: Double? = null) {
      val radius = radius ?: random(width * 0.125, width * 0.5, rng)

      for (rad in 0 until radius.toInt() step tileHeight.toInt()) {
        val outerRadius = rad + tileHeight / 2.0
        val innerRadius = rad - tileHeight / 2.0
        val circumference = PI * rad * 2.0
        val nTiles = (circumference / tileWidth).toInt()
        val tileAngularWidth = 2.0 * PI / nTiles
        var angle = random(-PI, PI, rng)

        val baseColor = color(rad / radius, rng)
        List(nTiles) {
          drawer.fill = baseColor.shade(random(0.85, 1.15, rng))
          drawer.stroke = baseColor.shade(0.5)
          drawer.contour(
            contour {
              val point0 = Vector2(cos(angle), sin(angle)) * outerRadius + center
              moveTo(point0)

              val point1 = Vector2(cos(angle + tileAngularWidth), sin(angle + tileAngularWidth)) * outerRadius + center
              /**
               * arcTo is pretty undocumented but I guess the params are self-explanatory (???)
               * crx, cry is the circle radius in x and y dimensions
               * angle is apparently the angular rotation of the arc?
               * largeArcFlag makes the arc travel the longest possible distance to close the arc when true
               * sweepFlag makes the arc bend outwards/inwards
               * end is the point at which the arc ends
               */
              arcTo(
                crx = outerRadius,
                cry = outerRadius,
                angle = Math.toDegrees(tileAngularWidth),
                largeArcFlag = false,
                sweepFlag = true,
                end = point1
              )

              val point2 = Vector2(cos(angle + tileAngularWidth), sin(angle + tileAngularWidth)) * innerRadius + center
              lineTo(point2)

              val point3 = Vector2(cos(angle), sin(angle)) * innerRadius + center
              arcTo(
                crx = innerRadius,
                cry = innerRadius,
                angle = Math.toDegrees(tileAngularWidth),
                largeArcFlag = false,
                sweepFlag = false,
                end = point3
              )

              close()
            }
          )
          angle += tileAngularWidth
        }
      }
    }

    // TODO: explore how to position the circles more intentionally. Perhaps they follow a curve or perhaps use circle packing so they do not overlap?
    extend {
      val rng = Random(seed)

      val tileHeight = 10.0
      val tileWidth = 15.0

      val bigRadius = bounds.dimensions.length
      val bigCenter = randomPointInRect(bounds, rng)
      drawTiledCircle(drawer, rng, tileHeight, tileWidth, bigCenter, bigRadius)

      var innerBounds = bounds.scale(0.5)
      innerBounds = innerBounds.moved(innerBounds.center - bounds.center)
      List(random(2.0, 4.0, rng).toInt()) {
        val center = randomPointInRect(innerBounds, rng)
        drawTiledCircle(drawer, rng, tileHeight, tileWidth, center)
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
      }
    }
  }
}

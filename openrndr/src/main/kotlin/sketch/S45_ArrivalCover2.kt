package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.shape.contour
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extras.color.palettes.ColorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.math.map
import util.timestamp
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.cos
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 1000
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    seed = 1417538202 // good design
    println("seed = $seed")

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 6.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
    }

    backgroundColor = ColorRGBa.WHITE
    val colors = listOf(
      "23214A",
      "94EF86",
      "F3ED76",
      "E15F33",
      "BE59E7",
      "E56DB1",
      "FF9B4E",
      "A2F2EC",
      "9C99E5",
      "005D7E",
    )

    extend {
      val rng = Random(seed)
      val spectrum = ColorSequence(colors.mapIndexed { index, hex ->
        Pair(map(0.0, colors.size - 1.0, 0.0, 1.0, index.toDouble()), ColorRGBa.fromHex(hex))
      })
      // using shuffled colors makes it more of a true "generative" piece,
      // but I like the hardcoded spectrum so I'm leaving it untouched
//      val spectrum = ColorSequence(colors.shuffled(rng).mapIndexed { index, hex ->
//        Pair(map(0.0, colors.size - 1.0, 0.0, 1.0, index.toDouble()), ColorRGBa.fromHex(hex))
//      })
      val strokeWeight = width * 0.003
      drawer.stroke = backgroundColor
      drawer.strokeWeight = strokeWeight
      
      val tileHeight = 10.0
      val tileWidth = 55.0
      val center = Vector2(width / 2.0, height / 2.0)
      val radius = hypot(width.toDouble(), height.toDouble())

      for (rad in 0 until radius.toInt() step (tileHeight * 1.5).toInt()) {
        val outerRadius = rad + tileHeight / 2.0
        val innerRadius = rad - tileHeight / 2.0
        val circumference = PI * rad * 2.0
        val nTiles = (circumference / tileWidth).toInt()
        val tileAngularWidth = 2.0 * PI / nTiles
        var angle = random(-PI, PI, rng)

        List(nTiles) {
          // line of interest is y=x+height
          // ~100% probability when "above" the line, i.e. x > height-y
          // 0 to 10% probability whe "below" the line, i.e. x < height-y
          // note: height = width
          // let's say, "lower bound" is y=x+(height*0.75), and upper bound is y=x+(height*1.25)
          // in that range, probability goes from 0.0 to 1.0
          // the way the "map" call works is a little strange IMO, but basically the y offset (height*0.75, height*1.25) is the "before" left/right values,
          // and then the "after" left/right values are the probability range.
          // x+y is the determinant value because, well, I'm not 100% sure. It's something to do with the axis orientation in OPENRNDR.
          val point0 = Vector2(cos(angle), sin(angle)) * outerRadius + center
          val probabilityCutoff = clamp(map(height * 0.55, height * 1.25, 1.0, 0.0, point0.x+point0.y), 0.0, 1.0)
          if (random(0.0, 1.02, rng) > probabilityCutoff) {
            // ahhhh, this is "wrong" because the angle might e.g. start at PI and then grow, therefore will never be in range [0,1]
            // however, this actually creates some cool artifacts, I think it is probably cooler than if it was "correct".
            // this is one of those "beautiful mistakes" I think!
            // of course... I still want to see what the "correct" version looks like 🙄
            drawer.fill = spectrum.index(map(-PI, PI, 0.0, 1.0, angle))

            // this is the "corrected" version
//            drawer.fill = if (angle > PI) {
//              spectrum.index(map(-PI, PI, 0.0, 1.0, angle - PI * 2.0))
//            } else if (angle < -PI) {
//              spectrum.index(map(-PI, PI, 0.0, 1.0, angle + PI * 2.0))
//            } else {
//              spectrum.index(map(-PI, PI, 0.0, 1.0, angle))
//            }

            // and this is fully-random
            // this was the first iteration, but compared to either of the above options, it is significantly uglier
//            drawer.fill = spectrum.index(random(0.0, 1.0, rng))
            drawer.contour(
              contour {
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
          }
          angle += tileAngularWidth
        }
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
      }
    }
  }
}

/**
 * Create new works here, then move to parent package when complete
 */
package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extras.color.palettes.ColorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.math.map
import util.grid
import util.timestamp
import kotlin.math.PI
import kotlin.math.hypot
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
//    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    // this is the one I like!
    var seed = 52716722
    println("seed = $seed")

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 5.0
      captureEveryFrame = true
      name = "screenshots/$progName/${timestamp()}-seed-$seed.png"
    }

    backgroundColor = ColorRGBa.WHITE
    val colors = listOf(
      ColorRGBa.fromHex("A2F2EC"),
      ColorRGBa.fromHex("F3ED76"),
      ColorRGBa.fromHex("FF9B4E"),
      ColorRGBa.fromHex("23214A"),
      ColorRGBa.fromHex("E56DB1"),
      ColorRGBa.fromHex("94EF86"),
      ColorRGBa.fromHex("9C99E5"),
      ColorRGBa.fromHex("005D7E"),
      ColorRGBa.fromHex("E15F33"),
      ColorRGBa.fromHex("BE59E7"),
    )


    extend {
      val rng = Random(seed)
      val spectrum = ColorSequence(colors.shuffled(rng).mapIndexed { index, color ->
        Pair(map(0.0, colors.size - 1.0, 0.0, 1.0, index.toDouble()), color)
      })
      val strokeWeight = width * 0.003
      // kind of arbitrary, this just looks nice
      val xCount = 9
      val yCount = xCount * 4
      val cellWidth = (width / xCount.toDouble()) // + strokeWeight * 2.0
      val cellHeight = (height / yCount.toDouble()) // + strokeWeight * 2.0
      drawer.stroke = backgroundColor
      
      drawer.strokeWeight = strokeWeight
      
      grid(0, width, width / xCount, 0, height, height / yCount) { x, y ->
        // line of interest is y=x+height
        // ~100% probability when "above" the line, i.e. x > height-y
        // 0 to 10% probability whe "below" the line, i.e. x < height-y
        // note: height = width
        // let's say, "lower bound" is y=x+(height*0.75), and upper bound is y=x+(height*1.25)
        // in that range, probability goes from 0.0 to 1.0
        // the way the "map" call works is a little strange IMO, but basically the y offset (height*0.75, height*1.25) is the "before" left/right values,
        // and then the "after" left/right values are the probability range.
        // x+y is the determinant value because, well, I'm not 100% sure. It's something to do with the axis orientation in OPENRNDR.
        val probabilityCutoff = clamp(map(height * 0.75, height * 1.05, 1.0, 0.0, x+y), 0.0, 1.0)
        if (random(0.0, 1.02, rng) > probabilityCutoff) {
          drawer.fill = spectrum.index(random(0.0, 1.0, rng))
          drawer.rectangle(x - strokeWeight, y - strokeWeight, cellWidth, cellHeight)
        } 
      }

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
      }
    }
  }
}

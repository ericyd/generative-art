/**
 * Layered curl noise,
 * this time used to make a Van Gogh impersonator!
 *
 * We got colors being grabbed using Perlin
 * We got line styles being grabbed using Perlin
 * We got patterns being generated using Perlin
 *
 * holy cow, maybe I like Perlin!
 */
package sketch.flow

import color.randomColor
import extensions.CustomScreenshots
import noise.curlOfCurl
import noise.mapToRadians
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.mix
import org.openrndr.draw.LineCap
import org.openrndr.extra.noise.perlin
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.math.mix
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import shape.grid
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.random.Random

data class ColorContour(val color: ColorRGBa, val contour: ShapeContour)

fun main() = application {
  configure {
    width = 1000
    height = 1000
  }

  program {
    // wow, sometimes you just gotta read the code! `extend` returns a reference to the extension,
    // which means we can manually trigger it as desired in the event loop!
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    val screenshots = extend(CustomScreenshots()) {
      quitAfterScreenshot = false
      scale = 2.0
      folder = "screenshots/$progName/"
      captureEveryFrame = true
    }

    // Static drawing properties
    val stepSize = 3
    val jitter = stepSize * 0.7
    val strokeWeightRange = Pair(1.0, 3.0)
    val meanLineLength = 20
    val meanOpacity = 0.35
    val origin = Vector2(width / 2.0, height / 2.0)
    val diagonal = hypot(width.toDouble(), height.toDouble())
    val bounds = width / 4

    val skyPalette = listOf(
      ColorRGBa.fromHex("E9CBF1"), // lavender-ish
      ColorRGBa.fromHex("FEFED2"), // light yellow, almost white
      ColorRGBa.fromHex("F8DF81"), // straight up yellow
      ColorRGBa.fromHex("003C5C"), // dark grayish blue
      ColorRGBa.fromHex("1B264F"), // dark saturated blue
      ColorRGBa.fromHex("274690"), // medium, almost-royal blue
      ColorRGBa.fromHex("F2AE6E"), // orange-yellow
      ColorRGBa.fromHex("A7BBEC") // light grayish blue
    )
    val groundPalette = listOf(
      ColorRGBa.fromHex("C6CAED"), // lavender-ish
      ColorRGBa.fromHex("3A2218"), // brown
      ColorRGBa.fromHex("C16200"), // rusty orange
      ColorRGBa.fromHex("881600"), // dark red
      ColorRGBa.fromHex("383005"), // olive
      ColorRGBa.fromHex("003049"), // blue
      ColorRGBa.fromHex("F7DBA7"), // pale yellow
      ColorRGBa.fromHex("090D07") // very dark green
    )

    // Seed must be set before the loop, and at the end of the loop,
    // to be able to set screenshots.append correctly
    var seed = random(1.0, Int.MAX_VALUE.toDouble()).toLong() // know your seed 😛
    screenshots.append = "seed-$seed"

    extend {
      val rand = Random(seed)
      val bg = randomColor(
        listOf(skyPalette[3], skyPalette[4], skyPalette[5], skyPalette[7], groundPalette[1], groundPalette[4], groundPalette[7]),
        rand
      )
      backgroundColor = bg

      // generate random noise scales for the three noise "octaves"
      val scaleOne = random(300.0, 800.0, rand)
      val scaleTwo = random(20.0, 100.0, rand)
      val scaleThree = random(100.0, 300.0, rand)

      // generate noise influences, which dictate how much each "octave" influences the overall vector field
      val influenceOne = Pair(random(0.01, 0.3, rand), random(0.3, 0.60, rand))
      val influenceTwo = Pair(random(0.01, 0.3, rand), random(0.3, 0.60, rand))
      val influenceThree = Pair(random(0.01, 0.3, rand), random(0.3, 0.60, rand))

      // generate "noise map scales", which change how the noiseScales are distributed spatially
      val noiseMapScaleOne = random(scaleOne, scaleTwo, rand)
      val noiseMapScaleTwo = random(scaleTwo, scaleThree, rand)
      val noiseMapScaleThree = random(scaleThree, scaleOne, rand)

      // other mildly-randomized properties
      val epsilon = random(0.5, 2.0, rand)

      /**
       * mixNoise1 creates three "octaves" of curl noise
       * and mixes them together based on the cursor position.
       * The noise is mixed with a straight vector pointing in the direction of the dominant "angle"
       * for the cursor. As the cursor moves away from the center, the mix of noise to straight angle changes.
       */
      fun mixNoise(cursor: Vector2): Vector2 {
        val distPct = cursor.distanceTo(origin) / diagonal
        // val scale = map(-1.0, 1.0, scaleOne, scaleTwo, simplex(seed.toInt(), cursor / mix(scaleOne, scaleTwo, distPct)))
        // return perlinCurlOfCurl(seed.toInt(), cursor / scale, epsilon).normalized

        val ratioOne = map(
          -1.0, 1.0,
          influenceOne.second, influenceOne.first,
          perlin(seed.toInt(), cursor / noiseMapScaleOne)
        )
        val ratioTwo = map(
          -1.0, 1.0,
          influenceTwo.first, influenceTwo.second,
          perlin(seed.toInt(), cursor / noiseMapScaleTwo)
        )
        val ratioThree = map(
          -1.0, 1.0,
          influenceThree.first, influenceThree.second,
          perlin(seed.toInt(), cursor / noiseMapScaleThree)
        )

        val curlFunc = { i: Int, x: Double, y: Double ->
          mapToRadians(-1.0, 1.0, perlin(i, x, y))
        }

        // layer curl noise together
        val res = curlOfCurl(curlFunc, seed.toInt(), cursor / scaleOne, epsilon) * ratioOne +
          curlOfCurl(curlFunc, seed.toInt(), cursor / scaleTwo, epsilon) * ratioTwo +
          curlOfCurl(curlFunc, seed.toInt(), cursor / scaleThree, epsilon) * ratioThree

        return res.normalized
      }

      /**
       * val should be in [-1.0, 1.0] range
       * Maps value to the length of the palette and picks a color from the corresponding index
       * then, mixes that color a little bit with another *random* color from the palette
       */
      fun mapPalette(value: Double, palette: List<ColorRGBa>): ColorRGBa {
        val primaryIndex = floor(map(-1.0, 1.0, 0.0, palette.size.toDouble(), value)).toInt()
        val randomIndex = floor(random(0.0, palette.size.toDouble(), rand)).toInt()
        return mix(palette.get(primaryIndex), palette.get(randomIndex), 0.55).opacify(random(meanOpacity * 0.7, meanOpacity * 1.3, rand))
      }

      /******************************
       *
       * Define colored contours
       *
       *******************************/
      val colorContours: List<ColorContour> = grid(
        0 - bounds, width + bounds,
        0 - bounds, height + bounds,
        stepSize
      ) { x: Double, y: Double ->
        val lineLength = random(meanLineLength * 0.3, meanLineLength * 1.7, rand).toInt()
        val colorPerlinThing = perlin(
          seed.toInt(),
          x / mix(scaleOne / 1.5, scaleTwo / 1.5, 0.35),
          y / mix(scaleTwo / 1.5, scaleThree / 1.5, 0.65)
        )
        val whichPalette = if (y + colorPerlinThing * height * 0.2 < height * 0.65) skyPalette else groundPalette
        val color = mapPalette(colorPerlinThing, whichPalette)
        val contr = contour {
          moveTo(
            x + random(-jitter, jitter, rand),
            y + random(-jitter, jitter, rand)
          )

          List(lineLength) {
            lineTo(cursor + mixNoise(cursor))
          }
        }
        ColorContour(color, contr)
      }

      println(
        """aww yeah, about to render...
        | seed = $seed
        | scaleOne = $scaleOne
        | scaleTwo = $scaleTwo
        | scaleThree = $scaleThree
        | influenceOne = ${influenceOne.first}, ${influenceOne.second}
        | influenceTwo = ${influenceTwo.first}, ${influenceTwo.second}
        | influenceThree = ${influenceThree.first}, ${influenceThree.second}
        | noiseMapScaleOne = $noiseMapScaleOne
        | noiseMapScaleTwo = $noiseMapScaleTwo
        | noiseMapScaleThree = $noiseMapScaleThree
      """.trimMargin()
      )

      /******************************
       *
       * Drawing
       *
       *******************************/
      drawer.clear(bg)
      drawer.fill = null
      drawer.stroke = null
      drawer.lineCap = LineCap.ROUND

      // interior contours
      colorContours.chunked(50).forEach { ccs ->
        drawer.strokeWeight = random(strokeWeightRange.first, strokeWeightRange.second, rand)
        ccs.forEach { cc ->
          drawer.stroke = cc.color
          drawer.contour(cc.contour)
        }
      }

      // set seed for next iteration
      seed = random(1.0, Int.MAX_VALUE.toDouble()).toLong()
      screenshots.append = "seed-$seed"
    }
  }
}

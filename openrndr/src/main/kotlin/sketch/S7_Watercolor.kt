/**
 * Of course, the inimitable Tyler Hobbes:
 * https://tylerxhobbs.com/essays/2017/a-generative-approach-to-simulating-watercolor-paints
 */
package sketch

import FilmGrain
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.perlin
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.contour
import shape.FractalizedPolygon
import shape.ellipse
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 1000
  }

  program {
    // In future, consider implementing own screenshot mechanism that could run after each draw loop
    // screenshot: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-extensions/src/main/kotlin/org/openrndr/extensions/Screenshots.kt
    // timestamp: https://github.com/openrndr/openrndr/blob/9f17eb3c24813454cbad1a99d697cd279fa80d96/openrndr-core/src/main/kotlin/org/openrndr/utils/NamedTimestamp.kt
    extend(Screenshots()) {
      quitAfterScreenshot = false
      scale = 2.0
    }
    val bg = ColorRGBa(0.95, 0.95, 0.95, 1.0)
    backgroundColor = bg

    val center = Vector2(width / 2.0, height / 2.0)
    val opacity = 0.02
    var seed = random(1.0, Long.MAX_VALUE.toDouble()).toLong()
    val rand = Random(seed)
    println("seed = $seed")

    val colors = listOf(
      ColorRGBa.fromHex("D4E6ED").opacify(opacity),
      ColorRGBa.fromHex("25A7F8").opacify(opacity),
      ColorRGBa.fromHex("F4B47B").opacify(opacity),
      ColorRGBa.fromHex("F67D51").opacify(opacity),
      ColorRGBa.fromHex("741A29").opacify(opacity)
    )

    val nBlobs = 60
    val nLayers = 20
    val subdivisions = 5
    val paintSplotches: List<PaintSplotch> = List(nBlobs) {
      val paintCenter = Vector2(
        random(0.0, width.toDouble(), rand),
        random(0.0, height.toDouble(), rand)
      )
      val w = map(0.0, height.toDouble(), 300.0, 800.0, paintCenter.y)
      val h = map(0.0, height.toDouble(), 300.0, 100.0, paintCenter.y)
      val base = FractalizedPolygon(ellipse(paintCenter, w, h, 10), rand = rand)
      base.fractalize(3)

      val splotches = List(nLayers) {
        base.clone().fractalize(subdivisions).shape
      }

      val color = colors
        .get(
          map(-1.0, 1.0, 0.0, colors.size.toDouble(), perlin(seed.toInt(), paintCenter.x / 200.0, paintCenter.y / 200.0)).toInt()
        )
        .shade(random(0.95, 1.05, rand))

      PaintSplotch(color, splotches)
    }

    /*
    Create bloom filter requisites
    */
    // -- create offscreen render target
    val offscreen = renderTarget(width, height) {
      colorBuffer()
      depthBuffer()
    }
    // -- create bloom filter
    val filter = FilmGrain()

    // -- create colorbuffer to hold filter results
    val filtered = colorBuffer(width, height)

    val useFilterForShapes = true

    extend {
      if (useFilterForShapes) {
        // -- draw to offscreen buffer
        drawer.isolatedWithTarget(offscreen) {
          clear(bg)
          drawer.stroke = null
          paintSplotches.forEach {
            drawer.fill = it.color
            // drawer.shadeStyle = radialGradient(it.color.opacify(0.5), it.color)
            it.splotches.forEach { drawer.contour(it) }
          }
        }
        // -- set filter parameters
        filter.grainStrength = 010.20
        filter.grainRate = 0.010
        filter.grainPitch = 1.50
        filter.grainLiftRatio = 03.10
        filter.colorLevel = 2.0

        // -- filter offscreen's color buffer into filtered
        filter.apply(offscreen.colorBuffer(0), filtered)
        drawer.image(filtered)
      } else {
        drawer.stroke = null
        paintSplotches.forEach {
          drawer.fill = it.color
          // drawer.stroke = it.color.opacify(100.0)
          // drawer.shadeStyle = radialGradient(it.color.opacify(0.5), it.color)
          it.splotches.forEach { drawer.contour(it) }
        }
      }
    }
  }
}

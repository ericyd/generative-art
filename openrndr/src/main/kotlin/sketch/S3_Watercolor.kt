/**
 * Of course, the inimitable Tyler Hobbes:
 * https://tylerxhobbs.com/essays/2017/a-generative-approach-to-simulating-watercolor-paints
 */
package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.fx.blur.Bloom
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import shape.DeJongAttractor
import shape.FractalizedPolygon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class PaintSplotch(val color: ColorRGBa, val splotches: List<ShapeContour>)

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
    backgroundColor = ColorRGBa.BLACK

    val center = Vector2(width / 2.0, height / 2.0)
    val opacity = 0.02
    var seed = random(1.0, Long.MAX_VALUE.toDouble()).toLong()
    // good seeds
    // seed = 823634897311705088
    // seed = 1701252600522147840
    seed = 5842774724912839680
    val rand = Random(seed)
    println("seed: $seed")

    // val colors = listOf(
    //   ColorRGBa.fromHex("4277A9").opacify(opacity),
    //   ColorRGBa.fromHex("D1E7F0").opacify(opacity),
    //   ColorRGBa.fromHex("EFA667").opacify(opacity),
    //   ColorRGBa.fromHex("191516").opacify(opacity),
    //   ColorRGBa.fromHex("874D93").opacify(opacity)
    // )

    val colors = listOf(
      ColorRGBa.fromHex("E5B769").opacify(opacity),
      ColorRGBa.fromHex("F2545B").opacify(opacity),
      ColorRGBa.fromHex("72DDF7").opacify(opacity),
      ColorRGBa.fromHex("AAF683").opacify(opacity),
      ColorRGBa.fromHex("E2ADF2").opacify(opacity),
      //  a couple dupes
      ColorRGBa.fromHex("E5B769").opacify(opacity),
      ColorRGBa.fromHex("AAF683").opacify(opacity),
      ColorRGBa.fromHex("E2ADF2").opacify(opacity)
    )

    val paintSplotches: List<PaintSplotch> = colors.mapIndexed { index, color ->
      val angle = map(0.0, colors.size.toDouble(), 0.0, 2.0 * PI, index.toDouble())
      val angleFrac = 2.0 * PI / colors.size.toDouble()
      val radius = 700.0
      val kite = contour {
        moveTo(
          cos(angle + angleFrac / 2.0) * radius / 2.0 + center.x,
          sin(angle + angleFrac / 2.0) * radius / 2.0 + center.y
        )
        lineTo(
          cos(angle) * radius + center.x,
          sin(angle) * radius + center.y
        )
        lineTo(
          cos(angle - angleFrac / 2.0) * radius / 2.0 + center.x,
          sin(angle - angleFrac / 2.0) * radius / 2.0 + center.y
        )
        lineTo(
          cos(angle - PI) * radius / 10.0 + center.x,
          sin(angle - PI) * radius / 10.0 + center.y
        )
        close()
      }
      val base = FractalizedPolygon(kite, rand)
      base.fractalize(4)

      val splotches = List(100) {
        base.clone().fractalize(7).shape
      }
      PaintSplotch(color, splotches)
    }

    val paramRange = 2.5
    val bounds = 2.0
    val nLines = 100
    var points = List(nLines) {
      Vector2(random(-bounds, bounds, rand), random(-bounds, bounds, rand))
    }
    val params = mapOf(
      "a" to random(-paramRange, paramRange, rand),
      "b" to random(-paramRange, paramRange, rand),
      "c" to random(-paramRange, paramRange, rand),
      "d" to random(-paramRange, paramRange, rand)
    )
    val deJong = DeJongAttractor(points, params)

    /*
    Create bloom filter requisites
     */
    // -- create offscreen render target
    val offscreen = renderTarget(width, height) {
      colorBuffer()
      depthBuffer()
    }
    // -- create bloom filter
    val bloom = Bloom()

    // -- create colorbuffer to hold bloom results
    val bloomed = colorBuffer(width, height)

    val useFilterForShapes = true

    extend {
      if (useFilterForShapes) {
        // -- draw to offscreen buffer
        drawer.isolatedWithTarget(offscreen) {
          clear(ColorRGBa.BLACK)
          // drawer.stroke = null
          paintSplotches.forEach {
            drawer.fill = it.color
            // drawer.stroke = it.color.opacify(100.0)
            // drawer.shadeStyle = radialGradient(it.color.opacify(0.5), it.color)
            it.splotches.forEach { drawer.contour(it) }
          }
        }
        // -- set bloom parameters
        bloom.brightness = 02.5
        bloom.padding = 0 // when padding > 0, it creates a fucked up blue border around the image... bug???
        bloom.downsamples = 6
        bloom.downsampleRate = 10

        // -- bloom offscreen's color buffer into bloomed
        bloom.apply(offscreen.colorBuffer(0), bloomed)
        drawer.image(bloomed)
      } else {
        drawer.stroke = null
        paintSplotches.forEach {
          drawer.fill = it.color
          // drawer.stroke = it.color.opacify(100.0)
          // drawer.shadeStyle = radialGradient(it.color.opacify(0.5), it.color)
          it.splotches.forEach { drawer.contour(it) }
        }
      }

      List(1000) { deJong.addNext() }
      drawer.fill = ColorRGBa.BLACK.opacify(0.35)
      deJong.points.map {
        // DeJong always returns between -2 and 2 since it is the difference of two basic trig funcs
        Vector2(
          map(-bounds, bounds, 0.0, width.toDouble(), it.x),
          map(-bounds, bounds, 0.0, height.toDouble(), it.y)
        )
      }.chunked(500) { drawer.points(it) }
    }
  }
}

package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.draw.isolated
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.color.palettes.ColorSequence
import org.openrndr.extra.fx.edges.Contour
import org.openrndr.extra.noise.fbm
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.*
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.translate
import org.openrndr.shape.*
import org.openrndr.svg.saveToFile
import util.grid
import util.rotatePoint
import util.timestamp
import java.io.File
import kotlin.math.*
import kotlin.random.Random

fun main() = application {
  println("org.openrndr.thing = ${System.getProperty("org.openrndr.application")}")
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
      contentScale = 3.0
      captureEveryFrame = false
      name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
    }

    val bg = ColorRGBa.WHITE
    backgroundColor = bg

    extend {
      val rng = Random(seed)
      drawer.stroke = ColorRGBa.BLACK
      drawer.strokeWeight = 1.0
      val center = Vector2(width / 2.0, height / 2.0)

      val svg = drawComposition {
        fill = null
        clipMode = ClipMode.REVERSE_DIFFERENCE
        val maxRadius = hypot(width * 0.1, height * 0.1)
        val minRadius = hypot(width * 0.01, height * 0.01)
        val offsetAngle = PI * 0.76
        for (i in 1..20) {
          circle(
            center -
              Vector2(sin(offsetAngle), cos(offsetAngle)) *
              map(ln(1.0), ln(20.0), maxRadius, minRadius, ln(i.toDouble())),
            map(ln(1.0), ln(20.0), minRadius, maxRadius, ln(i.toDouble()))
          )
        }

        val start = center -
          Vector2(sin(offsetAngle), cos(offsetAngle)) *
          (maxRadius + minRadius)
        for (i in 1..20) {
          val angle = map(1.0, 20.0, offsetAngle + PI * 0.5, offsetAngle + PI * 1.5, i.toDouble())
          val end = center - Vector2(sin(angle), cos(angle)) * maxRadius
          lineSegment(start, end)
        }
      }

      drawer.composition(svg)

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
        // save design for plotting
        val svgFile = File("screenshots/$progName/${timestamp()}-seed-$seed.svg")
        svgFile.parentFile?.let { file ->
          if (!file.exists()) {
            file.mkdirs()
          }
        }
        svg.saveToFile(svgFile)
      }
    }
  }
}

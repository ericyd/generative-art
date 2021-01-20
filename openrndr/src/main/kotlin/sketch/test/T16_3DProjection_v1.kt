/**
 * https://genuary2021.github.io/prompts
 * Jan.1
 * // TRIPLE NESTED LOOP
 *
 * References:
 * https://en.wikipedia.org/wiki/Spherical_coordinate_system
 */
package sketch.test

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.TransformTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extras.color.presets.ANTIQUE_WHITE
import org.openrndr.extras.color.presets.BLUE_STEEL
import org.openrndr.extras.color.presets.FOREST_GREEN
import org.openrndr.extras.color.presets.ORANGE_RED
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.map
import org.openrndr.math.transforms.lookAt
import org.openrndr.math.transforms.project
import org.openrndr.math.transforms.scale
import org.openrndr.shape.Segment
import util.timestamp
import java.io.File
import java.lang.Math.toRadians
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
  configure {
    width = 1000
    height = 1000
  }

  program {
    val scale = 3.0
    val w = width * scale
    val h = height * scale
    val center = Vector2(w / 2.0, h / 2.0)
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }

    // This is out hi-res render target which we draw to, before scaling it for the screen "preview"
    val rt = renderTarget(w.toInt(), h.toInt(), multisample = BufferMultisample.Disabled) { // multisample requires some weird copying to another color buffer
      colorBuffer()
      depthBuffer()
    }

    fun project3D(vec: Vector3): Vector3 {
      val zoom = 1.0
      // Camera is basically at the top corner of the "unit cube" defined by the bounds of the image.
      // assuming, of course, that the z-axis is as tall as the average of width and height
      val camera = Vector3(
        w * zoom,
        h * zoom,
        (w + h) * 0.5 * zoom
      )
      val projection = lookAt(camera, Vector3.ZERO, up = Vector3.UNIT_Z)
      return project(vec, projection, Matrix44.scale(Vector3(1.0 / camera.x, 1.0 / camera.y, 1.0 / camera.z)), width, height)

      // This works perfectly good! It's just a different camera angle.
      // Basically, it is looking straight down along the z-axis onto the x/y plane.
      // The bounds of the projection are pretty obvious but basically x range is [left,right], and y range is [bottom,top]
      // val projection = ortho(left=w*-0.5, right=w*0.5, bottom=h*-0.5, top=h*0.5, zNear=0.0, zFar=(w + h) * 0.5)
      // return project(vec, projection, Matrix44.IDENTITY, width, height)
    }

    extend {
      val segments = mutableListOf<List<Segment>>()
      var tempSegments = mutableListOf<Segment>()
      val thetaStartDegrees = -180

      for (percent in 1..100) {
        val radius = map(1.0, 100.0, 1000.0, 2000.0, percent.toDouble())
        val phi = map(1.0, 100.0, 0.0, 1.0 * PI, percent.toDouble())
        var cursor = Vector3(
          radius * sin(toRadians(thetaStartDegrees.toDouble())) * cos(phi),
          radius * sin(toRadians(thetaStartDegrees.toDouble())) * sin(phi),
          radius * cos(toRadians(thetaStartDegrees.toDouble()))
        )
        for (thetaDegrees in thetaStartDegrees..(thetaStartDegrees * -1) step 5) {
          val theta = toRadians(thetaDegrees.toDouble())

          val end = Vector3(
            radius * sin(theta) * cos(phi),
            radius * sin(theta) * sin(phi),
            radius * cos(theta)
          )
          val segment = Segment(project3D(cursor).xy, project3D(end).xy)
          tempSegments.add(segment)
          cursor = end
        }
        segments.add(tempSegments)
        tempSegments = mutableListOf()
      }

      // Render to the render target, save file, then scale and draw to screen
      drawer.isolatedWithTarget(rt) {
        // this is good for 2D but bad for 3D ... I guess?
        // drawer.ortho(rt)
        drawer.clear(ColorRGBa.ANTIQUE_WHITE)
        drawer.strokeWeight = 1.0
        drawer.stroke = ColorRGBa.BLACK
        drawer.fill = ColorRGBa.BLACK
        segments.forEach { drawer.segments(it) }

        // Just some good ol' axes for tracking where we are in space
        drawer.strokeWeight = 6.0
        drawer.stroke = ColorRGBa.FOREST_GREEN
        drawer.segment(Segment(project3D(Vector3.ZERO).xy, project3D(Vector3.UNIT_X * 3000.0).xy))

        drawer.stroke = ColorRGBa.ORANGE_RED
        drawer.segment(Segment(project3D(Vector3.ZERO).xy, project3D(Vector3.UNIT_Y * 3000.0).xy))

        drawer.stroke = ColorRGBa.BLUE_STEEL
        drawer.segment(Segment(project3D(Vector3.ZERO).xy, project3D(Vector3.UNIT_Z * 3000.0).xy))
      }

      drawer.scale(width.toDouble() / rt.width, TransformTarget.MODEL)
      drawer.image(rt.colorBuffer(0))

      // Change to `true` to capture screenshot
      if (false) {
        val targetFile = File("screenshots/$progName/${timestamp()}.png")
        targetFile.parentFile?.let { file ->
          if (!file.exists()) {
            file.mkdirs()
          }
        }
        rt.colorBuffer(0).saveToFile(targetFile, async = false)
      }
    }
  }
}

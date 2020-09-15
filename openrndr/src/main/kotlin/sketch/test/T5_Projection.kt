// Explorations in projecting 3D points to 2D
package sketch.test

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.lookAt
import org.openrndr.math.transforms.project
import org.openrndr.math.transforms.scale
import org.openrndr.shape.Segment
import org.openrndr.shape.Segment3D
import kotlin.math.hypot

fun main() = application {
  configure {
    width = 750
    height = 750
  }

  program {
    backgroundColor = ColorRGBa.WHITE
    val center = Vector3(width / 2.0, height / 2.0, 0.0)

    // fun project2D(vec: Vector3, camera: Vector3): Vector2 {
    //   val F = vec.z - camera.z
    //   val xPrime = ((vec.x - camera.x) * (F / vec.z)) + camera.x
    //   val yPrime = ((vec.y - camera.y) * (F / vec.z)) + camera.y
    //   return Vector2(xPrime, yPrime)
    // }

    fun project2D(vec: Vector3, camera: Vector3): Vector2 {
      val projection = lookAt(camera, Vector3.ZERO)
      // val projection = lookAt(camera, center)
      // val projection = perspective(45.0, 1.5, 0.10, 100.0, 0.0, 0.0)
      // val projection = perspective(45.0, 1.5, 010.0)

      // val projection = lookAt(camera, Vector3.ZERO)
      // fun project(point: Vector3, projection: Matrix44, view: Matrix44, width: Int, height: Int): Vector3 {
      // var p1 = project(vec, projection, Matrix44.rotate(Vector3.ONE, PI / 2.0), width, height)

      // var p1 = project(vec, projection, Matrix44.rotate(camera, 45.0), width, height)
      // return project(p1, projection, Matrix44.scale(Vector3(0.01 / hypot(width.toDouble(), height.toDouble()))), width, height).xy

      return project(vec, projection, Matrix44.scale(Vector3(1 / hypot(width.toDouble(), height.toDouble()))), width, height).xy
      // return project(vec, projection, Matrix44.scale(Vector3(0.0001)), width, height).xy

      // p1 = project(p1, projection, Matrix44.rotate(Vector3.UNIT_Y, PI / 2.0), width, height)
      // TODO: why does this need to be scaled to work properly?
      // return project(p1, projection, Matrix44.scale(Vector3(0.0001)), width, height).xy

      // return project(vec, projection, projection, width, height).xy
    }

    val xAxis = Segment3D(Vector3(0.0, 0.0, 0.0), Vector3(width.toDouble(), 0.0, 0.0))
    val yAxis = Segment3D(Vector3(0.0, 0.0, 0.0), Vector3(0.0, height.toDouble(), 0.0))
    val zAxis = Segment3D(Vector3(0.0, 0.0, 0.0), Vector3(0.0, 0.0, height.toDouble()))

    // val camera = center + Vector3(0.0, 0.0, width / 2.0)
    val camera = Vector3(width / 2.0)
    // val offset = center.xy
    val offset = Vector2.ZERO
    val xAxisProjected = Segment(project2D(xAxis.start, camera) + offset, project2D(xAxis.end, camera) + offset)
    val yAxisProjected = Segment(project2D(yAxis.start, camera) + offset, project2D(yAxis.end, camera) + offset)
    val zAxisProjected = Segment(project2D(zAxis.start, camera) + offset, project2D(zAxis.end, camera) + offset)

    println(xAxisProjected)
    println(yAxisProjected)
    println(zAxisProjected)

    extend {
      drawer.fill = ColorRGBa.BLACK

      // Testing projection
      drawer.strokeWeight = 2.0

      // drawer.stroke = ColorRGBa.RED
      // drawer.segment(Segment(xy.start.xy, xy.end.xy))
      //
      // drawer.stroke = ColorRGBa.BLUE
      // drawer.segment(Segment(yz.start.xy, yz.end.xy))
      //
      // drawer.stroke = ColorRGBa.GREEN
      // drawer.segment(Segment(xz.start.xy, xz.end.xy))

      drawer.stroke = ColorRGBa.RED
      drawer.segment(xAxisProjected)

      drawer.stroke = ColorRGBa.BLUE
      drawer.segment(yAxisProjected)

      drawer.stroke = ColorRGBa.GREEN
      drawer.segment(zAxisProjected)

      drawer.circle(project2D(Vector3(100.0, 100.0, 0.0), camera), 10.0)
    }
  }
}

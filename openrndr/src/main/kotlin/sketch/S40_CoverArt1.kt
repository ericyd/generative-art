package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.hsla
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.TransformTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Segment
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import util.QTreeNode
import util.QuadTree
import util.QuadTreeNode
import util.grid
import util.timestamp
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 1000
  }

  program {
    val scaleAmount = 1
    fun scale(v: Double): Double { return v * scaleAmount.toDouble() }
    fun scale(v: Int): Int { return v * scaleAmount }
    val w = scale(width)
    val h = scale(height)
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    val rt = renderTarget(w, h, multisample = BufferMultisample.Disabled) {
      colorBuffer()
      depthBuffer()
    }

    class Walker(val start: Vector2, val baseAngle: Double, val rng: Random, val velocity: Double = 5.0) {
      fun walkNoOverlap(length: Int, padding: Double, existingPoints: QuadTree, bounds: Rectangle): List<Vector2> {
        var angle = genAngle(baseAngle)
        var cursor = start
        existingPoints.add(QTreeNode(cursor))
        // it is, of course, valid to include the start point in the line.
        // However, it seems to result in unintentional overlaps, and doesn't dramatically improve the look
        // val list = mutableListOf(cursor)
        val list = mutableListOf<Vector2>()
        for (i in 0 until length) {
          angle = genAngle(angle)
          val point = Vector2(
            cursor.x + cos(angle) * velocity,
            cursor.y + sin(angle) * velocity
          )

          val nearPointsOverlap = existingPoints
            .query<QuadTreeNode>(Rectangle.fromCenter(point, velocity * 2.0, velocity * 2.0))
            .any { it.position.distanceTo(point) < padding }
          if (nearPointsOverlap || !bounds.contains(point)) {
            continue
          }

          cursor = point
          existingPoints.add(QTreeNode(cursor))
          list.add(cursor)
        }
        return list
      }

      // generate a new angle based on the previous angle
      private fun genAngle(previousAngle: Double): Double {
        // encourage straight lines
        if (random(0.0, 1.0, rng) < 0.8) {
          return previousAngle
        }
        val chance = random(0.0, 1.0, rng)
        return if (chance < 1.0 / 6.0) { PI * 3.0 / 2.0 }
        else if (chance >= 1.0 / 6.0 && chance < 2.0 / 6.0) { PI / 2.0 }
        else if (chance >= 2.0 / 6.0 && chance < 3.0 / 6.0) { baseAngle }
        else if (chance >= 3.0 / 6.0 && chance < 4.0 / 6.0) { PI - baseAngle }
        else if (chance >= 4.0 / 6.0 && chance < 5.0 / 6.0) { PI + baseAngle }
        else { PI * 2.0 - baseAngle }
      }
    }

    data class ContourWithEdge(val contour: ShapeContour, val edge: Segment)

    /*
    what might look nicer, is:
    try to identify if the shape has any corners
    if 3, follow triangle subdivision rule (longest edge to opposite corner)
    if 4, split in half into triangles - opposite corners
    if > 4, follow existing logic
     */
    fun subdivide_v2(dividable: ContourWithEdge, maxDepth: Int, currentDepth: Int, baseFidelity: Int = 100, rng: Random = Random.Default): List<ContourWithEdge> {
      val (contour, edge) = dividable
      // this could use some tuning
      if (contour.length < 20.0) {
        return listOf(dividable)
      }

      val fidelity = map(0.0, maxDepth - 1.0, baseFidelity.toDouble(), baseFidelity * 0.5, currentDepth.toDouble()).toInt()
      // split the contour into points
      val points = contour.equidistantPositions(fidelity)
      // val contourMidpointIndex = points.size / 2 // BORING!
      // val edgeMidpoint = (edge.start + edge.end) / 2.0 // BORING!

      val randMin = map(0.0, maxDepth - 1.0, 0.3, 0.5, currentDepth.toDouble())

      // split the list of points in "half"
      val contourMidpointIndex = (fidelity * random(randMin, 1.0 - randMin, rng)).toInt()
      // split the edge segment in "half"
      val edgeSplitPosition = random(randMin, 1.0 - randMin, rng)
      val edgeMidpointA = edge.position(edgeSplitPosition)
      val edgeSplit = map(0.0, maxDepth - 1.0, 0.01, 0.05, currentDepth.toDouble())
      val edgeMidpointB = edge.position(edgeSplitPosition - edgeSplit)

      // join together the first half of the contour points with the edge piece
      val pointsListA = listOf(edgeMidpointA) + points.subList(0, contourMidpointIndex)
      val edgeA = Segment(points[contourMidpointIndex], edgeMidpointA, corner = true)
      val contourA = ShapeContour.fromPoints(pointsListA, closed = false)

      val pointsListB = points.subList(contourMidpointIndex, points.size) + listOf(edgeMidpointB)
      val edgeB = Segment(edgeMidpointB, points[contourMidpointIndex], corner = true)
      val contourB = ShapeContour.fromPoints(pointsListB, closed = false)

      return listOf(ContourWithEdge(contourA, edgeA), ContourWithEdge(contourB, edgeB))
    }

    fun splitInTwo(contour: ShapeContour, fidelity: Int = 100, rng: Random = Random.Default): List<ContourWithEdge> {
      // get list of evenly-spaced points around the contour
      val points = contour.equidistantPositions(fidelity)

      var startPointIndex = random(fidelity * 0.4, fidelity * 0.6, rng).toInt()
      var endPointIndex = startPointIndex + random(fidelity * 0.4, fidelity * 0.6, rng).toInt()

      // normalize start/end point
      while (startPointIndex > fidelity) {
        startPointIndex -= fidelity
      }
      while (endPointIndex > fidelity) {
        endPointIndex -= fidelity
      }
      val (start, end) = listOf(startPointIndex, endPointIndex).sorted()

      val pointsListA = points.subList(start, end)
      val edgeA = Segment(pointsListA.last(), pointsListA.first(), corner = true)
      val contourA = ShapeContour.fromPoints(pointsListA, closed = false)

      val pointsListB = points.subList(end, fidelity) + points.subList(0, start)
      // val edgeB = Segment(pointsListB.first(), pointsListB.last(), corner = true)
      val edgeB = Segment(pointsListB.last(), pointsListB.first(), corner = true)
      val contourB = ShapeContour.fromPoints(pointsListB, closed = false)

      return listOf(ContourWithEdge(contourA, edgeA), ContourWithEdge(contourB, edgeB))
    }

    fun subdivideUntil(dividable: ContourWithEdge, fidelity: Int = 100, rng: Random = Random.Default, maxDepth: Int = 5, currentDepth: Int = 0): List<ContourWithEdge> {
      if (currentDepth > maxDepth) {
        return listOf()
      }
      return subdivide_v2(dividable, maxDepth, currentDepth, fidelity, rng)
        .flatMap {
          if (currentDepth == maxDepth) {
            listOf(it)
          } else {
            subdivideUntil(it, fidelity, rng, maxDepth, currentDepth + 1)
          }
        }
    }

    extend {
      // get that rng
      val rng = Random(seed.toLong())

      val angle = PI / 9.0
      val bounds = Rectangle(0.0, 0.0, w.toDouble(), h.toDouble())
      val existingPoints = QuadTree(bounds, 10)
      val velocity = scale(random(6.0, 13.0, rng))
      val padding = velocity - scale(1.0)
      val jitter = { n: Double -> random(n - scale(1.0), n + scale(1.0), rng) }

      // Render to the render target, then scale and draw to screen
      drawer.isolatedWithTarget(rt) {
        drawer.ortho(rt)
        drawer.clear(ColorRGBa.BLACK)
        drawer.stroke = ColorRGBa.WHITE
        drawer.strokeWeight = scale(0.5)

        // Background "walker" pattern
        /*
        */
        grid(0, w, 0, h, velocity.toInt() + padding.toInt()) { i: Int, j: Int ->
          // start at center to avoid biasing towards a corner
          val x = if (i % 2 == 0) { w / 2.0 + i / 2.0 } else { w / 2.0 - i / 2.0 }
          val y = if (j % 2 == 0) { h / 2.0 + j / 2.0 } else { h / 2.0 - j / 2.0 }
          if (j == 0) println("$i of $w")
          val start = Vector2(jitter(x), jitter(y))
          val walker = Walker(start, angle, rng, velocity)
          // TODO: would it be interesting to have randomized length?
          val line = walker.walkNoOverlap(scale(random(500.0, 1000.0, rng).toInt()), padding, existingPoints, bounds)
          drawer.lineStrip(line)
        }

        /*
        */
        val baseContour = Circle(w/2.0, h/2.0, w/4.0).contour
        drawer.stroke = null
        drawer.fill = ColorRGBa.BLACK
        drawer.contour(baseContour)

        val fidelity = 200
        val maxDepth = 11
        val (edgedContourA, edgedContourB) = splitInTwo(baseContour, fidelity, rng)
        val contours = subdivideUntil(edgedContourA, fidelity, rng, maxDepth) +
          subdivideUntil(edgedContourB, fidelity, rng, maxDepth)
        drawer.strokeWeight = scale(0.5)
        for (c in contours.map { it.contour.close }) {
          // drawer.fill = ColorRGBa.WHITE.opacify(random(0.15, 0.85, rng))
          drawer.fill = hsla(
            random(20.0, 60.0, rng),
            random(0.3, 0.4, rng),
            random(0.4, 0.6, rng),
            random(0.3, 0.8, rng),
          ).toRGBa()
          drawer.contour(c)
        }
        // drawer.contours(contours.map { it.contour.close })

        /*debugging
        drawer.fill = ColorRGBa.ORANGE_RED.opacify(0.15)
        val debugContour = contours[random(0.0, contours.size - 1.0, rng).toInt()]
        drawer.stroke = ColorRGBa.GREEN
        drawer.segments(debugContour.segments.filterIndexed { index, segment -> index % 2 == 0  })
        drawer.stroke = ColorRGBa.RED
        drawer.segments(debugContour.segments.filterIndexed { index, segment -> index % 2 != 0  })
        */
      }

      drawer.scale(width.toDouble() / rt.width, TransformTarget.MODEL)
      drawer.image(rt.colorBuffer(0))

      // Change to `true` to capture screenshot
      if (true) {
        val targetFile = File("screenshots/$progName/${timestamp()}-seed-$seed.jpg")
        targetFile.parentFile?.let { file ->
          if (!file.exists()) {
            file.mkdirs()
          }
        }
        rt.colorBuffer(0).saveToFile(targetFile, async = false)
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        println("seed = $seed")
      }
    }
  }
}

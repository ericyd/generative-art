package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.hsv
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.Drawer
import org.openrndr.draw.TransformTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.noise.random
import org.openrndr.extras.color.palettes.ColorSequence
import org.openrndr.extras.color.palettes.colorSequence
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Segment
import org.openrndr.shape.ShapeContour
import util.QTreeNode
import util.QuadTree
import util.QuadTreeNode
import util.grid
import util.timestamp
import java.io.File
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
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
    val center = Vector2(w / 2.0, h / 2.0)
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    val rt = renderTarget(w, h, multisample = BufferMultisample.Disabled) {
      colorBuffer()
      depthBuffer()
    }

    class Walker(val start: Vector2, val baseAngle: Double, val rng: Random, val velocity: Double = 5.0) {
      fun walkNoOverlap(length: Int, padding: Double, existingPoints: QuadTree, bounds: Rectangle, drawer: Drawer, gradient: ColorSequence): List<Vector2> {
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

          if (i > 1) {
            drawer.stroke = gradient.index(point.distanceTo(center) / (hypot(w * 0.5, h * 0.5)))
            drawer.lineSegment(cursor, point)
          }
          cursor = point
          existingPoints.add(QTreeNode(cursor))
          // and now that we're drawing the line directly, we don't even need the list, but its easier to keep the boilerplate
          // list.add(cursor)
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

    data class ContourWithEdges(val contour: ShapeContour, val edges: List<Segment>)

    /**
     * try this
     *  1. draw first edge through circle
     *  2. Estimate a "triangle" based on
     *    a. the edge (points 1 and 2)
     *    b. the "midpoint" (randomized) of the remaining contour (point 3)
     *  3. Proceed with "triangle subdivision"
     *    - not exactly sure how this will look for such a large thing
     *  4. Repeat for next segment
     */
    fun subdivide_v3(dividable: ContourWithEdges, maxDepth: Int, currentDepth: Int, baseFidelity: Int = 100, rng: Random = Random.Default): List<ContourWithEdges> {
      val (contour, edges) = dividable

      when (edges.size) {
        1 -> {
          // create "pseudo triangle" by combining the edge with the midpoint of the contour
          val edgeA = edges[0]
          // val edgeAEndCloseToContourStart = edgeA.end.distanceTo(contour.position(0.0)) < 10.0
          val contourMidpointT = random(0.4, 0.6, rng)
          val edgeB = Segment(edgeA.end, contour.position(contourMidpointT))
          val edgeC = Segment(contour.position(contourMidpointT), edgeA.start)

          val edgeMidpointTBase = random(0.4, 0.6, rng)
          if (edgeA.length > edgeB.length && edgeA.length > edgeC.length) { // edgeA is longest
            val edgeMidpointT = edgeMidpointTBase
            val divider = Segment(edgeA.position(edgeMidpointT), edgeB.end)
            val contourA = contour.sub(0.0, contourMidpointT)
            val contourB = contour.sub(contourMidpointT, 1.0)
            return listOf(ContourWithEdges(contourA, listOf(edgeA.sub(edgeMidpointT, 1.0), divider)), ContourWithEdges(contourB, listOf(divider, edgeB.sub(edgeMidpointT, 1.0))))
          } else if (edgeB.length > edgeA.length && edgeB.length > edgeC.length) { // edgeB is longest
            val edgeMidpointT = edgeMidpointTBase * contourMidpointT
            val divider = Segment(contour.position(edgeMidpointT), edgeA.start)
            val contourA = contour.sub(0.0, edgeMidpointT)
            val contourB = contour.sub(edgeMidpointT, 1.0)
            return listOf(ContourWithEdges(contourA, listOf(divider, edgeA)), ContourWithEdges(contourB, listOf(divider)))
          } else { // edgeC is longest (or math error)
            val edgeMidpointT = 1.0 - (edgeMidpointTBase * contourMidpointT)
            val divider = Segment(contour.position(edgeMidpointT), edgeA.end)
            val contourA = contour.sub(0.0, edgeMidpointT)
            val contourB = contour.sub(edgeMidpointT, 1.0)
            return listOf(ContourWithEdges(contourA, listOf(divider)), ContourWithEdges(contourB, listOf(divider, edgeA)))
          }
        }
        2 -> {
          // "pseudo triangle" has to be the contour combined with 2 edges (B and C) which meet at vertexBC
          val edgeA = contour
          val edgeAStart = contour.position(0.0)
          val edgeAEnd = contour.position(1.0)
          val pointsBC = listOf(edges[0].start, edges[0].end, edges[1].start, edges[1].end)
          val vertexBC = pointsBC.first { it.distanceTo(edgeAStart) > 1.0 && it.distanceTo(edgeAEnd) > 1.0 }
          val edgeB = Segment(edgeAEnd, vertexBC)
          val edgeC = Segment(vertexBC, edgeAStart)

          val edgeMidpointT = random(0.4, 0.6, rng)
          if (edgeA.length > edgeB.length && edgeA.length > edgeC.length) { // edgeA (contour) is longest
            val divider = Segment(contour.position(edgeMidpointT), vertexBC)
            val contourA = contour.sub(0.0, edgeMidpointT)
            val contourB = contour.sub(edgeMidpointT, 1.0)
            return listOf(ContourWithEdges(contourA, listOf(edgeC, divider)), ContourWithEdges(contourB, listOf(divider, edgeB)))
          } else if (edgeB.length > edgeA.length && edgeB.length > edgeC.length) { // edgeB is longest
            val divider = Segment(edgeB.position(edgeMidpointT), edgeAStart)
            val contourA = ShapeContour.EMPTY
            val contourB = contour
            return listOf(ContourWithEdges(contourA, listOf(edgeB.sub(edgeMidpointT, 1.0), edgeC, divider)), ContourWithEdges(contourB, listOf(edgeB.sub(0.0, edgeMidpointT), divider)))
          } else { // edgeC is longest (or math error)
            val divider = Segment(edgeC.position(edgeMidpointT), edgeAEnd)
            val contourA = ShapeContour.EMPTY
            val contourB = contour
            return listOf(ContourWithEdges(contourA, listOf(edgeC.sub(0.0, edgeMidpointT), divider, edgeB)), ContourWithEdges(contourB, listOf(divider, edgeC.sub(edgeMidpointT, 1.0))))
          }
        }
        // this *should* only get here with 3 edges (we treat it as always having 3 edges, so if there are more for some reason they will just be dropped)
        else -> {
          // triangle is made up one one known edge (edgeA) and the vertex of the other two edges
          // we don't know exactly where that vertex is b/c edgeB or edgeC could be flipped,
          // so we have to find the point that is furthest from both edges
          val edgeA = edges[0]
          val pointsBC = listOf(edges[1].start, edges[1].end, edges[2].start, edges[2].end)
          val vertexBC = pointsBC.first { it.distanceTo(edgeA.start) > 1.0 && it.distanceTo(edgeA.end) > 1.0 }
          val edgeB = Segment(edgeA.end, vertexBC)
          val edgeC = Segment(vertexBC, edgeA.start)

          val edgeMidpointT = random(0.4, 0.6, rng)
          if (edgeA.length > edgeB.length && edgeA.length > edgeC.length) { // edgeA is longest
            val divider = Segment(edgeA.position(edgeMidpointT), edgeB.end)
            return listOf(ContourWithEdges(ShapeContour.EMPTY, listOf(edgeA.sub(edgeMidpointT, 1.0), edgeB, divider)), ContourWithEdges(ShapeContour.EMPTY, listOf(divider, edgeC, edgeA.sub(0.0, edgeMidpointT))))
          } else if (edgeB.length > edgeA.length && edgeB.length > edgeC.length) { // edgeB is longest
            val divider = Segment(edgeB.position(edgeMidpointT), edgeA.start)
            return listOf(ContourWithEdges(ShapeContour.EMPTY, listOf(edgeB.sub(edgeMidpointT, 1.0), edgeC, divider)), ContourWithEdges(ShapeContour.EMPTY, listOf(divider, edgeA, edgeB.sub(0.0, edgeMidpointT))))
          } else { // edgeC is longest (or math error)
            val divider = Segment(edgeC.position(edgeMidpointT), edgeB.start)
            return listOf(ContourWithEdges(ShapeContour.EMPTY, listOf(edgeC.sub(edgeMidpointT, 1.0), edgeA, divider)), ContourWithEdges(ShapeContour.EMPTY, listOf(divider, edgeB, edgeC.sub(0.0, edgeMidpointT))))
          }
        }
      }
    }

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
      val edgeGap = map(0.0, maxDepth - 1.0, 0.01, 0.05, currentDepth.toDouble())
      val edgeMidpointB = edge.position(edgeSplitPosition - edgeGap)

      // join together the first half of the contour points with the edge piece
      val pointsListA = listOf(edgeMidpointA) + points.subList(0, contourMidpointIndex)
      val edgeA = Segment(points[contourMidpointIndex], edgeMidpointA, corner = true)
      val contourA = ShapeContour.fromPoints(pointsListA, closed = false)

      val pointsListB = points.subList(contourMidpointIndex, points.size) + listOf(edgeMidpointB)
      val edgeB = Segment(edgeMidpointB, points[contourMidpointIndex], corner = true)
      val contourB = ShapeContour.fromPoints(pointsListB, closed = false)

      return listOf(ContourWithEdge(contourA, edgeA), ContourWithEdge(contourB, edgeB))
    }

    fun splitInTwo(contour: ShapeContour, fidelity: Int = 100, rng: Random = Random.Default): List<ContourWithEdges> {
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

      return listOf(ContourWithEdges(contourA, listOf(edgeA)), ContourWithEdges(contourB, listOf(edgeB)))
    }

    fun explode(contour: ShapeContour, rng: Random = Random.Default): ShapeContour {
      val contourCenter = contour.bounds.center
      // wow this improves things SOOOOOOO much. What i need is an exponential interp or something. Maybe squaredDistanceTo
      if (contourCenter.distanceTo(center) < w * 0.1) {
        return contour
      }
      val explodeBasis = map(w * 0.1, w * 0.3, 0.0, w * 0.1, contourCenter.distanceTo(center))
      val explodeStrength = random(-explodeBasis, explodeBasis, rng)
      val explodeDirection = atan2(contourCenter.y - center.y, contourCenter.x - center.x) + random(-PI * 0.1, PI * 0.1, rng)
      val explodeVec = Vector2(cos(explodeDirection), sin(explodeDirection)) * explodeStrength
      val segments = contour.segments.map { Segment(it.start + explodeVec, it.end + explodeVec) }
      return ShapeContour.fromSegments(segments, closed = true)
    }

    fun subdivideUntil(dividable: ContourWithEdges, fidelity: Int = 100, rng: Random = Random.Default, maxDepth: Int = 5, currentDepth: Int = 0): List<ContourWithEdges> {
      if (currentDepth > maxDepth) {
        return listOf()
      }
      return subdivide_v3(dividable, maxDepth, currentDepth, fidelity, rng)
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

      // TODO: is this cooler with different colors... or just black and white?
      val gradients = listOf(
        colorSequence(
          0.0 to ColorRGBa.WHITE,
          1.0 to ColorRGBa.BLACK,
        ),
        colorSequence(
          0.0 to hsv(267.0, 0.18, 0.94),
          1.0 to hsv(225.0, 0.68, 0.44)
        ),
        colorSequence(
          0.0 to hsv(49.0, 0.35, 0.96),
          1.0 to hsv(32.0, 1.0, 0.48)
        ),
        colorSequence(
          0.0 to hsv(random(215.0, 230.0, rng), random(0.5, 0.7, rng), random(0.6, 0.95, rng)),
          1.0 to hsv(random(215.0, 230.0, rng), random(0.2, 0.4, rng), random(0.15, 0.3, rng))
        ),
        colorSequence(
          0.0 to hsv(random(20.0, 35.0, rng), random(0.7, 0.95, rng), random(0.6, 0.95, rng)),
          1.0 to hsv(random(20.0, 35.0, rng), random(0.3, 0.65, rng), random(0.25, 0.45, rng))
        )
      )

      // Render to the render target, then scale and draw to screen
      drawer.isolatedWithTarget(rt) {
        drawer.ortho(rt)
        drawer.clear(ColorRGBa.BLACK)
        drawer.stroke = ColorRGBa.WHITE
        drawer.strokeWeight = scale(0.5)

        // Background "walker" pattern
        val angle = PI / 9.0
        val bounds = Rectangle(0.0, 0.0, w.toDouble(), h.toDouble())
        val existingPoints = QuadTree(bounds, 10)
        // lower is better around 6.0 -- for final "cut", should reduce this
        val velocity = scale(random(6.0, 7.0, rng))
        val padding = velocity - scale(1.0)
        val jitter = { n: Double -> random(n - scale(1.0), n + scale(1.0), rng) }
        /*
        */
        grid(0, w, 0, h, velocity.toInt() + padding.toInt()) { i: Int, j: Int ->
          // start at center to avoid biasing towards a corner
          val x = if (i % 2 == 0) { w / 2.0 + i / 2.0 } else { w / 2.0 - i / 2.0 }
          val y = if (j % 2 == 0) { h / 2.0 + j / 2.0 } else { h / 2.0 - j / 2.0 }
          if (j == 0) println("$i of $w")
          val start = Vector2(jitter(x), jitter(y))
          val gradient = gradients[random(0.0, gradients.size.toDouble(), rng).toInt()]
          val walker = Walker(start, angle, rng, velocity)
          walker.walkNoOverlap(scale(random(500.0, 1000.0, rng).toInt()), padding, existingPoints, bounds, drawer, gradient)
        }

        /*
        */
        val baseContour = Circle(w / 2.0, h / 2.0, w * 0.18).contour
        drawer.stroke = null
        drawer.fill = ColorRGBa.BLACK
        drawer.contour(baseContour)

        val fidelity = 200
        val maxDepth = 9
        val (edgedContourA, edgedContourB) = splitInTwo(baseContour, fidelity, rng)
        val contours = subdivideUntil(edgedContourA, fidelity, rng, maxDepth) +
          subdivideUntil(edgedContourB, fidelity, rng, maxDepth)
        val mappedContours = contours
          // .map { it.contour.close }
          .map {
            when (it.edges.size) {
              1 -> {
                val startPoint = it.edges[0].start
                val endPoint = it.edges[0].end
                val vertexBC = it.contour.position(0.5)
                ShapeContour.fromPoints(listOf(startPoint, endPoint, vertexBC), closed = true)
              }
              2 -> {
                val startPoint = it.contour.position(0.0)
                val endPoint = it.contour.position(1.0)
                val pointsBC = listOf(it.edges[0].start, it.edges[0].end, it.edges[1].start, it.edges[1].end)
                val vertexBC = pointsBC.first { it.distanceTo(startPoint) > 1.0 && it.distanceTo(endPoint) > 1.0 }
                ShapeContour.fromPoints(listOf(startPoint, endPoint, vertexBC), closed = true)
              }
              else -> {
                val edgeA = it.edges[0]
                val pointsBC = listOf(it.edges[1].start, it.edges[1].end, it.edges[2].start, it.edges[2].end)
                val vertexBC = pointsBC.first { it.distanceTo(edgeA.start) > 1.0 && it.distanceTo(edgeA.end) > 1.0 }
                ShapeContour.fromPoints(listOf(edgeA.start, edgeA.end, vertexBC), closed = true)
              }
            }
          }
          .map { explode(it, rng) }
        drawer.strokeWeight = scale(0.5)
        drawer.shadeStyle = null
        for (c in mappedContours) {
          // drawer.fill = ColorRGBa.WHITE.opacify(random(0.15, 0.85, rng))

          // drawer.fill = hsla(
          //   random(20.0, 60.0, rng),
          //   random(0.3, 0.4, rng),
          //   random(0.4, 0.6, rng),
          //   random(0.3, 0.8, rng),
          // ).toRGBa()

          val baseColor = gradients[random(0.0, gradients.size - 1.0, rng).toInt()]
            .index(random(0.1, 0.3, rng))
          drawer.fill = baseColor.opacify(random(0.3, 0.8, rng))
          drawer.stroke = baseColor
          // drawer.segments(c.edges)
          // drawer.contour(c.contour)
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

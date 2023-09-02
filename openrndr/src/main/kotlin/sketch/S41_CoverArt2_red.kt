package sketch

import org.openrndr.application
import org.openrndr.color.ColorHSVa
import org.openrndr.color.ColorRGBa
import org.openrndr.color.hsv
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.DepthFormat
import org.openrndr.draw.Drawer
import org.openrndr.draw.TransformTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.noise.random
import org.openrndr.extra.shadestyles.RadialGradient
import org.openrndr.extra.color.palettes.ColorSequence
import org.openrndr.extra.color.palettes.colorSequence
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
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
  configure {
    width = 1000
    height = 1000
  }

  program {
    val scaleAmount = 3
    fun scale(v: Double): Double { return v * scaleAmount.toDouble() }
    fun scale(v: Int): Int { return v * scaleAmount }
    val w = scale(width)
    val h = scale(height)
    val center = Vector2(w / 2.0, h / 2.0)
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    data class Options(
      val shatterCircleRadius: Double,
      val maxDepth: Int
    )

    // seed: 1043584980
    val options = Options(
      shatterCircleRadius = w * 0.35,
      maxDepth = 10
    )

    val rt = renderTarget(w, h) {
      colorBuffer()
      depthBuffer()
    }

    class ContourWithVertices(val contour: ShapeContour = ShapeContour.EMPTY, val vertices: List<Vector2> = listOf()) {
      fun toClosedShapeContour(): ShapeContour {
        val points = when (this.vertices.size) {
          0 -> {
            val vertexA = this.contour.position(0.0)
            val vertexB = this.contour.position(1.0)
            val vertexC = this.contour.position(0.5)
            listOf(vertexA, vertexB, vertexC)
          }
          1 -> {
            val vertexA = this.contour.position(0.0)
            val vertexB = this.contour.position(1.0)
            val vertexC = this.vertices[0]
            listOf(vertexA, vertexB, vertexC)
          }
          else -> {
            val vertexA = this.vertices[0]
            val vertexB = this.vertices[1]
            val vertexC = this.vertices[2]
            listOf(vertexA, vertexB, vertexC)
          }
        }
        return ShapeContour.fromPoints(points, closed = true)
      }

      /**
       *  Triangle subdivision of arbitrary ShapeContours.
       *  Assumes that the ShapeContour is not closed.
       *  The algorithm could run with a closed ShapeContour but it might not produce good results.
       *
       *  Three scenarios, each with discrete algorithm:
       *  A. ShapeContour only, no known vertices
       *    1. Consider start of ShapeContour as vertexA
       *    2. Consider end of ShapeContour as vertexB
       *    3. Take a point in the middle of the ShapeContour (with some randomization) as vertexC
       *    4. Split the ShapeContour based on which edge is the longest
       *    5. Return the two ShapeContours matched with the midpoint from step 3. On subsequent subdivisions, this data will fall into scenario B (below)
       *  B. ShapeContour with 1 known vertex
       *    1. Consider start of ShapeContour as vertexA
       *    2. Consider end of ShapeContour as vertexB
       *    3. The known vertex is vertexC
       *    4. If the contour is the longest section
       *      a. get the midpoint (with some randomization)
       *      b. split the ShapeContour at the quasi-midpoint
       *      c. return each ShapeContour split with vertexC. These will both fall into scenario B again on subsequent subdivisions
       *    5. If either edge BC or edge CA is longest
       *      a. get the midpoint (with some randomization)
       *      b. Return one triangle (3 known vertices) from the new midpoint and 2 other vertices
       *      c. Return the full ShapeContour along with the midpoint as the known vertex
       *  C. No ShapeContour, but 3 known vertices
       *    1. Find the longest edge
       *    2. Split the longest edge (with some randomization)
       *    3. Return 2 new triangles using the new vertex
       */
      fun subdivide(rng: Random = Random.Default): List<ContourWithVertices> {
        val contour = this.contour
        val vertices = this.vertices

        when (vertices.size) {
          0 -> {
            val vertexA = contour.position(0.0)
            val vertexB = contour.position(1.0)
            val contourMidpointT = random(0.4, 0.6, rng)
            val vertexC = contour.position(contourMidpointT)

            val lengthAB = vertexA.distanceTo(vertexB)
            val lengthBC = vertexB.distanceTo(vertexC)
            val lengthCA = vertexC.distanceTo(vertexA)

            val edgeMidpointT = random(0.4, 0.6, rng)
            val newVertex = vertexA.mix(vertexB, edgeMidpointT)
            return if (lengthAB > lengthBC && lengthAB > lengthCA) {
              val contourA = contour.sub(0.0, contourMidpointT)
              val contourB = contour.sub(contourMidpointT, 1.0)
              listOf(ContourWithVertices(contourA, listOf(newVertex)), ContourWithVertices(contourB, listOf(newVertex)))
            } else if (lengthBC > lengthAB && lengthBC > lengthCA) {
              // midpoint is between *start* of contour and "contourMidpoint" aka vertexC
              val contourSplitpointT = edgeMidpointT * contourMidpointT
              val contourA = contour.sub(0.0, contourSplitpointT)
              val contourB = contour.sub(contourSplitpointT, 1.0)
              listOf(ContourWithVertices(contourA, listOf(newVertex)), ContourWithVertices(contourB, listOf(newVertex)))
            } else {
              // midpoint is between *end* of contour and "contourMidpoint" aka vertexC
              val contourSplitpointT = 1.0 - (edgeMidpointT * contourMidpointT)
              val contourA = contour.sub(0.0, contourSplitpointT)
              val contourB = contour.sub(contourSplitpointT, 1.0)
              listOf(ContourWithVertices(contourA, listOf(newVertex)), ContourWithVertices(contourB, listOf(newVertex)))
            }
          }
          1 -> {
            val vertexA = contour.position(0.0)
            val vertexB = contour.position(1.0)
            val vertexC = vertices[0]

            val lengthAB = vertexA.distanceTo(vertexB)
            val lengthBC = vertexB.distanceTo(vertexC)
            val lengthCA = vertexC.distanceTo(vertexA)

            val edgeMidpointT = random(0.4, 0.6, rng)
            return if (lengthAB > lengthBC && lengthAB > lengthCA) { // contour is longest
              val contourA = contour.sub(0.0, edgeMidpointT)
              val contourB = contour.sub(edgeMidpointT, 1.0)
              listOf(ContourWithVertices(contourA, listOf(vertexC)), ContourWithVertices(contourB, listOf(vertexC)))
            } else if (lengthBC > lengthAB && lengthBC > lengthCA) {
              val newVertex = vertexB.mix(vertexC, edgeMidpointT)
              listOf(ContourWithVertices(vertices = listOf(newVertex, vertexC, vertexA)), ContourWithVertices(contour, listOf(newVertex)))
            } else {
              val newVertex = vertexC.mix(vertexA, edgeMidpointT)
              listOf(ContourWithVertices(vertices = listOf(newVertex, vertexB, vertexC)), ContourWithVertices(contour, listOf(newVertex)))
            }
          }
          3 -> {
            val vertexA = vertices[0]
            val vertexB = vertices[1]
            val vertexC = vertices[2]

            val lengthAB = vertexA.distanceTo(vertexB)
            val lengthBC = vertexB.distanceTo(vertexC)
            val lengthCA = vertexC.distanceTo(vertexA)

            val edgeMidpointT = random(0.4, 0.6, rng)
            return if (lengthAB > lengthBC && lengthAB > lengthCA) { // AB is longest
              val newVertex = vertexA.mix(vertexB, edgeMidpointT)
              listOf(ContourWithVertices(vertices = listOf(newVertex, vertexA, vertexC)), ContourWithVertices(vertices = listOf(newVertex, vertexB, vertexC)))
            } else if (lengthBC > lengthAB && lengthBC > lengthCA) { // BC is longest
              val newVertex = vertexB.mix(vertexC, edgeMidpointT)
              listOf(ContourWithVertices(vertices = listOf(newVertex, vertexA, vertexB)), ContourWithVertices(vertices = listOf(newVertex, vertexA, vertexC)))
            } else { // CA is longest
              val newVertex = vertexC.mix(vertexA, edgeMidpointT)
              listOf(ContourWithVertices(vertices = listOf(newVertex, vertexB, vertexC)), ContourWithVertices(vertices = listOf(newVertex, vertexA, vertexB)))
            }
          }
          else -> {
            throw Error("Found ${vertices.size} vertices! Only 0, 1, or 3 allowed")
          }
        }
      }
    }

    fun splitInTwo(contour: ShapeContour, fidelity: Int = 100, rng: Random = Random.Default): List<ContourWithVertices> {
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

      val gapSize = 0

      // add 1 to "close" the gap (the range is half-open, i.e. end-exclusive), then subtract any gap
      val pointsListA = points.subList(start, end + 1 - gapSize)
      val contourA = ShapeContour.fromPoints(pointsListA, closed = false)

      val pointsListB = points.subList(end, fidelity + 1 - gapSize) + points.subList(0, start + 1 - gapSize)
      val contourB = ShapeContour.fromPoints(pointsListB, closed = false)

      return listOf(ContourWithVertices(contourA), ContourWithVertices(contourB))
    }

    /**
     * Shifts a ShapeContour by a vector amount.
     * The vector amount is based on
     * 1. how far it is from the center of the circle
     * 2. its current position (i.e. it moves outwards w.r.t. to the center of the circle)
     */
    fun explode(contour: ShapeContour, maxRadius: Double, rng: Random = Random.Default): ShapeContour {
      val contourCenter = contour.bounds.center
      // strange variable name, but idea is that the value scales in a cubic way not linearly. If it were linear scaling it would just be contourCenter.distanceTo(center) / maxRadius
      val cubicInterpPercentage = contourCenter.distanceTo(center).pow(3) / maxRadius.pow(3)
      val explodeJitter = w * 0.08 * cubicInterpPercentage
      val explodeStrength = random(-explodeJitter, explodeJitter, rng)
      val directionJitter =  PI * 0.1 * cubicInterpPercentage
      val explodeDirection = atan2(contourCenter.y - center.y, contourCenter.x - center.x) + random(-directionJitter, directionJitter, rng)
      val explodeVec = Vector2(cos(explodeDirection), sin(explodeDirection)) * explodeStrength
      val segments = contour.segments.map { Segment(it.start + explodeVec, it.end + explodeVec) }
      return ShapeContour.fromSegments(segments, closed = true)
    }

    fun subdivideUntil(dividable: ContourWithVertices, rng: Random = Random.Default, maxDepth: Int = 5, currentDepth: Int = 0): List<ContourWithVertices> {
      if (currentDepth > maxDepth) {
        return listOf(dividable)
      }
      val chanceOfSpontaneousStopping = map(0.0, maxDepth - 1.0, -0.1, 0.2, currentDepth.toDouble())
      if (random(0.0, 1.0, rng) < chanceOfSpontaneousStopping) {
        return listOf(dividable)
      }
      return dividable.subdivide(rng)
        .flatMap { subdivideUntil(it, rng, maxDepth, currentDepth + 1) }
    }

    extend {
      // get that rng
      val rng = Random(seed.toLong())

      val colors = listOf(
        ColorRGBa.BLACK,
        ColorRGBa.fromHex("DE4628"),
        ColorRGBa.fromHex("C41A11"),
        ColorRGBa.fromHex("D76A22"),
        ColorRGBa.BLACK,
        ColorRGBa.fromHex("FF5F25"),
        ColorRGBa.fromHex("D00808"),
      )
      val shatterStrokeGradient = ColorSequence(
        colors.mapIndexed { index, colorRGBa ->
          Pair(index.toDouble() / (colors.size.toDouble() - 1.0), colorRGBa)
        }
      )

      /**
       * Background glow
       */
      drawer.isolatedWithTarget(rt) {
        drawer.ortho(rt)
        drawer.clear(ColorRGBa.BLACK)
        val glowColor = shatterStrokeGradient.index(random(0.15, 0.4, rng)).opacify(0.9)
        drawer.stroke = null
        drawer.shadeStyle = RadialGradient(
          glowColor,
          // ColorRGBa.TRANSPARENT,
          // length = 0.80
          // length = 1.05,
          ColorRGBa.BLACK,
          length = 0.95,
          exponent = 0.35,
          offset = Vector2.ZERO,
        )
        drawer.circle(Circle(center, hypot(w * 0.5, h * 0.5)))
      }

      /**
       * Center "shatter" pattern
       */
      drawer.isolatedWithTarget(rt) {
        drawer.ortho(rt)

        val baseContour = Circle(center, options.shatterCircleRadius).contour
        val fidelity = 500
        val contoursWithVertices = splitInTwo(baseContour, fidelity, rng)
        val contours = contoursWithVertices.flatMap { subdivideUntil(it, rng, options.maxDepth) }
        val mappedContours = contours
          .map { it.toClosedShapeContour() }
          .map { explode(it, options.shatterCircleRadius, rng) }

        val shatterFillGradient = ColorSequence(
          listOf(
            0.0 to ColorRGBa.BLACK,
            0.5 to ColorRGBa.BLACK,
          ) +
            colors.mapIndexed { index, colorRGBa ->
              Pair(map(0.0, colors.size - 1.0, 0.8, 0.95, index.toDouble()), colorRGBa)
            } +
            listOf(
              1.0 to ColorRGBa.WHITE
            )
        )
        drawer.strokeWeight = scale(1.05)
        drawer.shadeStyle = null
        for (c in mappedContours) {
          drawer.fill = shatterFillGradient.index(random(0.0, 1.0, rng)) // .opacify(random(0.3, 0.7, rng))
          drawer.stroke = shatterStrokeGradient.index(random(0.0, 1.0, rng)) // .opacify(random(0.8, 1.0, rng))
          drawer.contour(c)
        }
      }

      drawer.scale(width.toDouble() / rt.width, TransformTarget.MODEL)
      drawer.image(rt.colorBuffer(0))

      // `true` == capture screenshot
      if (true) {
        val fileName = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
        val targetFile = File(fileName)
        // println("saving to $fileName")
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

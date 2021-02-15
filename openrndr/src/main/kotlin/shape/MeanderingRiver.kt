package shape

import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Segment
import org.openrndr.shape.ShapeContour
import util.angularDifference
import kotlin.math.abs
import kotlin.math.ceil

typealias Polyline = List<Vector2>

/**
 * Implementation of the Meander project by Robert Hodgin
 * http://roberthodgin.com/project/meander
 *
 * Terminology:
 * - tangent: a standard tangent to a curve
 * - bitangent: the perpendicular to the tangent, in the same plane as the image (as defined in the linked resource)
 *
 * Note: using lambas for parameters allows them to vary spatially
 *
 * @param channel Primary river channel for the river
 * @param oxbows List of contours that represent oxbow lakes (disconnected contours). May be empty
 * @param tangentBitangentRatio Should always return in range [0.0, 1.0], where 1.0 is full bitangent influence, 0.0 is full tangent influence, and 0.5 is a perfect mix
 * @param meanderStrength The length of the meander influence vector
 * @param curvatureScale The number of adjacent segments to evaluate when determining the curvature at a point in a contour
 * @param oxbowShrinkRate The rate at which oxbows shrink
 * @param pointTargetDistance The target distance between points when adjusting the spacing
 * @param oxbowNearnessMetric Segments that are closer together than this metric will be cut into an oxbow, unless the indices are closer than `indexNearnessMetric`
 * @param shouldSmooth when false, no smoothing will occur
 * @param firstFixedPointPct before this percentage down the channel, all points are fixed
 * @param lastFixedPointPct after this percentage down the channel, all points are fixed
 */
class MeanderingRiver(
  var channel: ShapeContour,
  var oxbows: MutableList<ShapeContour>,
  val tangentBitangentRatio: (Vector2) -> Double,
  val meanderStrength: (Vector2) -> Double,
  val curvatureScale: (Vector2) -> Int,
  val oxbowShrinkRate: Double,
  val smoothness: Int,
  val pointTargetDistance: (Vector2) -> Double,
  val oxbowNearnessMetric: Double,
  val shouldSmooth: Boolean,
  val firstFixedPointPct: Double,
  val lastFixedPointPct: Double,
) {
  val indexNearnessMetric = ceil(curvatureScale(Vector2.ONE) * 1.5).toInt()
  val contours: List<ShapeContour>
    get() = listOf(channel) + oxbows
  val points: Polyline
    get() = channel.segments.map { it.start } + channel.segments.last().end

  fun run() {
    // This could obviously be written more cleanly but this way gives a lot of control to tune when and how the line is smoothed - good for experimentation
    var polyline = meander(channel.segments)
    polyline = smooth(polyline)
    polyline = joinMeanders(polyline)
    // polyline = smooth(polyline)
    polyline = adjustSpacing(polyline)
    polyline = smooth(polyline)
    channel = ShapeContour.fromPoints(polyline, closed = false)

    // shrinking oxbows doesn't matter w.r.t. smoothing or state mutation because it doesn't affect the channel, only the oxbows
    shrinkOxbows()
  }

  fun influenceVectors(showEvery: Int = 1): List<LineSegment> =
    channel
      .segments
      .mapIndexedNotNull { i, s ->
        when {
          i % showEvery == 0 -> LineSegment(s.start, s.start + influence(s, i, channel.segments))
          else -> null
        }
      }

  // chaikinSmooth(points, smoothness) adds points to increase smoothness. Might be worth experimenting with at some point
  private fun smooth(points: Polyline): Polyline = when {
    shouldSmooth -> SmoothLine(points).movingAverage(smoothness)
    else -> points
  }

  /**
   * Calculate the "meandering forces" at each point, then apply the forces and create a new contour based on the updated values
   * Use the difference between segment's start and end points as a proxy for the tangent.
   * To ensure a continuous end result, we only apply the meandering forces to the start point,
   * and then create a new ShapeContour from those points
   */
  private fun meander(segments: List<Segment>): Polyline {
    // split the segments into 3 parts.
    // The first and third parts do not move (stops the line from going offscreen a lot)
    // The middle part is the only part that meanders
    val firstFixedIndex = (segments.size * firstFixedPointPct).toInt()
    val lastFixedIndex = (segments.size * lastFixedPointPct).toInt()
    val firstFixedPoints = segments.subList(0, firstFixedIndex).map { it.start }
    val lastFixedPoints = segments.subList(lastFixedIndex, segments.size).map { it.start } + segments.last().end
    val middleSegments = segments.subList(firstFixedIndex, lastFixedIndex)

    // Apply meandering influence to the points in the second section.
    // influence must be added to start point for final position
    val nextPoint = { i: Int, segment: Segment -> segment.start + influence(segment, i + firstFixedIndex, segments) }
    val adjustedMiddlePoints = middleSegments.mapIndexed(nextPoint)
    return firstFixedPoints + adjustedMiddlePoints + lastFixedPoints
  }

  /**
   * Calculate the bitangent direction from the curvature of the contour,
   * and blend with the tangent. Then, scale the result by the curvature at that point.
   * The bitangent is always perpendicular to the tangent, but it's direction of rotation
   * depends on the predominant curvature at the given point
   */
  private fun influence(segment: Segment, i: Int, segments: List<Segment>): Vector2 {
    val tangent = tangent(segment)
    // Use surrounding segments to determine curvature
    val min = if (i < curvatureScale(segment.start)) 0 else i - curvatureScale(segment.start)
    val max = if (i > segments.size - curvatureScale(segment.start)) segments.size else i + curvatureScale(segment.start)
    val curvature = averageCurvature(segments.subList(min, max))
    // in openrndr, positive rotation is clockwise. But, I have no intuition around YPolarity because it depends on which way the vector is pointing, and it all gets very confusing very fast
    val polarity = if (curvature < 0.0) YPolarity.CCW_POSITIVE_Y else YPolarity.CW_NEGATIVE_Y
    val bitan = tangent.perpendicular(polarity)

    return tangent.normalized.mix(bitan.normalized, tangentBitangentRatio(segment.start)) * abs(curvature) * meanderStrength(segment.start)
  }

  /**
   * tangent always points forwards, in the direction of the segments in the channel
   */
  private fun tangent(segment: Segment): Vector2 = segment.end - segment.start

  /**
   * use atan2 b/c cross product doesn't work so well for this implementation
   * get the average difference in orientation between a list of segments
   */
  private fun averageCurvature(segments: List<Segment>): Double {
    val diffs = segments.mapIndexedNotNull { index, segment ->
      if (index == segments.size - 1) {
        null
      } else {
        angularDifference(segments[index + 1], segment)
      }
    }
    return diffs.sum() / diffs.size.toDouble()
  }

  /**
   * Detect segments that are spatially close but but not close in index.
   * Join these segments together and cut off the old segment into an "oxbow" lake
   *
   * Algorithm in a nutshell:
   * 1. For each point in the polyline
   * 2. Loop through all points with higher index
   * 3. If a point is spatially close and the indices are sufficiently far apart,
   *    add it as the next point in the new list,
   *    and create an oxbow from the interim pieces.
   *    Resume iteration from the next point's index
   */
  private fun joinMeanders(polyline: Polyline): Polyline {
    val line = mutableListOf<Vector2>()
    var i = 0
    while (i < polyline.size) {
      val point = polyline[i]
      line.add(point)

      // We only need to compare to the points "in front of" our current point -- we'll never join backwards
      for (j in i until polyline.size) {
        val other = polyline[j]

        // check for proximity:
        // if points are proximate, we should cut the interim pieces into an oxbow, UNLESS the indices are very close
        if (potentialOxbow(point, other) && !indicesAreNear(i, j)) {
          line.add(other)
          addOxbow(i, j)
          // i will be incremented again below, but that's OK since we already added `polyline[j]` (a.k.a. `other`)
          i = j
          break
        }
      }
      i++
    }
    return line
  }

  private fun potentialOxbow(p1: Vector2, p2: Vector2): Boolean = p1.distanceTo(p2) < oxbowNearnessMetric
  private fun indicesAreNear(i: Int, j: Int): Boolean = abs(i - j) <= indexNearnessMetric

  private fun addOxbow(i: Int, j: Int) {
    oxbows.add(ShapeContour(channel.segments.subList(i, j), closed = false))
  }

  /**
   * As points move (and others do not), the relative spacing of segments may become unbalanced.
   * On each iteration, check all segments and remove if they are too close together, or add if they are too far apart
   */
  private fun adjustSpacing(polyline: Polyline): Polyline =
    polyline
      .flatMapIndexed { index, point ->
        if (index == 0 || index == polyline.size - 1) {
          return@flatMapIndexed listOf(point)
        }

        val next = polyline[index + 1]
        val targetDist = pointTargetDistance(point)
        val distance = next.distanceTo(point)

        // If too far apart, add a midpoint
        // If too close, skip current point
        // If neither too close nor too far, add the point normally
        when {
          distance > targetDist -> {
            // ensure that for points with large distances between, an appropriate number of midpoints are added
            val nMidpointsNeeded = (distance / targetDist).toInt() + 1
            (0 until nMidpointsNeeded).map { i -> point.mix(next, 1.0 / nMidpointsNeeded * i.toDouble()) }
          }
          distance < targetDist * 0.3 -> listOf()
          else -> listOf(point)
        }
      }

  /**
   * Over time, the oxbows (disconnected meanders) will shrink and disappear
   */
  private fun shrinkOxbows() {
    // TODO: erase oxbows that are in danger of being overlapped by the channel
    oxbows = oxbows
      // get rid of oxbows that are too small (4 segments are required to handle the shrinking algorithm)
      .filter { it.length > oxbowShrinkRate && it.segments.size > 4 }
      .map {
        val percentage = oxbowShrinkRate / it.length / 2.0
        val sub = it.sub(percentage, 1.0 - percentage)

        // `contour.sub` method doesn't always shrink the contour. If that's the case, we need to manually adjust the size of the first and last segments in the contour
        if (sub.length == it.length) {
          val segments = mutableListOf<Segment>()
          if (it.segments.first().length > oxbowShrinkRate) {
            segments.add(Segment(it.segments.first().start - tangent(it.segments.first()).normalized * oxbowShrinkRate, it.segments.first().end))
          }
          segments.addAll(it.segments.subList(1, it.segments.size - 1))
          if (it.segments.last().length > oxbowShrinkRate) {
            segments.add(Segment(it.segments.last().start, it.segments.last().end + tangent(it.segments.last()).normalized * oxbowShrinkRate))
          }
          ShapeContour(segments, closed = false)
        } else {
          sub
        }
      }
      .toMutableList()
  }
}

/**
 * A naive attempt at a "builder" pattern. I'm not a JVM person by nature so this is new to me
 *
 * @param channel Primary river channel for the river
 * @param oxbows List of contours that represent oxbow lakes (disconnected contours). May be empty
 * @param tangentBitangentRatio Should always return in range [0.0, 1.0], where 1.0 is full bitangent influence, 0.0 is full tangent influence, and 0.5 is a perfect mix
 * @param meanderStrength The length of the meander influence vector
 * @param curvatureScale The number of adjacent segments to evaluate when determining the curvature at a point in a contour
 * @param oxbowShrinkRate The rate at which oxbows shrink
 * @param pointTargetDistance The target distance between points when adjusting the spacing
 * @param oxbowNearnessMetric Segments that are closer together than this metric will be cut into an oxbow, unless the indices are closer than `indexNearnessMetric`
 * @param shouldSmooth when false, no smoothing will occur
 */
class MeanderingRiverBuilder {
  var channel: ShapeContour = ShapeContour.fromPoints(listOf(Vector2.ZERO, Vector2.ONE), closed = false)
    get() = when {
      // TODO: interpolate `nSegments` points between `start` and `end`
      start != null && end != null -> ShapeContour.fromPoints(listOf(start!!, end!!), closed = false)
      points.isNotEmpty() -> ShapeContour.fromPoints(SmoothLine(points).movingAverage(smoothness), closed = false)
      else -> field
    }
  var points: Polyline = listOf()
  var start: Vector2? = null
  var end: Vector2? = null
  var nSegments: Int? = null
  var oxbows: MutableList<ShapeContour> = mutableListOf()
  var tangentBitangentRatio: (Vector2) -> Double = { 0.5 }
  var meanderStrength: (Vector2) -> Double = { 10.0 }
  var curvatureScale: (Vector2) -> Int = { 3 }
  var oxbowShrinkRate: Double = 2.0
  var smoothness: Int = 5
  var pointTargetDistance: (Vector2) -> Double = { 5.0 }
  var oxbowNearnessMetric: Double = 10.0
  var shouldSmooth: Boolean = true
  var firstFixedPointPct: Double = 0.05
  var lastFixedPointPct: Double = 0.85

  fun toMeanderingRiver(): MeanderingRiver =
    MeanderingRiver(
      channel, oxbows, tangentBitangentRatio, meanderStrength, curvatureScale, oxbowShrinkRate,
      smoothness, pointTargetDistance, oxbowNearnessMetric, shouldSmooth, firstFixedPointPct, lastFixedPointPct
    )
}

// I'm not totally sure I understand the builder pattern but I'm still exploring
fun meanderRiver(f: MeanderingRiverBuilder.() -> Unit): MeanderingRiver {
  val builder = MeanderingRiverBuilder()
  builder.f()
  return builder.toMeanderingRiver()
}

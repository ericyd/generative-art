/*
I wanted to test an alternative algorithm for the "fractal subdivision" algorithm:
http://rectangleworld.com/blog/archives/413
http://rectangleworld.com/blog/archives/462

My original implementation uses regular Lists, and performs the fractalization by
instantiating new Lists on each iteration. This is optimized for a List's fast `add` method.

In contrast, the original implementation uses a minimal custom "linked list" type structure.
I wanted to see if it was substantially more performant that creating tons of new lists.
Turns out, it is! Not terribly surprising given that it's modifying the structure in place
rather than a more "functional-ish" approach.

However, I don't feel that the increased complexity of implementation is probably worth it for
relatively small performance gains.

With 15 subdivisions:
------------------------------------
gauss (list)        :   4.11 s,  1,000 samples,  4,107.00 ( 4,107.00) ms / 1000 samples,   243.49 (243.49) hz
gauss (linkedlist)  :   3.64 s,  1,000 samples,  3,636.92 ( 3,636.92) ms / 1000 samples,   274.96 (274.96) hz
perp (list)         :   3.01 s,  1,000 samples,  3,014.24 ( 3,014.24) ms / 1000 samples,   331.76 (331.76) hz
perp (linkedlist)   :   2.61 s,  1,000 samples,  2,612.20 ( 2,612.20) ms / 1000 samples,   382.82 (382.82) hz
------------------------------------

With 17 subdivisions
------------------------------------
gauss (list)        :  17.26 s,  1,000 samples, 17,261.81 (17,261.81) ms / 1000 samples,   57.93 (57.93) hz
gauss (linkedlist)  :  16.17 s,  1,000 samples, 16,171.64 (16,171.64) ms / 1000 samples,   61.84 (61.84) hz
perp (list)         :  13.00 s,  1,000 samples, 12,996.68 (12,996.68) ms / 1000 samples,   76.94 (76.94) hz
perp (linkedlist)   :  11.70 s,  1,000 samples, 11,702.14 (11,702.14) ms / 1000 samples,   85.45 (85.45) hz
------------------------------------

From this we can see that while the LinkedList does slightly outperform the List implementation,
it scales approximately linearly with the number of subdivisions.

=====================================

Caveat: This doesn't measure memory, and I have a feeling that memory usage would be SUBSTANTIALLY
lower with the LinkedList implementation. So, if I find I'm running out of memory, perhaps I should
try using this solution
*/
package sketch.test

import extensions.SimpleProfiler
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.random
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment
import shape.FractalizedLine
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Node<T : Any>(val value: T, var next: Node<T>? = null)

private class CustomLinkedList<T : Any>(initialItems: List<T>?) {
  var head: Node<T>? = null
  init {
    if (!initialItems.isNullOrEmpty()) {
      head = Node(initialItems.first())
      var item = head!!
      for (index in 1 until initialItems.size) {
        val next = Node(initialItems[index])
        item.next = next
        item = next
      }
    }
  }

  val size: Int
    get() {
      var calcSize = 0
      return when (head) {
        is Node<T> -> {
          var element = head
          calcSize++
          while (element?.next != null) {
            calcSize++
            element = element.next
          }
          calcSize
        }
        else -> 0
      }
    }
}

private class FractalizedLinkedList(var points: CustomLinkedList<Vector2>, private val rng: Random = Random.Default) {

  val segments: List<Segment>
    get() {
      val segments = mutableListOf<Segment>()
      if (points.head != null) {
        var point = points.head!!
        while (point.next != null) {
          val next = point.next!!
          segments.add(Segment(point.value, next.value))
          point = next
        }
      }
      return segments
    }

  // Will need to implement this if I want to use it
  //
  // val shape: ShapeContour
  //   get() = ShapeContour(segments, true)

  /**
   * Recursively subdivide the points using perpendicular offset
   */
  fun perpendicularSubdivide(subdivisions: Int, offsetPct: Double = 0.50): FractalizedLinkedList =
    subdivide(subdivisions, offsetPct, ::perpendicularOffset)

  /**
   * Recursively subdivide the points using gaussian offset
   * The default offsetPct here is intentionally lower than perpendicular because it gets _wild_ real quick
   */
  fun gaussianSubdivide(subdivisions: Int, offsetPct: Double = 0.35): FractalizedLinkedList =
    subdivide(subdivisions, offsetPct, ::gaussianOffset)

  /**
   * Recursively subdivide the points
   */
  private fun subdivide(subdivisions: Int, offsetPct: Double = 0.5, offsetFn: (Vector2, Vector2, Double) -> Vector2): FractalizedLinkedList {
    for (i in 0 until subdivisions) {
      // TODO: add check for null head
      if (points.head == null) {
        return this
      }
      var point = points.head!!

      while (point.next != null) {
        val mid = Node(offsetFn(point.value, point.next!!.value, offsetPct))
        val newNext = point.next
        mid.next = newNext
        point.next = mid
        point = newNext!!
      }
    }
    return this
  }

  private fun perpendicularOffset(start: Vector2, end: Vector2, offsetPct: Double): Vector2 {
    val perpendicular = atan2(end.y - start.y, end.x - start.x) - (PI / 2.0)
    val maxDeviation = (start - end).length / 2.0 * offsetPct
    val mid = (start + end) / 2.0
    val offset = random(-maxDeviation, maxDeviation, rng)
    return mid + Vector2(cos(perpendicular) * offset, sin(perpendicular) * offset)
  }

  private fun gaussianOffset(start: Vector2, end: Vector2, offsetPct: Double): Vector2 =
    Vector2.gaussian(
      // midpoint
      mean = (start + end) / 2.0,
      deviation = Vector2((start - end).length / 2.0) * offsetPct,
      random = rng
    )
}

fun main() = application {
  configure {
    width = 750
    height = 750
  }

  program {

    backgroundColor = ColorRGBa.WHITE

    val seed = random(1.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")
    val rng = Random(seed.toLong())

    val profiler = SimpleProfiler(resetAfterSampleCount = 500000)

    // Be careful! Every subdivision adds 2^(n-1) points
    // (or something like that, I'm not sure I thought it through fully).
    // But yeah, there's some type of exponential growth to the size of the resulting list
    val subdivisions = 15

    var line1: FractalizedLine? = null
    var line2: FractalizedLine? = null
    var line3: FractalizedLinkedList? = null
    var line4: FractalizedLinkedList? = null

    List(1000) {
      profiler.startSection("perp (list)")
      line1 = FractalizedLine(listOf(Vector2(20.0, height * 0.2), Vector2(width - 20.0, height * 0.2)), rng)
        .perpendicularSubdivide(subdivisions)
      profiler.endSection()

      profiler.startSection("gauss (list)")
      line2 = FractalizedLine(listOf(Vector2(20.0, height * 0.4), Vector2(width - 20.0, height * 0.4)), rng)
        .gaussianSubdivide(subdivisions)
      profiler.endSection()

      // ok.... something about this linkedList implementation is THE SLOWEST THING EVER...
      // need to investigate because I thought quick inserts was literally the point of a LL
      val ll1 = CustomLinkedList<Vector2>(listOf(Vector2(20.0, height * 0.6), Vector2(width - 20.0, height * 0.6)))
      profiler.startSection("perp (linkedlist)")
      line3 = FractalizedLinkedList(ll1, rng)
        .perpendicularSubdivide(subdivisions)
      profiler.endSection()

      val ll2 = CustomLinkedList<Vector2>(listOf(Vector2(20.0, height * 0.8), Vector2(width - 20.0, height * 0.8)))
      profiler.startSection("gauss (linkedlist)")
      line4 = FractalizedLinkedList(ll2, rng)
        .gaussianSubdivide(subdivisions)
      profiler.endSection()

      if (profiler.report(10))
        println("----------------------")
    }

    profiler.report(0)
    println("about to draw")

    extend {
      drawer.fill = null
      drawer.stroke = ColorRGBa.BLACK
      drawer.strokeWeight = 2.0

      drawer.segments(line1!!.segments)
      drawer.segments(line2!!.segments)
      drawer.segments(line3!!.segments)
      drawer.segments(line4!!.segments)
    }
  }
}

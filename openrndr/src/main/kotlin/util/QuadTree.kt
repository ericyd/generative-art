package util

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

interface QuadTreeNode {
  val position: Vector2
}

// naming...
open class QTreeNode(override val position: Vector2) : QuadTreeNode

class QuadTree(val boundary: Rectangle, val capacity: Int) {
  var isSubdivided = false
  val points = mutableListOf<QuadTreeNode>()
  var northeast: QuadTree? = null
  var southeast: QuadTree? = null
  var southwest: QuadTree? = null
  var northwest: QuadTree? = null

  fun <T : QuadTreeNode> add(point: T) {
    if (!boundary.contains(point.position)) {
      return
    }

    if (points.size < capacity) {
      points.add(point)
      return
    }

    if (!isSubdivided) {
      subdivide()
    }

    northeast!!.add(point)
    southeast!!.add(point)
    southwest!!.add(point)
    northwest!!.add(point)
  }

  fun <T : QuadTreeNode> addAll(points: List<T>) {
    for (point in points) {
      this.add(point)
    }
  }

  private fun subdivide() {
    val w = boundary.width / 2.0
    val h = boundary.height / 2.0

    val ne = Rectangle(boundary.center - Vector2(0.0, h), w, h)
    northeast = QuadTree(ne, capacity)

    val se = Rectangle(boundary.center, w, h)
    southeast = QuadTree(se, capacity)

    val sw = Rectangle(boundary.center - Vector2(w, 0.0), w, h)
    southwest = QuadTree(sw, capacity)

    val nw = Rectangle(boundary.corner, w, h)
    northwest = QuadTree(nw, capacity)

    isSubdivided = true
  }

  fun <T : QuadTreeNode> query(rect: Rectangle): List<T> {
    if (!boundary.intersects(rect)) {
      return listOf()
    }

    val northeastPoints = northeast?.query<T>(rect) ?: listOf()
    val southeastPoints = southeast?.query<T>(rect) ?: listOf()
    val southwestPoints = southwest?.query<T>(rect) ?: listOf()
    val northwestPoints = northwest?.query<T>(rect) ?: listOf()
    return (points as MutableList<T>).filter { rect.contains(it.position) } +
      northeastPoints +
      southeastPoints +
      southwestPoints +
      northwestPoints
  }

  fun <T : QuadTreeNode> queryCenteredAt(point: Vector2, rect: Rectangle): List<T> {
    return this.query(rect.moved(point - rect.center))
  }

  fun clone(): QuadTree {
    val newTree = QuadTree(boundary, capacity)
    newTree.addAll(query(boundary))
    return newTree
  }
}

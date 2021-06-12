package shape

import force.MovingBody
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import util.QuadTree

/**
 * Differential Line
 * A form of growing a line (set of points) to simulate organic growth
 *
 * Resources:
 * 1. http://www.codeplastic.com/2017/07/22/differential-line-growth-with-processing/
 * 2. https://inconvergent.net/generative/differential-line/
 *
 * Default values taken from Resource #1
 */
class DifferentialLine(
  var nodes: MutableList<MovingBody> = mutableListOf(),
  var maxForce: (MovingBody) -> Double = { _ -> 0.9 },
  /**
   * When nodes are further away than `maxNodeSeparation`, a new node will be added at their midpoint
   */
  var maxNodeSeparation: (MovingBody) -> Double = { _ -> 5.0 },
  // When true, the edges of the `nodes` list do not move
  var fixedEdges: Boolean = false,
  var closed: Boolean = false,
  var bounds: Rectangle = Rectangle(0.0, 0.0, 0.0, 0.0),
  /**
   * Customizable rule(s) that determine whether or not a node should spawn at a particular point.
   * This may actually benefit from getting a reference to the quadtree too, but for now this is fine
   */
  var spawnRule: (MovingBody, QuadTree) -> Boolean = { _, _ -> true },
  var cohesionForceFactor: (MovingBody) -> Double = { 1.0 },
  var separationForceFactor: (MovingBody) -> Double = { 1.0 },
) {
  private val quadtreeCapacity = 10
  var qtree: QuadTree = QuadTree(bounds, quadtreeCapacity)

  // Since we aren't using a very official builder pattern, there are some things that need to happen after the values are applied
  fun init() {
    qtree = QuadTree(bounds, quadtreeCapacity)
    for (node in nodes) {
      qtree.add(node)
    }
  }

  val smoothLine: SmoothLine
    get() = SmoothLine(nodes.map { it.position })

  fun run() {
    differentiate()
    grow()
  }

  fun differentiate() {
    for ((i, node) in nodes.withIndex()) {
      if (!closed && fixedEdges && (i == 0 || i == nodes.size - 1)) {
        continue
      }
      applySeparationForces(node)

      val maximum = maxForce(node)

      // this first one is very important to ensure that separation and cohesion aren't too unbalanced
      // TODO: would be nice to refactor this into some type of "limit" function, maybe in MovingBody
      if (node.acceleration.length > maximum) {
        node.acceleration = node.acceleration.normalized * maximum
      }

      // Cohesion prevents the shape from overlapping itself
      applyCohesionForce(node, i)

      if (node.acceleration.length > maximum) {
        node.acceleration = node.acceleration.normalized * maximum
      }

      node.update()
    }
  }

  // Extremely similar to `subdivide` function in FractalizedLine
  fun grow() {
    val newNodes = mutableListOf<MovingBody>()
    // TODO: would it be better to _only_ add new nodes to QuadTree, and avoid clearing it each time? Downside: would not account for node positions moving through time
    qtree = QuadTree(bounds, quadtreeCapacity)

    // Iterate through all points.
    // Skip the last index because we are accessing j+1 to get the "next" point anyway.
    // The last point will be added after the loop
    for (j in 0 until nodes.size - 1) {
      val current = nodes[j]
      val next = nodes[j + 1]
      newNodes.add(current)
      qtree.add(current)

      // This is our "rule" for whether or not to insert a node at this position
      // current.position.distanceTo(next.position) > maxNodeSeparation(current)
      // gahhh!! Just realized that the qtree isn't even fully populated at this point 🤦🏻‍ so this won't really be effective for a large number of the points
      if (current.position.distanceTo(next.position) > maxNodeSeparation(current) && spawnRule(current, qtree)) {
        val mid = MovingBody((current.position + next.position) / 2.0)
        newNodes.add(mid)
        qtree.add(mid)
      }
    }
    newNodes.add(nodes.last())
    qtree.add(nodes.last())

    // add point between "end" and "start" if distance is too big and shape is closed
    if (closed && nodes.last().position.distanceTo(nodes.first().position) > maxNodeSeparation(nodes.last())) {
      val mid = MovingBody((nodes.last().position + nodes.first().position) / 2.0)
      newNodes.add(mid)
      qtree.add(mid)
    }

    nodes = newNodes
  }

  private fun applySeparationForces(node: MovingBody): Int {
    val scaledRange = bounds.scale(0.1)
    val searchRange = scaledRange.moved(node.position - scaledRange.center)
    val otherNodes = qtree.query(searchRange)

    for (other in otherNodes) {
      val force = separationForce(node, other as MovingBody) * separationForceFactor(node)
      if (force.length > 0.0) {
        node.applyForce(force)
        other.applyForce(force * -1.0)
      }
    }

    return otherNodes.size
  }

  private fun separationForce(n1: MovingBody, n2: MovingBody): Vector2 {
    // variation 1 (squared distance, no normalize
    val squaredDistance = n2.position.squaredDistanceTo(n1.position)
    val diff = (n1.position - n2.position)
    return (diff / squaredDistance) // Weight by distance

    // // variation 2 (regular distance, normalize
    // val distance = n2.position.distanceTo(n1.position)
    // val diff = (n1.position - n2.position).normalized // TODO: is .normalized needed? Answer: YES (but why???)
    // return (diff / distance) // Weight by distance
  }

  private fun applyCohesionForce(node: MovingBody, i: Int) {
    val adjacentNodesMidpoint = getAdjacentNodesMidpoint(i)
    val cohesionForce = cohesionForce(node, adjacentNodesMidpoint)
    node.applyForce(cohesionForce)
  }

  private fun getAdjacentNodesMidpoint(index: Int): Vector2 {
    val size = nodes.size
    val isFirst = index == 0
    val isLast = index == size - 1
    val adjacentNodePositionsSum = if (!isFirst && !isLast) {
      nodes[index - 1].position + nodes[index + 1].position
    } else if (isFirst) {
      nodes.last().position + nodes[index + 1].position
    } else if (isLast) {
      nodes[index - 1].position + nodes.first().position
    } else { // should never happen
      nodes[index].position * 2.0
    }
    return adjacentNodePositionsSum / 2.0
  }

  /**
   * The purpose of this is to calculate a force between the `node` and the `target`
   */
  fun cohesionForce(node: MovingBody, target: Vector2): Vector2 {
    // normalized vs denormalized
    // normalized seems to give a "knobby-er" appearance, which I like
    // but also seems to make it grow more slowly. Hmm
    var steer = (target - node.position).normalized
    steer -= node.velocity // TODO: figure out why this is required
    steer *= cohesionForceFactor(node)
    // steer -= node.acceleration

    return steer
  }
}

// I know this isn't a true "builder" pattern but I just wanted a shortcut way to write this
fun differentialLine(f: DifferentialLine.() -> Unit): DifferentialLine {
  val line = DifferentialLine().apply(f)
  line.init()
  return line
}

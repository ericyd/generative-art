package shape

import force.MovingBody
import org.openrndr.math.Vector2
import kotlin.math.sqrt

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
  var maxForce: Double = 0.9,
  var maxSpeed: Double = 1.0,
  desiredSeparation: Double = 9.0,
  var separationCohesionRatio: Double = 1.1,
  var maxEdgeLen: Double = 5.0,
  // When true, the edges of the `nodes` list do not move
  var fixedEdges: Boolean = false,
) {
  var desiredSeparation = desiredSeparation
    set(value) {
      squaredDesiredSeparation = value * value
      field = value
    }
  var squaredDesiredSeparation: Double = desiredSeparation * desiredSeparation

  val smoothLine: SmoothLine
    get() = SmoothLine(nodes.map { it.position })

  fun run() {
    differentiate()
    grow()
  }

  fun differentiate() {
    for ((i, node) in nodes.withIndex()) {
      if (fixedEdges && (i == 0 || i == nodes.size - 1)) {
        continue
      }
      val nearNodes = applySeparationForces(node)

      // apply some damping to the node so it doesn't explode too fast
      if (nearNodes > 0) {
        // exponent and accuracy change this image quite a bit
        node.applyFriction(Math.pow(nearNodes.toDouble(), 2.25), 0.13)
      }
      if (node.acceleration.length > 0) {
        node.acceleration = node.acceleration.normalized * maxSpeed
        if (node.acceleration.length > maxForce) {
          node.acceleration = node.acceleration.normalized * maxForce
        }
      }
      node.acceleration *= separationCohesionRatio

      // Cohesion prevents the shape from overlapping itself
      node.applyForce(cohesionForce(node, i))
      node.update()
    }
  }

  // Extremely similar to `subdivide` function in FractalizedLine
  fun grow() {
    val newNodes = mutableListOf<MovingBody>()

    // Iterate through all points.
    // Skip the last index because we are accessing j+1 to get the "next" point anyway.
    // The last point will be added after the loop
    for (j in 0 until nodes.size - 1) {
      val current = nodes[j]
      val next = nodes[j + 1]
      newNodes.add(current)

      // This is our "rule" for whether or not to insert a node at this position
      if (current.position.distanceTo(next.position) > maxEdgeLen) {
        val mid = MovingBody((current.position + next.position) / 2.0)
        newNodes.add(mid)
      }
    }
    newNodes.add(nodes.last())

    nodes = newNodes
  }

  private fun applySeparationForces(node: MovingBody): Int {
    var nearNodes = 0
    for (other in nodes) {
      val force = separationForceBetween(node, other)
      if (force.length > 0.0) {
        node.applyForce(force)
        other.applyForce(force * -1.0)
        nearNodes++
      }
    }
    return nearNodes
  }

  private fun separationForceBetween(n1: MovingBody, n2: MovingBody): Vector2 {
    var steer = Vector2.ZERO
    val squaredDistance = n2.position.squaredDistanceTo(n1.position)
    if (squaredDistance > 0.0 && squaredDistance < squaredDesiredSeparation) {
      val diff = (n1.position - n2.position).normalized
      steer += (diff / sqrt(squaredDistance)) // Weight by distance
    }
    return steer
  }

  private fun cohesionForce(node: MovingBody, index: Int): Vector2 {
    val size = nodes.size
    val sum = if (index != 0 && index != size - 1) {
      nodes[index - 1].position + nodes[index + 1].position
    } else if (index == 0) {
      nodes[size - 1].position + nodes[index + 1].position
    } else if (index == size - 1) {
      nodes[index - 1].position + nodes[0].position
    } else {
      Vector2.ZERO
    }
    return node.seek(sum / 2.0, maxSpeed, maxForce)
  }
}

// I know this isn't a true "builder" pattern but I just wanted a shortcut way to write this
fun differentialLine(f: DifferentialLine.() -> Unit): DifferentialLine {
  return DifferentialLine().apply(f)
}

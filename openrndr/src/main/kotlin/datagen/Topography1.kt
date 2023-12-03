package datagen

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.openrndr.extra.noise.random
import org.openrndr.math.clamp
import org.openrndr.math.map
import util.timestamp
import java.io.File
import java.time.LocalDateTime
import kotlin.random.Random

enum class DataOrientation {
  ROW_MAJOR,
  COLUMN_MAJOR
}

data class TopographyResult(val nCols: Int, val nRows: Int, val initialHeight: Double, val orientation: DataOrientation, val points: List<Double>)

/**
 * This is a computational function that models rain falling on a landscape and eroding it.
 * It crunches a bunch of numbers and then writes JSON to a file.
 */
fun main() {
  println("start at ${timestamp()}")
  /** Initial conditions */
  val iterations = 50000
  val nCols = 1000
  val nRows = 1000
  val initialHeight = 100.0
  val rainChancePercentage = 0.6 // Rain has 90% chance of falling
  val erosivePower = 0.001
  val cascadeDuration = 100 // length/distance a drop will drip downhill
  var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
  println("seed = $seed")
  val rng = Random(seed)
  val gridSize = nCols * nRows
  val grid = MutableList(gridSize) {
    map(0.0, gridSize.toDouble(), initialHeight, initialHeight * 0.8, it.toDouble()) + random(-0.001, 0.001, rng)
  }
  // !! Row-Major !!
  val getIndex = { x: Int, y: Int -> x * nCols + y }

  for (i in 0 until iterations) {
    if (i % 100 == 0) {
      println("iteration $i of $iterations")
    }
    for (x in 0 until nCols) {
      for (y in 0 until nRows) {
        val index = getIndex(x, y)
        // does rain fall on this grid point?
        if (random(0.0, 1.0, rng) >= rainChancePercentage) {
          continue
        }

        // rain has fallen; erode this point
        grid[index] = clamp(grid[index] - erosivePower, 0.0, initialHeight)

        // cascade to lower points - lowest point gets the droplet
        // (optional future enhancement: each lower point gets a portion of the rain based on it's relative height)
        var lowestNeighbor = findLowest(grid, x, y, getIndex)
        var cascadeCount = 0
        while (lowestNeighbor != null && cascadeCount <= cascadeDuration) {
          val (lowestNeighborX, lowestNeighborY) = lowestNeighbor
          val lowestHeightIndex = getIndex(lowestNeighborX, lowestNeighborY)
          val cascadeErosivePower = map(0.0, cascadeDuration.toDouble(), erosivePower * 0.8, erosivePower * 0.1, cascadeCount.toDouble())
          grid[lowestHeightIndex] = clamp(grid[lowestHeightIndex] - cascadeErosivePower, 0.0, initialHeight)
          lowestNeighbor = findLowest(grid, lowestNeighborX, lowestNeighborY, getIndex)
          cascadeCount++
        }
      }
    }
  }

  val end = LocalDateTime.now()
  println("end at ${timestamp(end)}")

  val gson = Gson()
  val filename = "screenshots/datagen/Topography1-${timestamp(end)}-seed-$seed.json"
  val file = File(filename)
  file.parentFile?.let { parent ->
    if (!parent.exists()) {
      parent.mkdirs()
    }
  }
  file.writeText(gson.toJson(TopographyResult(nCols, nRows, initialHeight, DataOrientation.ROW_MAJOR, grid)))
  println("written to $filename")
}

fun findLowest(grid: MutableList<Double>, x: Int, y: Int, getIndex: (x: Int, y: Int) -> Int): Pair<Int, Int>? {
  var lowestHeight = 0.0
  var lowestIndex: Pair<Int, Int>? = null
  for (i in (x - 1)..(x + 1)) {
    for (j in (y - 1)..(y + 1)) {
      val index = getIndex(i, j)
      if (index < 0 || index > grid.size - 1) {
        continue
      }
      if (grid[index] < lowestHeight) {
        lowestIndex = Pair(i, j)
        lowestHeight = grid[index]
      }
    }
  }
  return lowestIndex
}

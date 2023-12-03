package sketch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.random
import org.openrndr.extra.shapes.toRounded
import org.openrndr.shape.*
import org.openrndr.svg.saveToFile
import util.timestamp
import java.io.File
import kotlin.math.*
import kotlin.random.Random

fun main() = application {
  configure {
    width = 800
    height = 800
  }

  program {
    val progName = this.name.ifBlank { this.window.title.ifBlank { "my-amazing-drawing" } }
    // Seed is the basis for all our randomization, because it is used to create a seeded RNG (Random(seed))
    var seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
    println("seed = $seed")

    val screenshots = extend(Screenshots()) {
      quitAfterScreenshot = false
      contentScale = 3.0
      captureEveryFrame = false
      name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
    }

    val bg = ColorRGBa.WHITE
    backgroundColor = bg

    fun gridByDimensions(nCols: Int, nRows: Int): List<Pair<Int, Int>> =
      (0 until nCols).flatMap { x ->
        (0 until nRows).map { y ->
          Pair(x, y)
        }
      }

    // This will find all available sizes without any chance of overlaps
    fun findAvailableSizes(grid: List<Boolean>, x: Int, y: Int, nCols: Int, nRows: Int, getIndex: (x: Int, y: Int) -> Int, maxDimension: Int): List<Pair<Int, Int>> {
      // compute the available sizes, e.g. 1x1, 1x2, 2x2, 2x3, etc, up to 4x4
      val availableSizes = mutableListOf<Pair<Int, Int>>()
      /**
       * This way is ultimate brute force, check all combos twice because
       * different orientations might be different
       * Gotta be a better way
       */
      for ((i, j) in gridByDimensions(maxDimension, maxDimension)) {
        if (i + x > nCols - 1 || j + y > nRows - 1) {
          continue
        }
        if (!grid[getIndex(i + x, j + y)]) {
          availableSizes.add(Pair(i + 1, j + 1))
        } else {
          break
        }
      }
      for ((j, i) in gridByDimensions(maxDimension, maxDimension)) {
        if (i + x > nCols - 1 || j + y > nRows - 1) {
          continue
        }
        val pair = Pair(i + 1, j + 1)
        if (!grid[getIndex(i + x, j + y)] && availableSizes.contains(pair)) {
          availableSizes.add(pair)
        } else {
          break
        }
      }
      return availableSizes
    }

    // This has the possibility of overlaps, but that can be kind of visually interesting
    fun findAvailableSizesWithOverlaps(grid: List<Boolean>, x: Int, y: Int, nCols: Int, nRows: Int, getIndex: (x: Int, y: Int) -> Int, maxDimension: Int): List<Pair<Int, Int>> {
      // compute the available sizes, e.g. 1x1, 1x2, 2x2, 2x3, etc, up to 4x4
      val availableSizes = mutableListOf<Pair<Int, Int>>()
      for ((i, j) in gridByDimensions(maxDimension, maxDimension)) {
        if (i + x > nCols - 1 || j + y > nRows - 1) {
          break
        }
        // this check is not perfect, and results in some visually interesting overlap.
        // however, it is not correct, because it allows things to overlap, which they "should" not
        if (!grid[getIndex(i + x, j + y)]) {
          availableSizes.add(Pair(i + 1, j + 1))
        }
      }

      return availableSizes
    }

    extend {
      val rng = Random(seed)

      val nCols = 32
      val nRows = 32
      val maxDimension = 6
      val xCell = (width / nCols).toDouble()
      val yCell = (height / nRows).toDouble()
      val cornerRadius = hypot(xCell, yCell) * 0.15
      val padding = cornerRadius / 2.0
      val gridSize = nCols * nRows
      // this just tracks if a certain space is "taken"
      val grid = MutableList(gridSize) { false }
      // !! Row-Major !!
      val getIndex = { x: Int, y: Int -> x * nCols + y }

      /**
       * algorithm
       * 1. left to right, top to bottom
       * 2. is space available?
       *    yes:
       *      1. what is the maximum size (1-4) I can make this shape?
       *        1.a. look at every position from current to `current+3` in x and y axes
       *        2.a. when occupied space is found, that indicates the max space
       *             if no occupied space is found, then max is 4
       *      2. choose random size between [1, max]
       *      3. draw shape
       *      4. mark as "occupied" in grid based on size
       *    no:
       *      1. continue
       */
      val svg = drawComposition {
        stroke = ColorRGBa.BLACK
        strokeWeight = 0.450
        fill = null
        clipMode = ClipMode.REVERSE_DIFFERENCE
        for ((x, y) in gridByDimensions(nCols, nRows)) {
          if (grid[getIndex(x, y)]) {
            continue
          }

          val availableSizes = findAvailableSizes(grid, x, y, nCols, nRows, getIndex, maxDimension)
//          val availableSizes = findAvailableSizesWithOverlaps(grid, x, y, nCols, nRows, getIndex, maxDimension)
          val (xDim, yDim) = availableSizes.random(rng)

          // for "square" shapes, 10% chance of being a circle
          if (xDim == yDim && random(0.0, 1.0, rng) < 0.1) {
            val radius = (xDim / 2.0 * xCell) - padding
            circle(
              x * xCell + radius + padding,
              y * yCell + radius + padding,
              radius
            )
          } else {
            val rect = Rectangle(
              x * xCell + padding,
              y * yCell + padding,
              xDim * xCell - padding * 2.0,
              yDim * yCell - padding * 2.0,
            )
            contour(rect.toRounded(cornerRadius).contour)
          }

          // mark cells as "occupied"
          for (i in x until (x + xDim)) {
            for (j in y until (y + yDim)) {
              grid[getIndex(i, j)] = true
            }
          }
        }
      }

      drawer.composition(svg)

      if (screenshots.captureEveryFrame) {
        seed = random(0.0, Int.MAX_VALUE.toDouble()).toInt()
        screenshots.name = "screenshots/$progName/${timestamp()}-seed-$seed.jpg"
        // save design for plotting
        val svgFile = File("screenshots/$progName/${timestamp()}-seed-$seed.svg")
        svgFile.parentFile?.let { file ->
          if (!file.exists()) {
            file.mkdirs()
          }
        }
        svg.saveToFile(svgFile)
      }
    }
  }
}

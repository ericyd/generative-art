import { vec2, randomSeed, createRng, Vector2, random, circle, ColorHex, randomFromArray, rect, hypot, Grid, range } from 'artsvg'
import { renderSvg } from 'artsvg/render'

const config = {
  background: '#fff',
  width: 100,
  height: 100,
  scale: 5,
  loopCount: 1,
}

let seed =  randomSeed()
seed = 241429291638143

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)

  svg.fill = null
  svg.stroke = ColorHex.Black
  svg.strokeWidth = 0.15

  const columnCount = 32
  const rowCount = 32
  const cellSize = svg.width / columnCount
  populateGrid(svg, rng, {
    xStart: 0,
    yStart: 0,
    columnCount,
    rowCount,
    cellSize
  })  

  return () => {
    seed = randomSeed()
  }
})

function populateGrid(svg, rng, { xStart, yStart, columnCount, rowCount, cellSize }) {
  const maxDimension = Math.min(10, columnCount, rowCount)
  const cornerRadius = cellSize * 0.25
  const padding = cornerRadius
  const grid = new Grid({ columnCount, rowCount })
  
  for (const [{ x, y }, isOccupied] of grid) {
    if (isOccupied) {
      continue
    }

    const availableSizes = findAvailableSizes(grid, x, y, maxDimension)
    const dim = randomFromArray(availableSizes, rng)

    // for "square" shapes, 10% chance of being a circle
    if (dim.x == dim.y && dim.x > 1 && random(0, 1, rng) < 0.2) {
      const radius = (dim.x / 2.0 * cellSize) - padding
      svg.circle(circle(
        xStart + x * cellSize + radius + padding,
        yStart + y * cellSize + radius + padding,
        radius
      ))
    } else {
      const r = rect(
        xStart + x * cellSize + padding,
        yStart + y * cellSize + padding,
        dim.x * cellSize - padding * 2.0,
        dim.y * cellSize - padding * 2.0,
      )
      r.borderRadius = cornerRadius
      svg.rect(r)

      // SUB GRID!!!
      if (dim.x > 1 && dim.y > 1 && random(0, 1, rng) < 0.8) {
        populateGrid(svg, rng, {
          xStart: xStart + x * cellSize + cellSize / 2,
          yStart: yStart + y * cellSize + cellSize / 2,
          columnCount: dim.x - 1,
          rowCount: dim.y - 1,
          cellSize,
        })
      }
    }

    // mark cells as "occupied"
    for (let i = x; i < (x + dim.x); i++) {
      for (let j = y; j < (y + dim.y); j++) {
        grid.set(i, j, true)
      }
    }
  }
}

/**
 * This will find all available sizes without any chance of overlaps
 * 
 * Algo:
 * 1. Starting at [0, 0], move diagonally to "maxDimension".
 * 2. At each diagonal point, extend in the x and y axes to the maxDimension.
 *    2.a. A each cell, check if it is occupied
 *      - If a cell is occupied, update the "max" for the given axis
 *      - Else, add the coordinate pair to the list of available sizes
 * 3. Return the list of available sizes
 * 
 * One issue with this algorithm is that point [diagonal, diagonal] gets added twice every iteration.
 * This can be "fixed" easily by adjusting the range of the y iterator, for example, to start after the diagonal space.
 * Future experimenters can see what they prefer.
 * 
 * Despite the nested loops, this should be O(n) time complexity since every cell in the grid is only visited once
 * (with the exception of the diagonals, which is mentioned above).
 * 
 * @param {Grid} grid 
 * @param {Integer} x 
 * @param {Integer} y 
 * @param {Integer} maxDimension 
 * @returns {Vector2[]}
 */
function findAvailableSizes(grid, x, y, maxDimension) {
  // compute the available sizes, e.g. 1x1, 1x2, 2x2, 2x3, etc, up to 4x4
  const availableSizes = []

  // avoid extending beyond the grid
  let maxX = Math.min(grid.columnCount, x + maxDimension) - x
  let maxY = Math.min(grid.rowCount, y + maxDimension) - y
  for (const diagonal of range(0, maxDimension)) {
    // if diagonal is ever greater than or equal to our max X or Y dimension,
    // then we've already found all our valid pairs
    if (diagonal >= maxX || diagonal >= maxY) {
      break
    }
    // extend from "diagonal" to "max" in the x axis
    for (const i of range(diagonal, maxX)) {
      const j = diagonal
      if (!grid.get(i + x, j + y)) {
        availableSizes.push(vec2(i + 1, j + 1))
      } else {
        maxX = Math.min(maxX, i)
        break
      }
    }

    // extend from "diagonal" to "max" in the y axis
    for (const j of range(diagonal, maxY)) {
      const i = diagonal
      if (!grid.get(i + x, j + y)) {
        availableSizes.push(vec2(i + 1, j + 1))
      } else {
        maxY = Math.min(maxY, j)
        break
      }
    }
  }
  return availableSizes
}

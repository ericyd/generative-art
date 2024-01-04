/**
 * Genuary 2024, Day 3
 * https://genuary.art/prompts
 * 
 * """
 * JAN. 3 (credit: Stranger in the Q)
 * 
 * Droste effect.
 * 
 * Wikipedia: https://en.wikipedia.org/wiki/Droste_effect
 * """
 */
import { vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range } from 'artsvg'
import { renderSvg } from 'artsvg/render'

const config = {
  width: 100,
  height: 100,
  scale: 5,
  loopCount: 15,
}

let seed =  randomSeed()
/**
 * There are lots of good seeds here!
 * 6357543149678615 - this is probably the best one, it's cool because the same pattern recurses on different backgrounds
 * 277422208658223 - nice, but kinda boring
 * 3741342450214379 this one is kinda interesting, goes very deep
 * 6099059956284419 - pleasing, multiple recursion locations
 * 3477452704573403 - ha, very maximalist
 */
// seed = 3741342450214379

const colors = [
  'EEA16E',
  'E2799F',
  '9E8FB2',
  'A7ACD9',
  '93C6D6',
  '82AEB1',
  '668586',
]

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }

  svg.fill = null
  svg.stroke = ColorRgb.Black
  svg.strokeWidth = 0.15

  const columnCount = 6
  const rowCount = 6
  populateGrid(svg, seed, {
    xStart: 0,
    width: svg.width,
    yStart: 0,
    columnCount,
    rowCount,
    seed,
    depth: 0
  })

  return () => {
    seed = randomSeed()
  }
})

function populateGrid(svg, seed, { xStart, yStart, columnCount, rowCount, width, depth }) {
  if (depth > 5) return
  const rng = createRng(seed)
  const maxDimension = Math.min(20, columnCount, rowCount)
  const cellSize = width / columnCount
  const cornerRadius = cellSize * 0.25
  const padding = cornerRadius * 0.5
  const grid = new Grid({ columnCount, rowCount })
  
  for (const [{ x, y }, isOccupied] of grid) {
    if (isOccupied) {
      continue
    }

    const availableSizes = findAvailableSizes(grid, x, y, maxDimension)
    const dim = randomFromArray(availableSizes, rng)

    // for "square" shapes, some chance of being a circle
    if (dim.x == dim.y && dim.x > 1 && random(0, 1, rng) < 0.2) {
      const radius = (dim.x / 2.0 * cellSize) - padding
      svg.fill = ColorRgb.fromHex(randomFromArray(colors, rng)).toString()
      svg.circle(circle(
        xStart + x * cellSize + radius + padding,
        yStart + y * cellSize + radius + padding,
        radius
      ))
    } else {
      svg.fill = ColorRgb.fromHex(randomFromArray(colors, rng)).toString()
      const r = rect(
        xStart + x * cellSize + padding,
        yStart + y * cellSize + padding,
        dim.x * cellSize - padding * 2.0,
        dim.y * cellSize - padding * 2.0,
      )
      r.borderRadius = cornerRadius
      svg.rect(r)

      // SUB GRIDS!!!
      if (dim.x > 1 && dim.y > 1) {
        // when element is square, recurse with the same seed, which gives a Droste effect https://en.wikipedia.org/wiki/Droste_effect
        // otherwise, use a new seed if chance wills it to be!
        const opts = {
          xStart: xStart + x * cellSize + cellSize / 4,
          yStart: yStart + y * cellSize + cellSize / 4,
          columnCount,
          rowCount,
          width: dim.x * cellSize - cornerRadius * 2.0,
          depth: depth + 1
        }
        if (dim.x === dim.y) {
          populateGrid(svg, seed, opts)
        } else if (random(0, 1, rng) < 0.8) {
          populateGrid(svg, randomSeed(rng), opts)
        }
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

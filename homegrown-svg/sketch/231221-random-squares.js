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
// seed = 7863031615148757

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)

  const columnCount = 32
  const rowCount = 32
  const maxDimension = 6
  const xCell = svg.width / columnCount
  const yCell = svg.height / rowCount
  const cornerRadius = hypot(xCell, yCell) * 0.15
  const padding = cornerRadius / 2.0
  const grid = new Grid({ columnCount, rowCount })
  svg.fill = null
  svg.stroke = ColorHex.Black
  svg.strokeWidth = 0.15

  for (const [{ x, y }, isOccupied] of grid) {
    console.log(x,y)
    if (isOccupied) {
      continue
    }

    const availableSizes = findAvailableSizes(grid, x, y, maxDimension)
    const dim = randomFromArray(availableSizes, rng)

    // for "square" shapes, 10% chance of being a circle
    if (dim.x == dim.y && random(0.0, 1.0, rng) < 0.1) {
      const radius = (dim.x / 2.0 * xCell) - padding
      svg.circle(circle(
        x * xCell + radius + padding,
        y * yCell + radius + padding,
        radius
      ))
    } else {
      const r = rect(
        x * xCell + padding,
        y * yCell + padding,
        dim.x * xCell - padding * 2.0,
        dim.y * yCell - padding * 2.0,
      )
      r.borderRadius = cornerRadius
      svg.rect(r)
    }

    // mark cells as "occupied"
    for (let i = x; i < (x + dim.x); i++) {
      for (let j = y; j < (y + dim.y); j++) {
        grid.set(i, j, true)
      }
    }
  }

  return () => {
    seed = randomSeed()
  }
})

// This will find all available sizes without any chance of overlaps
/**
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


  // ok let's try a different algorithm
  // rather than going left to right, top to bottom, let's go diagonally, then grow outward from each diagonal.
  // at each diagonal, we can identify if we're above the maxX or maxY and stop at the appropriate place
  let maxX = maxDimension
  let maxY = maxDimension
  for (const diagonal of range(0, maxDimension)) {
    // if diagonal is ever greater than our max X or Y dimension, then we've already found all our valid pairs
    if (diagonal > maxX || diagonal > maxY) {
      break
    }
    for (const i of range(diagonal, maxX)) {
      const j = diagonal
      // outside grid dimensions, continue
      if (i + x > grid.columnCount - 1 || j + y > grid.rowCount - 1) {
        continue
      }
      if (!grid.get(i + x, j + y)) {
        availableSizes.push(vec2(i + 1, j + 1))
      } else {
        // TODO: including the "-1" fixes any overlaps, but it creates a strange artifact where things are predominantly oriented in the grid's `order` direction
        // One hypothesis is that, when the first row is filled, it creates irregularities that are harder to fill later.
        // This almost makes me think that I need to find a quasi-random way to select starting points to fill out this grid.
        // It's almost like there is a selection bias against certain orientations.
        maxX = Math.min(maxX, i - 1)
        break
      }
    }

    // extend from "diagonal" to "max" in the y axis
    for (const j of range(diagonal + 1, maxY)) {
      const i = diagonal
      // outside grid dimensions, continue
      if (i + x > grid.columnCount - 1 || j + y > grid.rowCount - 1) {
        continue
      }
      if (!grid.get(i + x, j + y)) {
        availableSizes.push(vec2(i + 1, j + 1))
      } else {
        maxY = Math.min(maxY, j - 1)
        break
      }
    }
  }
  return availableSizes
}
/**
 * Genuary 2024, Day 23
 * https://genuary.art/prompts
 *
 * """
 * JAN. 23 (credit: Marc Edwards)
 *
 * 8×8.
 * """
 */
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 10,
  loopCount: 1,
}

let seed = randomSeed()
/* good seeds */
seed = 2567865651087179

const colors = [
  '#974F7A',
  '#D093C2',
  '#6F9EB3',
  '#E5AD5A',
  '#EEDA76',
  '#B5CE8D',
]

/*
Rules

1. Every cell of an 8x8 grid must be filled with a circle or a part of a circle
2. A circle may have a radius between 1/2 cell width and 1.5 cell widths
3. Circles must be placed in the center of their cell, or the center of the group of cells they fill
4. Circles will "connect" to an adjacent circle with outer bitangents
    - a "chain" of circles will be created, where the chain can connect orthogonally or diagonally
5. The included shape will be filled with a random color
*/

/** @typedef {{ circle: import('@salamivg/core').Circle, corner: import('@salamivg/core').Vector2 }} GridThing */

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)
  const bg = '#F1ECE2'
  svg.setBackground(bg)
  svg.numericPrecision = 3

  const gridSize = 8
  const maxDimension = 2
  const cellSize = svg.width / gridSize
  const padding = cellSize * 0.125

  /******
    Populate grid with circles
   ******/
  // shuffle the grid so we don't always start at 0,0
  const grid = new Grid({ columnCount: gridSize, rowCount: gridSize })
  for (const [{x,y}] of shuffle(grid, rng)) {
    const availableSizes = findAvailableSizes(grid, x, y, maxDimension)
    if (availableSizes.length === 0) continue
    const dim = availableSizes.at(-1) // always taking the largest one (they are ordered by size, because of how the algorithm runs)

    const baseRadius = (dim.x / 2.0 * cellSize) - padding
    const radius = random(baseRadius * 0.25, baseRadius * 1.25, rng)
    const c = circle(
      cellSize * (x + (dim.x / 2)),
      cellSize * (y + (dim.y / 2)),
      radius
    )

    // mark cells as "occupied"
    // this is useful because then we can just mutate it once and propagate everywhere
    const gridObj = { occupied: true, corner: vec2(x,y), dim, circle: c, paired: false }
    for (let i = x; i < (x + dim.x); i++) {
      for (let j = y; j < (y + dim.y); j++) {
        grid.set(i, j, gridObj)
      }
    }
  }

  /******
    Connect adjacent circles with outer tangents
    find neighbor
    draw bitangents between value.circle and neighbor.circle
    mark both as paired
  ******/
  for (const thing of shuffle(grid, rng)) {
    /** @type {GridThing} */
    const value = thing[1]
    if (value.paired) continue
    
    let current = value
    let neighbors = findValidNeighbors(grid, current)
    while (neighbors.filter(Boolean).length > 0) {
      const neighbor = randomFromArray(neighbors.filter(Boolean), rng)
      // once a circle is paired, it can't be paired again!
      neighbor.paired = true
      current.paired = true

      drawConnectedCircles(current.circle, neighbor.circle, svg, rng)
      current = neighbor
      neighbors = findValidNeighbors(grid, current)
    }
  }

  return () => { seed = randomSeed() }  
})

/**
 * Finds outer bitangents between two circles and draws a path around them.
 * Also draws two circles at the center of each to give some visual interest.
 * @param {import('@salamivg/core').Circle} circleA
 * @param {import('@salamivg/core').Circle} circleB 
 * @param {import('@salamivg/core').Svg} svg 
 * @param {import('@salamivg/core').Rng} rng 
 */
function drawConnectedCircles(circleA, circleB, svg, rng) {
  svg.stroke = '#000'
  svg.strokeWidth = 0.15
  svg.fill = ColorRgb.fromHex(randomFromArray(colors, rng)).toString()
  const [tanA, tanB] = circleB.outerTangents(circleA)
  const [[fromA, fromB], [toA, toB]] = [[tanA[0], tanB[0]], [tanA[1], tanB[1]]]
  const smallRadius = Math.min(circleB.radius, circleA.radius)
  const bigRadius = Math.max(circleB.radius, circleA.radius)
  svg.path(path => {
    path.moveTo(toB)
    path.arc({ rx: bigRadius, ry: bigRadius, end: toA, largeArcFlag: true })
    path.lineTo(fromA)
    path.arc({ rx: smallRadius, ry: smallRadius, end: fromB, largeArcFlag: false })
    path.close()
  })

  svg.fill = ColorRgb.fromHex(randomFromArray(colors, rng)).toString()
  svg.circle({ center: circleA.center, radius: circleA.radius * 0.8 })
  svg.fill = ColorRgb.fromHex(randomFromArray(colors, rng)).toString()
  svg.circle({ center: circleB.center, radius: circleB.radius * 0.8 })
}


/**
 * This will find all available sizes without any chance of overlaps
 * 
 * Algo:
 * 1. Starting at [x, y], move diagonally to "maxDimension".
 * 2. At each diagonal point, extend in the x and y axes to the maxDimension.
 *    - if all cells are empty, then add to available sizes and continue
 *    - else break
 * 3. Return the list of available sizes
 * 
 * (modified from homegrown-svg/sketch/231229-Genuary2024_03.js)
 * 
 * @param {Grid} grid 
 * @param {Integer} x 
 * @param {Integer} y 
 * @param {Integer} maxDimension 
 * @returns {Vector2[]}
 */
function findAvailableSizes(grid, x, y, maxDimension) {
  // compute the available sizes, e.g. 1x1, 2x2, etc, up to maxDimension x maxDimension
  const availableSizes = []

  // avoid extending beyond the grid
  let maxX = Math.min(grid.columnCount, x + maxDimension) - x
  let maxY = Math.min(grid.rowCount, y + maxDimension) - y
  for (const diagonal of range(0, maxDimension)) {
    // if diagonal is greater than or equal to our max X or Y dimension,
    // then we've already found all our valid pairs
    if (diagonal >= maxX || diagonal >= maxY) {
      break
    }
    let available = true // ugly but whatevs
    for (const i of range(0, diagonal + 1)) {
      for (const j of range(0, diagonal + 1)) {
        if (grid.get(i + x, j + y)?.occupied) {
          available = false
          break
        }
      }  
    }

    if (available) {
      availableSizes.push(vec2(diagonal + 1, diagonal + 1))
    }
  }

  return availableSizes
}

function findValidNeighbors(grid, value) {
  /** @type {GridThing[]}*/
  const neighbors = []
    
  // this is going to be pretty ugly actually.
  // top boundary
  for (let i = Math.max(0, value.corner.x - 1); i < Math.min(grid.columnCount, value.corner.x + value.dim.x + 1); i++) {
    const j = value.corner.y - 1
    if (j < 0) break
    const neighbor = grid.get(i, j)
    if (!neighbor?.paired) {
      neighbors.push(neighbor)
    }
  }

  // right boundary (upper right already captured)
  for (let j = value.corner.y; j < Math.min(grid.rowCount, value.corner.y + value.dim.y + 1); j++) {
    const i = value.corner.x + value.dim.x
    if (i >= grid.columnCount) break
    const neighbor = grid.get(i, j)
    if (!neighbor?.paired) {
      neighbors.push(neighbor)
    }
  }

  // bottom boundary (lower right already captured)
  for (let i = Math.max(0, value.corner.x - 1); i < Math.min(grid.columnCount, value.corner.x + value.dim.x); i++) {
    const j = value.corner.y + value.dim.y
    if (j >= grid.rowCount) break
    const neighbor = grid.get(i, j)
    if (!neighbor?.paired) {
      neighbors.push(neighbor)
    }
  }

  // left boundary (lower left and upper left already captured)
  for (let j = value.corner.y; j < Math.min(grid.rowCount, value.corner.y + value.dim.y); j++) {
    const i = value.corner.x - 1
    if (i < 0) break
    const neighbor = grid.get(i, j)
    if (!neighbor?.paired) {
      neighbors.push(neighbor)
    }
  }
  return neighbors
}
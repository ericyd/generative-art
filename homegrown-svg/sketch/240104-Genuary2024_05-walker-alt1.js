/**
 * Genuary 2024, Day 5
 * https://genuary.art/prompts
 * 
 * """
 * In the style of Vera Molnár (1924-2023).
 *
 * Wikipedia: https://en.wikipedia.org/wiki/Vera_Moln%C3%A1r
 * """
 * 
 * Inspired by Vera Molnár, Molndrian, e.g. https://artlogic-res.cloudinary.com/w_1100,c_limit,f_auto,fl_lossy,q_auto:good/ws-ropac/usr/images/artworks/main_image/items/05/05fd6865c7f24ef6878e59503fe61350/vem_1016_300dpi.jpg
 */
import { map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle } from 'artsvg'
import { renderSvg } from 'artsvg/render'

const config = {
  width: 100,
  height: 100,
  scale: 6,
  loopCount: 1,
}

let seed = randomSeed()
seed = 1213658905635049

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)

  svg.fill = null
  svg.stroke = null
  svg.strokeWidth = 0.15

  const center = vec2(svg.width, svg.height).div(2)
  const rowCount = 30
  const columnCount = 30
  const grid = new Grid({ columnCount, rowCount })
  const cellWidth = svg.width / rowCount
  const cellHeight = svg.height / columnCount
  const hueMin = random(0, 180, rng)
  const hueMax = hueMin + 180
  svg.strokeWidth = 1

  /**
   * Rules:
   * 1. random walkers start in each corner.
   * 2. Walkers can move either diagonally or orthogonally.
   * 3. After each move, walkers have an X% chance of splitting in 2.
   * 4. Walkers keep going until they cannot go any further.
   * 5. No cell can be visited twice
   * 6. walkers (slightly) prefer to move in straight lines
   */

  // Rule 1
  const walkers = [
    newWalker(vec2(0, 0)),
    newWalker(vec2(0, rowCount - 1)),
    newWalker(vec2(columnCount - 1, rowCount - 1)),
    newWalker(vec2(columnCount - 1, 0)),
  ]

  // Rule 4
  while (walkers.some(({ canMove }) => canMove)) {
    for (const walker of walkers) {
      if (!walker.canMove) {
        continue
      }
      // Rule 2
      const nextDirectionOptions = [-1, 0, 1].flatMap((x) => [-1, 0, 1].map((y) => vec2(x, y)))
        .filter((vec) => {
          const position = walker.position.add(vec)
          if (position.x < 0 || position.x > rowCount - 1 || position.y < 0 || position.y > columnCount - 1) {
            return false
          }
          if (grid.get(position.x, position.y)) {
            return false
          }
          return true
        })

      if (nextDirectionOptions.length === 0) {
        walker.canMove = false
        continue
      }

      // Rule 6
      const next = random(0, 1, rng) < 0.52 && walker.moves.length > 0
        ? walker.moves[walker.moves.length - 1]
        : randomFromArray(nextDirectionOptions, rng)
      // Rule 3
      if (random(0, 1, rng) < 0.025 && nextDirectionOptions.length > 1) {
        const splitStart = randomFromArray(nextDirectionOptions.filter(vec => !vec.eq(next)), rng)
        walkers.push(newWalker(splitStart))
      }
      walker.position = walker.position.add(next)
      walker.moves.push(next)
      grid.set(walker.position.x, walker.position.y, true)
    }
  }


  // The more efficient route would be to render in place, every time the walker moves.
  // The problem with rendering in place is that we don't know the final length of the walker path so it's hard to map the sat/lum values in the way I want.
  // Looking at 10 random renders, the max length of a walker is 153, so we could use that as a max and simply decrement accordingly.
  // Two problems with that:
  // 1. it isn't scalable; different grid orientations or rules will result in different max lengths.
  // 2. the range of sat/lum will be different for each walker. (not necessarily "wrong" but again not what I want)

  // render via walker paths
  for (const walker of walkers) {
    // not super efficient but we just retrace the same steps. I guess I could just render this immediately when the path is defined...
    walker.position = walker.start
    walker.hue = map(0, hypot(svg.width, svg.height) / 2, hueMin, hueMax, walker.position.distanceTo(center))
    walker.sat = random(0.2, 0.4, rng)
    walker.light = random(0.6, 0.8, rng)
    
    svg.stroke = hsl(walker.hue, walker.sat, walker.light)

    const rect = new Rectangle({ x: walker.position.x * cellWidth, y: walker.position.y * cellHeight, width: cellWidth, height: cellHeight })
    let sides = rect.sides().filter(() => random(0, 1, rng) > 0.5)
    if (sides.length === 0) {
      sides = [randomFromArray(rect.sides(), rng)]
    }
    for (const side of sides) {
      side.setAttributes({ 'stroke-linecap': 'square' })
      svg.lineSegment(side)
    }

    for (let i = 0; i < walker.moves.length; i++) {
      const sat = map(0, walker.moves.length, walker.sat, 0.8, i)
      const light = map(0, walker.moves.length, walker.light, 0.2, i)
      svg.stroke = hsl(walker.hue, sat, light)
      const move = walker.moves[i]
      walker.position = walker.position.add(move)

      const rect = new Rectangle({ x: walker.position.x * cellWidth, y: walker.position.y * cellHeight, width: cellWidth, height: cellHeight })
      let sides = rect.sides().filter(() => random(0, 1, rng) > 0.5)
      if (sides.length === 0) {
        sides = [randomFromArray(rect.sides(), rng)]
      }
      for (const side of sides) {
        side.setAttributes({ 'stroke-linecap': 'square' })
        svg.lineSegment(side)
      }
    }

    // console.log(walker.moves.length)
  }

  return () => {
    seed = randomSeed()
  }
})

function newWalker(position) {
  return {
    position,
    canMove: true,
    start: position,
    moves: []
  }
}

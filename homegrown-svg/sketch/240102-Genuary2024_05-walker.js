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
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 6,
  loopCount: 1,
}

let seed = randomSeed()
seed = 4294291396938055

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

  /**
   * Rules:
   * 1. random walkers start in each corner.
   * 2. Walkers can move either diagonally or orthogonally.
   * 3. After each move, walkers have an X% chance of splitting in 2.
   * 4. Walkers keep going until they cannot go any further.
   * 5. No cell can be visited twice
   */

  // Rule 1
  const walkers = [
    {
      position: vec2(0, 0),
      canMove: true
    },
    {
      position: vec2(0, rowCount - 1),
      canMove: true
    },
    {
      position: vec2(columnCount - 1, rowCount - 1),
      canMove: true
    },
    {
      position: vec2(columnCount - 1, 0),
      canMove: true
    },
  ]

  // Rule 4
  while (walkers.some(({ canMove }) => canMove)) {
    for (const walker of walkers) {
      if (!walker.canMove) {
        continue
      }
      // Rule 2
      const nextPositionOptions = [-1, 0, 1].flatMap((relativeX) =>
        [-1, 0, 1].map((relativeY) =>
          walker.position.add(vec2(relativeX, relativeY))
        )
      ).filter((vec) => {
        if (vec.x < 0 || vec.x > rowCount - 1 || vec.y < 0 || vec.y > columnCount - 1) {
          return false
        }
        if (grid.get(vec.x, vec.y)) {
          return false
        }
        return true
      })

      if (nextPositionOptions.length === 0) {
        walker.canMove = false
        continue
      }

      const next = randomFromArray(nextPositionOptions, rng)
      // Rule 3
      if (random(0, 1, rng) < 0.025 && nextPositionOptions.length > 1) {
        const splitStart = randomFromArray(nextPositionOptions.filter(vec => !vec.eq(next)), rng)
        walkers.push({ position: splitStart, canMove: true })
      }
      walker.position = next
      grid.set(next.x, next.y, true)
    }
  }

  const cellWidth = svg.width / rowCount
  const cellHeight = svg.height / columnCount
  const hueMin = random(0, 180, rng)
  const hueMax = hueMin + 180
  for (const [vec, filled] of grid) {
    if (!filled) {
      continue
    }
    const pos = vec2(vec.x * cellWidth, vec.y * cellHeight)
    const hue = map(0, hypot(svg.width, svg.height) / 2, hueMin, hueMax, pos.distanceTo(center))
    const sat = random(0.4, 0.6, rng)
    const light = random(0.4, 0.6, rng)
    svg.stroke = hsl(hue, sat, light)
    svg.strokeWidth = 1
    const rect = new Rectangle({ x: pos.x, y: pos.y, width: cellWidth, height: cellHeight })

    let sides = rect.sides().filter(() => random(0, 1, rng) > 0.5)
    if (sides.length === 0) {
      sides = [randomFromArray(rect.sides(), rng)]
    }
    for (const side of sides) {
      side.setAttributes({ 'stroke-linecap': 'square' })
      svg.lineSegment(side)
    }
  }

  return () => {
    seed = randomSeed()
  }
})

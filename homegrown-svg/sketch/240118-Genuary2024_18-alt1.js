/**
 * Genuary 2024, Day 18
 * https://genuary.art/prompts
 *
 * """
 * JAN. 18 (credit: Chris Barber)
 *
 * Bauhaus.
 * """
 */
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 10,
  loopCount: 10,
}

let seed = 7118028728691427 // randomSeed()

const colors = [
  '#72AAD2',
  '#D56666',
  '#F6D851',
  '#E77D31',
  '#4C5E9E',
  '#000000',
]

const orientations = {
  NScW: {
    name: 'north-south concave west',
    rotation: PI/2,
  },
  NScE: {
    name: 'north-south concave east',
    rotation: PI*3/2,
  },
  EWcN: {
    name: 'east-west concave north',
    rotation: PI,
  },
  EWcS: {
    name: 'east-west concave south',
    rotation: 0,
  }
}

/**
 * Rules
 * 1. 8x8 grid
 * 2. each grid gets 2 random semi-circles
 */

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)
  const bg = '#F6E2BA'
  svg.setBackground(bg) // #eda is also nice
  svg.numericPrecision = 3

  const gridSize = 8
  for (const [position] of new Grid({ columnCount: gridSize, rowCount: gridSize })) {
    const center = position.multiply(svg.width / gridSize).add(vec2(svg.width / gridSize / 2, svg.height / gridSize / 2))
    const orientation = randomFromObject(orientations, rng)
    const radius = svg.width / gridSize / 2
    const shape = semiCircle(center, orientation, radius, rng)
    svg.path(shape.path)
  }

  return () => { seed = randomSeed() }  
})

function semiCircle(center, orientation, radius, rng) {
  const color = randomFromArray(colors, rng)
  const stripeSize = radius / 3
  const filled = random(0, 1, rng) < 0.7
  const vector = vec2(cos(orientation.rotation), sin(orientation.rotation))
  const start = center.subtract(vector.multiply(radius))
  const end = center.add(vector.multiply(radius))
  const strokeWidth = 1
  const p = path(p => {
    p.fill = filled ? color : null
    p.stroke = filled ? null : color
    p.strokeWidth = strokeWidth

    if (filled) {
      p.moveTo(start)
      p.arc({ rx: radius, ry: radius, end })
      p.close()
    } else {
      for (let r = stripeSize - strokeWidth / 2; r < (radius); r += stripeSize) {
        p.moveTo(center.subtract(vector.multiply(r)))
        p.arc({ rx: r, ry: r, end: center.add(vector.multiply(r)) })
      }
    }
  })
  return {
    path: p,
    start,
    end,
    radius,
    orientation
  }
}
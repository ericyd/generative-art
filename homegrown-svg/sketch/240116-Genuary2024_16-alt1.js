/**
 * Genuary 2024, Day 16
 * https://genuary.art/prompts
 *
 * """
 * JAN. 16 (credit: Bruce Holmer & Michael Lowe)
 *
 * Draw 10 000 of something.
 * """
 * 
 * This is a riff on 240113-Genuary2024_13.js, but combining the curl and normal noise function
 */
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, Circle } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 10,
  loopCount: 1,
}

let seed = randomSeed()
seed = 6536093506434323

renderSvg(config, (svg) => {
  const cleanup = () => { seed = randomSeed() }
  const rng = createRng(seed)
  const grad = svg.defineLinearGradient({
    colors: [
      '#2E6479',
      '#2D3F74',
    ],
  })
  svg.setBackground(grad)

  svg.numericPrecision = 3
  const angleMin = random(-PI, PI, rng)
  const noiseFn = createOscCurl(seed)
  const noiseFn2 = createOscNoise(seed)

  const spectrum = ColorSequence.fromHexes(shuffle([
    '#E8BD67',
    '#D9713D',
    '#CBCFE8',
    '#CB6464',
  ], rng))

  svg.fill = null
  svg.stroke = ColorRgb.Black
  svg.strokeWidth = 0.2

  const nPoints = 1000
  // this is clumsy but works for now
  const points = new Array(nPoints).fill(0)
    .map(() => Vector2.random(svg.width * -0.01, svg.width * 1.01, svg.height * -0.01, svg.height * 1.01, rng))

  const scale = random(0.02, 0.04, rng)
  const visited = []
  const noises = []
  const padding = 0.85
 
  for (const startPoint of points) {
    let cursor = startPoint.jitter(0.3, rng)
    const color = spectrum.at(random(0, 1, rng))

    for (let i = 0; i < 100; i++) {
      const angle = map(-1, 1, angleMin, angleMin + TAU, noiseFn2(cursor.x * scale, cursor.y * scale))
      const vec = noiseFn(cursor.x * scale, cursor.y * scale).add(vec2(cos(angle), sin(angle)))
      // TODO: would be nice if Circle could accept a "center" argument and automatically take x/y from that
      const nextCenter = cursor.add(vec)
      const fill = random(0, 1, rng) < 0.5 ? color : null
      // noise radius
      // const radius = map(-1, 1, padding * 0.2, padding * 1.5, noiseFn2(cursor.x * scale * 2, cursor.y * scale * 2))
      // "random" radius. This is actually nicer
      const radius = random(0, 1, rng) < 0.1 ? random(padding * 0.8, padding * 2, rng) : random(padding * 0.2, padding * 0.5, rng)
      const next = new Circle({ x: nextCenter.x, y: nextCenter.y, radius, fill, stroke: color })
      if (!nearAnyPoint(next, visited, padding / 2.5)) {
        noises.push(noiseFn2(cursor.x * scale, cursor.y * scale))
        svg.circle(next)
        // technically.... this might count points that are outside of the canvas. But if we draw 10k inside the canvas it's a little too busy IMO
        visited.push(next)
      }
      cursor = next.center
      if (visited.length === 10000) {
        svg.filenameMetadata = { seed, 'visted': visited.length }
        console.log({minNoise: Math.min(...noises), maxNoise: Math.max(...noises)})
        return cleanup
      }
    }
  }

  console.log({minNoise: Math.min(...noises), maxNoise: Math.max(...noises)})
  svg.filenameMetadata = { seed, 'visted': visited.length }
  return cleanup
})

/**
 * 
 * @param {Circle} circle 
 * @param {Circle[]} others 
 * @param {*} padding 
 * @returns
 */
function nearAnyPoint(circle, others, padding) {
  // hypothesis: points are more likely to be near the most recently-placed points than near the oldest points
  for (let i = others.length - 1; i >= 0; i--) {
    // intersectsCircle(circle) is a little too strict
    if (circle.intersectsCircle(others[i], padding)) {
      return true
    }
  }
  return false
}

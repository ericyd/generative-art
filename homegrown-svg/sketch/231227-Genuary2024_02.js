/**
 * Genuary 2024, Day 2
 * https://genuary.art/prompts
 * JAN. 2 (credit: Luis Fraguada)
 * 
 * No palettes.
 * 
 * Generative colors, procedural colors, emergent colors.
 */

import { vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, Oscillator, ColorHsl, hsl, path, FractalizedLine, cos, sin } from 'artsvg'
import { renderSvg } from 'artsvg/render'

const config = {
  background: '#fff',
  width: 100,
  height: 100,
  scale: 5,
  loopCount: 1,
}

let seed =  randomSeed()

renderSvg(config, (svg) => {
  const rng = createRng(seed)
  svg.filenameMetadata = { seed }

  svg.fill = null
  svg.strokeWidth = 0.15
  const columnCount = 3
  const rowCount = 3

  for (const [gridPoint] of new Grid({ columnCount, rowCount })) {
    // center each point in a grid of the canvas dimensions
    const x = gridPoint.x * svg.width / columnCount + svg.width / (columnCount * 2)
    const y = gridPoint.y * svg.height / rowCount + svg.height / (rowCount * 2)

    const baseHue = random(0, 310, rng)
    const baseSaturation = random(0.25, 0.75, rng)
    const baseLightness = random(0.35, 0.95, rng)

    for (let radius = 1; radius < 20; radius += 1) {
      const p = path(p => {
        const center = vec2(x, y)
        p.moveTo(center.add(vec2(cos(0), sin(0)).multiply(radius)))
        for (let angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
          p.lineTo(center.add(vec2(cos(angle), sin(angle)).multiply(radius)))
        }
      })
      const line = new FractalizedLine(p.points, rng)
      svg.stroke = hsl(random(baseHue, baseHue + 50, rng), random(baseSaturation, baseSaturation + 0.25, rng), random(baseLightness, baseLightness - 0.25, rng))
      svg.path(line.perpendicularSubdivide(10, 0.25).path())
    }
  }
  
  return () => {
    seed = randomSeed()
  }
})

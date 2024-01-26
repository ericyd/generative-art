/**
 * Genuary 2024, Day 26
 * https://genuary.art/prompts
 *
 * """
 * JAN. 26 (credit: Monokai)
 *
 * Grow a seed.
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
// these are my favs
seed = 7597300052541357
seed = 7196228887045077

const colors = {
  dark: '#2E4163',
  light: '#DAE7E8',
  medium: '#B8C3CB',
}

const spectrum = ColorSequence.fromColors(Object.values(colors))

/*
Rules

1. Draw two smooth bezier curves chained together
2. Split sometimes
3. Each split gets the same recursive treatment
*/

renderSvg(config, (svg) => {
  const rng = createRng(seed)
  const maxDepth = randomInt(4, 6, rng)
  svg.filenameMetadata = { seed, maxDepth }
  svg.setBackground(colors.dark)
  svg.numericPrecision = 3
  svg.fill = null
  svg.stroke = null
  svg.strokeWidth = 0.25

  const points = []
  const padding = 0.5

  function drawPath(angle, entry, scale, curve = 1, depth = 0) {
    if (depth > maxDepth) return

    const curveScale = scale * 0.5
    const newAngle = random(PI * 0.4, PI * 0.6, rng)
    const radius = scale / 10

    svg.path(it => {
      it.stroke = colors.light
      let start = entry
      it.moveTo(start)
      let end = start.add(Vector2.fromAngle(angle).scale(scale))
      let mid = Vector2.midpoint(start, end)
      let ctrl = mid.add(Vector2.fromAngle(angle + (PI / 2 * curve)).scale(curveScale))
      // attempt to prevent overlaps. It isn't perfect ¯\_(ツ)_/¯
      if ([ctrl, end].some(p => points.some(p2 => p.distanceTo(p2) < padding))) {
        return
      }
      points.push(start, ctrl, end)
      if (svg.contains(start) && svg.contains(end)) {
        it.quadraticBezier(ctrl, end)
        // draw the "fruit" of the tree
        svg.circle({center:start, radius, fill: spectrum.at(random(0,1,rng))})
        svg.circle({center:end, radius, fill: spectrum.at(random(0,1,rng))})
        svg.circle({center:ctrl, radius, fill: spectrum.at(random(0,1,rng))})
      }
  

      // recurse twice from midpoint
      drawPath(angle - newAngle, end, scale * random(0.3, 0.95, rng), curve, depth + 1)
  
      start = end
      end = start.add(Vector2.fromAngle(angle).scale(scale))
      mid = Vector2.midpoint(start, end)
      ctrl = mid.add(Vector2.fromAngle(angle - (PI / 2 * curve)).scale(curveScale))
      // attempt to prevent overlaps. It isn't perfect ¯\_(ツ)_/¯
      if ([ctrl, end].some(p => points.some(p2 => p.distanceTo(p2) < padding))) {
        return
      }
      points.push(start, ctrl, end)
      if (svg.contains(start) && svg.contains(end)) {
        it.quadraticBezier(ctrl, end)
        // draw the "fruit" of the tree
        svg.circle({center:start, radius, fill: spectrum.at(random(0,1,rng))})
        svg.circle({center:end, radius, fill: spectrum.at(random(0,1,rng))})
        svg.circle({center:ctrl, radius, fill: spectrum.at(random(0,1,rng))})
      }

      // recurse twice from end
      drawPath(angle, end, scale * random(0.3, 0.95, rng), curve, depth + 1)
      drawPath(angle + newAngle, end, scale * random(0.3, 0.95, rng),  curve * -1, depth + 1)
    })
  }

  const angle = random(0, TAU, rng)

  drawPath(angle, svg.center.add(Vector2.fromAngle(angle - PI).scale(40)), 20)

  return () => { seed = randomSeed() }  
})

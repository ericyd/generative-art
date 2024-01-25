/**
 * Genuary 2024, Day 24
 * https://genuary.art/prompts
 *
 * """
 * JAN. 24 (credit: Jorge Ledezma)
 *
 * Impossible objects (undecided geometry).
 * """
 */
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 10,
  loopCount: 1,
}

renderSvg(config, (svg) => {
  svg.numericPrecision = 3
  svg.fill = null
  svg.stroke = ColorRgb.Black
  
  let yScale = 15
  let y = yScale
  svg.path(path => {
    path.moveTo(vec2(10, y))
    path.cubicBezier(vec2(30, y - yScale), vec2(50, y + yScale), vec2(70, y))
  })

  y += yScale
  svg.path(path => {
    path.moveTo(vec2(10, y))
    path.smoothBezier(vec2(30, y - yScale), vec2(70, y))
  })

  y += yScale
  svg.path(path => {
    path.moveTo(vec2(10, y))
    path.cubicBezier(vec2(20, y - yScale), vec2(40, y + yScale), vec2(50, y))
    path.smoothBezier(vec2(60, y - yScale), vec2(70, y))
  })

  y += yScale
  svg.path(path => {
    path.moveTo(vec2(10, y))
    path.quadraticBezier(vec2(20, y - yScale), vec2(40, y))
    path.smoothQuadraticBezier(vec2(70, y))
  })

  y += yScale
  svg.path(path => {
    path.moveTo(vec2(10, y))
    path.quadraticBezier(vec2(35, y + yScale), vec2(70, y))
  })
})

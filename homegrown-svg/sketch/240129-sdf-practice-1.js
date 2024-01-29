/**
 * Genuary 2024, Day 29
 * https://genuary.art/prompts
 *
 * """
 * JAN. 29 (credit: Melissa Wiederrecht & Camille Roux)
 * 
 * Signed Distance Functions (if we keep trying once per year, eventually we will be good at it!).
 * """
 *
 */
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 10,
  loopCount: 1,
}

let seed = randomSeed()

const bg = ColorRgb.White

renderSvg(config, (svg) => {
  const rng = createRng(seed)
  svg.filenameMetadata = { seed }
  svg.setBackground(bg)
  svg.numericPrecision = 3
  svg.fill = null
  svg.stroke = null
  svg.strokeWidth = 0.25

  for (let i = 0; i < 10000; i++) {
    const point = Vector2.random(0, svg.width, 0, svg.height, rng)
    const dist = sdfCircleLineIntersection(point)
    const lineWidth = 2
    const color = Math.abs(dist) < lineWidth / 2 ? hsl(0, 0, 0.8) : dist < -(lineWidth/2) ? hsl(0, 0.8, map(-(lineWidth/2), -50, 1, 0, dist)) : hsl(220, 0.8, map((lineWidth/2), 50, 1, 0, dist))
    svg.circle({ center: point, radius: 0.5, fill: color })
  }

  return () => { seed = randomSeed() }  
})

function sdfCircleLineIntersection(point) {
  const circle = sdfCircle(point)
  const lineX = sdfLine(point.y - config.height / 2)
  return Math.min(circle, lineX)
}

function sdfRoundedCircle(point) {
  // creates a horizontal line
  let lineX = sdfLine(point.y - config.height / 2)
  // creates a mirrored horizontal line
  lineX = Math.abs(lineX)- 25
  // creates a vert line
  let lineY = sdfLine(point.x - config.width / 2)
  // mirrored vert line
  lineY = Math.abs(lineY)- 25
  // creates an "edge" of the box
  const e = edge(vec2(lineX, lineY))
  // "inflates" the box by 10, but with a rounded corner
  return e - 10
}

function sdfCircle(point, center = vec2(config.width / 2, config.height / 2), radius = 20) {
  return point.subtract(center).length() - radius
}

function sdfLine(t) {
  return t
}

function edge(point) {
  return point.x > 0 && point.y > 0 ? point.length() : point.x > point.y ? point.x : point.y
}
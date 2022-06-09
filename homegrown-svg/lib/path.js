// utilities for svg path tags
// https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/d#path_commands
// 
// MoveTo: M, m
// LineTo: L, l, H, h, V, v
// Cubic Bézier Curve: C, c, S, s
// Quadratic Bézier Curve: Q, q, T, t
// Elliptical Arc Curve: A, a
// ClosePath: Z, z

// Note: Commands are case-sensitive. 
// An upper-case command specifies absolute coordinates, 
// while a lower-case command specifies coordinates relative to the current position.

// It is always possible to specify a negative value as an argument to a command:
//   negative angles will be anti-clockwise;
//   absolute negative x and y values are interpreted as negative coordinates;
//   relative negative x values move to the left, and relative negative y values move upwards.

import { random } from './util.js'

/**
 * C
 * (x1,y1, x2,y2, x,y)
 * Draw a cubic Bézier curve from the current point to the end point specified by x,y.
 * The start control point is specified by x1,y1 and the end control point is specified by x2,y2
 * 
 * S
 * Draw a smooth cubic Bézier curve from the current point to the end point specified by x,y.
 * The end control point is specified by x2,y2.
 * The start control point is a reflection of the end control point of the previous curve command
 */
function cubicBezierPoint() {

}

function smoothBezierPoint(endPoint, controlPoint) {
  return [
    'S',
    controlPoint.x,
    controlPoint.y,
    endPoint.x,
    endPoint.y
  ].join(' ')
}

function point(x, y) {
  return { x, y }
}

function randomPoint(xMin, xMax, yMin, yMax) {
  return point(
    random(xMin, xMax),
    random(yMin, yMax)
  )
}

function moveTo(x, y) {
  return [
    'M',
    x,
    y
  ].join(' ')
}

export function d() {
  const start = randomPoint(0, 100, 0, 100)
  return [
    moveTo(start.x, start.y),
    smoothBezierPoint(randomPoint(20, 80, 20, 80), randomPoint(20, 80, 20, 80)),
    smoothBezierPoint(randomPoint(20, 80, 20, 80), randomPoint(20, 80, 20, 80)),
    smoothBezierPoint(randomPoint(20, 80, 20, 80), randomPoint(20, 80, 20, 80)),
    smoothBezierPoint(randomPoint(20, 80, 20, 80), randomPoint(20, 80, 20, 80)),
    smoothBezierPoint(randomPoint(20, 80, 20, 80), randomPoint(20, 80, 20, 80)),
    smoothBezierPoint(randomPoint(20, 80, 20, 80), randomPoint(20, 80, 20, 80)),
    smoothBezierPoint(randomPoint(20, 80, 20, 80), randomPoint(20, 80, 20, 80)),
  ].join(' ')
}

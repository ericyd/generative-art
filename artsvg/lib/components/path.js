// utilities for svg path tags
// https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/d#path_commands
//
// MoveTo: M, m
// LineTo: L, l, H, h, V, v
// Cubic Bézier Curve: C, c, S, s
// Quadratic Bézier Curve: Q, q, T, t
// Elliptical Arc Curve: A, a
// ClosePath: Z, z
//
// Note: Commands are case-sensitive.
// An upper-case command specifies absolute coordinates,
// while a lower-case command specifies coordinates relative to the current position.
//
// It is always possible to specify a negative value as an argument to a command:
//   negative angles will be anti-clockwise;
//   absolute negative x and y values are interpreted as negative coordinates;
//   relative negative x values move to the left, and relative negative y values move upwards.

import { Tag } from './tag.js'
import { vec2, Vector2 } from '../vector2.js'

/**
 * @typedef {object} PathAttributes
 * @property {number} x
 * @property {number} y
 * @property {number} radius
 */

/**
 * @class Path
 * @property {PathInstruction[]} instructions
 */
export class Path extends Tag {
  #d = []
  /**
   * @param {PathAttributes} attributes
   */
  constructor({ ...attributes } = {}) {
    super('path', {
      ...attributes,
    })
    this.cursor = vec2(0, 0)
  }

  /** @param {'none' | string | null} value */
  set fill(value) {
    const fill = value === null ? 'none' : value
    this.setAttributes({ fill })
  }

  /** @param {'none' | string | null} value */
  set stroke(value) {
    const stroke = value === null ? 'none' : value
    this.setAttributes({ stroke })
  }

  /** @param {number} value */
  set strokeWidth(value) {
    this.setAttributes({ 'stroke-width': value })
  }

  /**
   *
   * @param {Point} endPoint
   * @param {CoordinateType} coordinateType
   * @returns {PathInstruction}
   */
  moveTo(endPoint, coordinateType = 'absolute') {
    this.#d.push(
      new PathInstruction(coordinateType === 'absolute' ? 'M' : 'm', [
        endPoint,
      ]),
    )
    this.cursor = endPoint
  }

  /**
   *
   * @param {Point} endPoint
   * @param {CoordinateType} coordinateType
   * @returns {PathInstruction}
   */
  lineTo(endPoint, coordinateType = 'absolute') {
    this.#d.push(
      new PathInstruction(coordinateType === 'absolute' ? 'L' : 'l', [
        endPoint,
      ]),
    )
    this.cursor = endPoint
  }

  /**
   * * C
   * (x1,y1, x2,y2, x,y)
   * Draw a cubic Bézier curve from the current point to the end point specified by x,y.
   * The start control point is specified by x1,y1 and the end control point is specified by x2,y2
   * @param {Point} controlPoint1
   * @param {Point} controlPoint2
   * @param {Point} endPoint
   * @param {CoordinateType} coordinateType
   * @returns {PathInstruction}
   */
  cubicBezier(
    controlPoint1,
    controlPoint2,
    endPoint,
    coordinateType = 'absolute',
  ) {
    this.#d.push(
      new PathInstruction(coordinateType === 'absolute' ? 'C' : 'c', [
        controlPoint1,
        controlPoint2,
        endPoint,
      ]),
    )
    this.cursor = endPoint
  }

  /**
   * S
   * Draw a smooth cubic Bézier curve from the current point to the end point specified by x,y.
   * The end control point is specified by x2,y2.
   * The start control point is a reflection of the end control point of the previous curve command
   * @param {Point} controlPoint
   * @param {Point} endPoint
   * @param {'absolute' | 'relative'} coordinateType
   * @returns {PathInstruction}
   */
  smoothBezier(controlPoint, endPoint, coordinateType = 'absolute') {
    this.#d.push(
      new PathInstruction(coordinateType === 'absolute' ? 'S' : 's', [
        controlPoint,
        endPoint,
      ]),
    )
    this.cursor = endPoint
  }

  // TODO
  // Quadratic Bézier Curve: Q, q, T, t
  // Elliptical Arc Curve: A, a

  close() {
    this.#d.push(new PathInstruction('Z', []))
    this.cursor = this.#d[0].endPoint
  }

  render() {
    this.setAttributes({ d: this.#d.map((p) => p.render()).join(' ') })
    return super.render()
  }
}

/**
 * @param {PathAttributes | (Path: Path) => void} attrsOrBuilder
 * @param {PathAttributes?} attributes
 */
export function path(attrsOrBuilder, attributes = {}) {
  if (typeof attrsOrBuilder === 'function') {
    const c = new Path(attributes)
    attrsOrBuilder(c)
    return c
  }
  if (typeof attrsOrBuilder === 'object') {
    return new Path(attrsOrBuilder)
  }
  throw new Error(`Unable to construct Path from "${attrsOrBuilder}"`)
}

//------------------------------------------------------------------------------

/**
 * @typedef CoordinateType
 * @type {'absolute' | 'relative'}
 */

/**
 * @typedef PathBuilder
 * @type {object}
 * @property {() => Point} cursor get the current point
 * @property {(endPoint: Point, coordinateType: CoordinateType) => void} move lift up pen and move to point
 * @property {() => void} close close the path back to the start point
 * @property {(endPoint: Point, coodinateType: CoordinateType) => void} line straight line to point
 * @property {(controlPoint1: Point, controlPoint2: Point, endPoint: Point, coordinateType: CoordinateType) => void} cubicBezier
 * @property {(controlPoint: Point, endPoint: Point, coordinateType: CoordinateType) => void} smoothBezier
 */

/**
 * @typedef Radians
 * @type {number}
 */

class PathInstruction {
  /**
   *
   * @param {'l' | 'L' | 'm' | 'M' | 'c' | 'C' | 's' | 'S'} commandType
   * @param {Vector2[]} points
   */
  constructor(commandType, points) {
    this.endPoint = points?.[0] ?? vec2(0, 0)
    this.points = points
    this.commandType = commandType
  }

  render() {
    return [
      this.commandType,
      ...this.points.map((pt) => [pt.x, pt.y].join(' ')),
    ].join(' ')
  }
}

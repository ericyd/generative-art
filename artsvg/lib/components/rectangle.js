import { Vector2, vec2 } from '../vector2.js'
import { LineSegment } from './line-segment.js'
import { Tag } from './tag.js'

/**
 * @typedef {object} RectangleAttributes
 * @property {number} [x=0]
 * @property {number} [y=0]
 * @property {number} [width=1]
 * @property {number} [height=1]
 * @property {number} [borderRadius] if provided, sets the `rx` property of the SVG rect tag.
 *    Takes precedence over `rx` property if both are passed in the constructor
 * @property {number} [rx] border radius in the x direction
 * @property {number} [ry] border radius in the y direction. Defaults to rx if not provided.
 */

/**
 * @class Rectangle
 * @example
 *   const r = rect(r => {
 *     r.fill = '#000'
 *     r.stroke = '#055'
 *     r.x = 1
 *     r.y = 10
 *     r.width = 100
 *     r.height = 15
 *     r.borderRadius = 1.4
 *   })
 * @example
 *   const r = rect({ x: 1, y: 10, width: 100, height: 15, borderRadius: 1.4 })
 */
export class Rectangle extends Tag {
  /**
   * @param {RectangleAttributes} [attributes={}]
   */
  constructor({ x = 0, y = 0, width = 1, height = 1, ...attributes } = {}) {
    super('rect', {
      x,
      y,
      width,
      height,
      rx: attributes.borderRadius ?? attributes.rx,
      ...attributes,
    })
  }

  /**
   * @param {number} value
   */
  set x(value) {
    this.setAttributes({ x: value })
  }
  /**
   * @returns {number}
   */
  get x() {
    // @ts-expect-error TODO: why?
    return this.attributes.x
  }

  /**
   * @param {number} value
   */
  set y(value) {
    this.setAttributes({ y: value })
  }
  /**
   * @returns {number}
   */
  get y() {
    // @ts-expect-error TODO: why?
    return this.attributes.y
  }

  /**
   * @param {number} value
   */
  set width(value) {
    this.setAttributes({ width: value })
  }
  /**
   * @returns {number}
   */
  get width() {
    // @ts-expect-error TODO: why?
    return this.attributes.width
  }

  /**
   * @param {number} value
   */
  set height(value) {
    this.setAttributes({ height: value })
  }
  /**
   * @returns {number}
   */
  get height() {
    // @ts-expect-error TODO: why?
    return this.attributes.height
  }

  /**
   * @param {number} value
   */
  set borderRadius(value) {
    this.setAttributes({ rx: value })
  }

  /**
   * @returns {Vector2}
   */
  center() {
    return vec2(this.x + this.width / 2, this.y + this.height / 2)
  }

  /**
   * @returns {Vector2} the upper left corner of the rectangle
   */
  corner() {
    return vec2(this.x, this.y)
  }

  /**
   * @returns {Array<Vector2>} List of all vertices of the rectangle
   */
  vertices() {
    return [
      vec2(this.x, this.y),
      vec2(this.x, this.y + this.height),
      vec2(this.x + this.width, this.y + this.height),
      vec2(this.x + this.width, this.y),
    ]
  }

  /**
   * @returns {Array<LineSegment>} List of all sides of the rectangle as LineSegments
   */
  sides() {
    return [
      new LineSegment(vec2(this.x, this.y), vec2(this.x, this.y + this.height)),
      new LineSegment(vec2(this.x, this.y + this.height), vec2(this.x + this.width, this.y + this.height)),
      new LineSegment(vec2(this.x + this.width, this.y + this.height), vec2(this.x + this.width, this.y)),
      new LineSegment(vec2(this.x + this.width, this.y), vec2(this.x, this.y)),
    ]
  }
}

/**
 * @overload
 * @param {RectangleAttributes} attrsOrBuilderOrX
 * @returns {Rectangle}
 */
/**
 * @overload
 * @param {number} attrsOrBuilderOrX
 * @param {number} y
 * @param {number} width
 * @param {number} height
 * @returns {Rectangle}
 */
/**
 * @overload
 * @param {(rect: Rectangle) => void} attrsOrBuilderOrX
 * @returns {Rectangle}
 */
/**
 * @param {RectangleAttributes | number | ((rect: Rectangle) => void)} attrsOrBuilderOrX
 * @param {number} [y]
 * @param {number} [width]
 * @param {number} [height]
 */
export function rect(attrsOrBuilderOrX, y, width, height) {
  if (typeof attrsOrBuilderOrX === 'function') {
    const c = new Rectangle()
    attrsOrBuilderOrX(c)
    return c
  }
  if (typeof attrsOrBuilderOrX === 'object') {
    return new Rectangle(attrsOrBuilderOrX)
  }
  if (
    typeof attrsOrBuilderOrX === 'number' &&
    (typeof y === 'number' || y === undefined) &&
    (typeof width === 'number' || width === undefined) &&
    (typeof height === 'number' || height === undefined)
  ) {
    return new Rectangle({ x: attrsOrBuilderOrX, y, width, height })
  }
  throw new Error(
    `Unable to construct circle from "${attrsOrBuilderOrX}, ${y}, ${width}, ${height}"`,
  )
}

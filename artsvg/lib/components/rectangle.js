import { Vector2, vec2 } from '../vector2.js'
import { Tag } from './tag.js'

/**
 * @typedef {object} RectangleAttributes
 * @property {number} [x=0]
 * @property {number} [y=0]
 * @property {number} [width=1]
 * @property {number} [height=1]
 */

/**
 * @class Rectangle
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

import { Tag } from './tag.js'

/**
 * @typedef {object} CircleAttributes
 * @property {number} x
 * @property {number} y
 * @property {number} radius
 */

/**
 * @class Circle
 * @property {number} x
 * @property {number} y
 * @property {number} radius
 */
export class Circle extends Tag {
  /**
   * @param {CircleAttributes} attributes
   */
  constructor({ x = 0, y = 0, radius = 1, ...attributes } = {}) {
    super('circle', {
      cx: attributes.x,
      cy: attributes.y,
      r: attributes.radius,
      ...attributes,
    })
    this.x = attributes.x
    this.y = attributes.y
    this.radius = attributes.radius
  }

  /**
   * @param {number} value
   */
  set x(value) {
    this.setAttributes({ cx: value })
    return value
  }

  /**
   * @param {number} value
   */
  set y(value) {
    this.setAttributes({ cy: value })
    return value
  }

  /**
   * @param {number} value
   */
  set radius(value) {
    this.setAttributes({ r: value })
    return value
  }
}

/**
 * @param {CircleAttributes | x: number | (circle: Circle) => void} attrsOrBuilderOrX
 * @param {number?} y
 * @param {number?} radius
 */
export function circle(attrsOrBuilderOrX, y, radius) {
  if (typeof attrsOrBuilderOrX === 'function') {
    const c = new Circle()
    attrsOrBuilderOrX(c)
    return c
  } else if (typeof attrsOrBuilderOrX === 'object') {
    return new Circle(attrsOrBuilderOrX)
  } else if (
    typeof attrsOrBuilderOrX === 'number' &&
    typeof y === 'number' &&
    typeof radius === 'number'
  ) {
    return new Circle({ x: attrsOrBuilderOrX, y, radius })
  } else {
    throw new Error(
      `Unable to construct circle from "${attrsOrBuilderOrX}, ${y}, ${radius}"`,
    )
  }
}

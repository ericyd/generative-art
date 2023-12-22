import { Vector2, vec2 } from '../vector2.js'
import { Tag } from './tag.js'

/**
 * @typedef {object} CircleAttributes
 * @property {number} [x=0]
 * @property {number} [y=0]
 * @property {number} [radius=1]
 */

/**
 * @class Circle
 * @property {number} x
 * @property {number} y
 * @property {number} radius
 */
export class Circle extends Tag {
  #x = 0
  #y = 0
  #radius = 0
  /**
   * @param {CircleAttributes} [attributes={}]
   */
  constructor({ x = 0, y = 0, radius = 1, ...attributes } = {}) {
    super('circle', {
      cx: x,
      cy: y,
      r: radius,
      ...attributes,
    })
    this.#x = x
    this.#y = y
    this.#radius = radius
  }

  /**
   * @param {number} value
   */
  set x(value) {
    this.setAttributes({ cx: value })
    this.#x = value
  }
  get x() {
    return this.#x
  }

  /**
   * @param {number} value
   */
  set y(value) {
    this.setAttributes({ cy: value })
    this.#y = value
  }
  get y() {
    return this.#y
  }

  /**
   * @param {number} value
   */
  set radius(value) {
    this.setAttributes({ r: value })
    this.#radius = value
  }
  get radius() {
    return this.#radius
  }

  /**
   * @returns {Vector2}
   */
  center() {
    return vec2(this.x, this.y)
  }

  /**
   * @param {Circle} other
   * @returns {boolean}
   */
  intersectsCircle(other) {
    return this.center().distanceTo(other.center()) < this.radius + other.radius
  }

  /**
   * Returns a list of all bitangents, i.e. lines that are tangent to both circles.
   * Thanks SO! https://math.stackexchange.com/questions/719758/inner-tangent-between-two-circles-formula
   * @param {Circle} other
   * @returns {[Vector2, Vector2, number, 'inner' | 'outer'][]} a list of tangents,
   * where the first value is the point on `small` and the second value is the point on `large`,
   * and the third value is the angle of the tangent points relative to 0 radians
   */
  bitangents(other) {
    // there is some duplicated calculations in outer and inner tangents; consider refactoring
    // @ts-expect-error TS can't handle that 'outer' concatenated with 'inner' becomes 'outer' | 'inner'
    return this.outerTangents(other).concat(this.innerTangents(other))
  }

  /**
   * @param {Circle} other
   * @returns {[Vector2, Vector2, number, 'outer'][]} outer tangent lines
   * where the first value is the point on `small` and the second value is the point on `large`,
   * and the third value is the angle of the tangent points relative to 0 radians
   */
  outerTangents(other) {
    const small = this.radius > other.radius ? other : this
    const large = this.radius > other.radius ? this : other
    const hypotenuse = small.center().distanceTo(large.center())
    const short = large.radius - small.radius
    const angleBetweenCenters = Math.atan2(small.y - large.y, small.x - large.x)
    const phi = angleBetweenCenters + Math.acos(short / hypotenuse)
    const phi2 = angleBetweenCenters - Math.acos(short / hypotenuse)

    return [
      [
        vec2(
          small.x + small.radius * Math.cos(phi),
          small.y + small.radius * Math.sin(phi),
        ),
        vec2(
          large.x + large.radius * Math.cos(phi),
          large.y + large.radius * Math.sin(phi),
        ),
        phi,
        'outer',
      ],
      [
        vec2(
          small.x + small.radius * Math.cos(phi2),
          small.y + small.radius * Math.sin(phi2),
        ),
        vec2(
          large.x + large.radius * Math.cos(phi2),
          large.y + large.radius * Math.sin(phi2),
        ),
        phi2,
        'outer',
      ],
    ]
  }

  /**
   * @param {Circle} other
   * @returns {[Vector2, Vector2, number, 'inner'][]} inner tangent lines,
   * where the first value is the point on `small` and the second value is the point on `large`,
   * and the third value is the angle of the tangent points relative to 0 radians
   */
  innerTangents(other) {
    if (this.intersectsCircle(other)) {
      return []
    }
    const small = this.radius > other.radius ? other : this
    const large = this.radius > other.radius ? this : other
    const hypotenuse = small.center().distanceTo(large.center())
    const short = large.radius + small.radius
    const angleBetweenCenters = Math.atan2(small.y - large.y, small.x - large.x)
    const phi =
      angleBetweenCenters + Math.asin(short / hypotenuse) - Math.PI / 2
    const phi2 =
      angleBetweenCenters - Math.asin(short / hypotenuse) - Math.PI / 2

    return [
      [
        vec2(
          small.x - small.radius * Math.cos(phi),
          small.y - small.radius * Math.sin(phi),
        ),
        vec2(
          large.x + large.radius * Math.cos(phi),
          large.y + large.radius * Math.sin(phi),
        ),
        phi,
        'inner',
      ],
      [
        vec2(
          small.x + small.radius * Math.cos(phi2),
          small.y + small.radius * Math.sin(phi2),
        ),
        vec2(
          large.x - large.radius * Math.cos(phi2),
          large.y - large.radius * Math.sin(phi2),
        ),
        phi2,
        'inner',
      ],
    ]
  }
}

/**
 * @overload
 * @param {CircleAttributes} attrsOrBuilderOrX
 * @returns {Circle}
 */
/**
 * @overload
 * @param {number} attrsOrBuilderOrX
 * @param {number} y
 * @param {number} radius
 * @returns {Circle}
 */
/**
 * @overload
 * @param {(circle: Circle) => void} attrsOrBuilderOrX
 * @returns {Circle}
 */
/**
 * @param {CircleAttributes | number | ((circle: Circle) => void)} attrsOrBuilderOrX
 * @param {number} [y]
 * @param {number} [radius]
 */
export function circle(attrsOrBuilderOrX, y, radius) {
  if (typeof attrsOrBuilderOrX === 'function') {
    const c = new Circle()
    attrsOrBuilderOrX(c)
    return c
  }
  if (typeof attrsOrBuilderOrX === 'object') {
    return new Circle(attrsOrBuilderOrX)
  }
  if (
    typeof attrsOrBuilderOrX === 'number' &&
    typeof y === 'number' &&
    typeof radius === 'number'
  ) {
    return new Circle({ x: attrsOrBuilderOrX, y, radius })
  }
  throw new Error(
    `Unable to construct circle from "${attrsOrBuilderOrX}, ${y}, ${radius}"`,
  )
}

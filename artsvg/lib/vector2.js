import { random } from './random.js'

export class Vector2 {
  constructor(x, y) {
    this.x = x
    this.y = y
  }

  /**
   * @param {Vector2} other 
   * @returns {Vector2}
   */
  add(other) {
    return vec2(this.x + other.x, this.y + other.y)
  }

  /**
   * @param {Vector2} other 
   * @returns {Vector2}
   */
  subtract(other) {
    return vec2(this.x - other.x, this.y - other.y)
  }

  /**
   * @param {number} n
   * @returns {Vector2}
   */
  div(n) {
    return vec2(this.x / n, this.y / n)
  }

  /**
   * @param {number} n
   * @returns {Vector2}
   */
  multiply(n) {
    return vec2(this.x * n, this.y * n)
  }

  /**
   * Returns a Vector2 that is a mix
   * @param {Vector2} a 
   * @param {Vector2} b 
   * @param {number} mix a mix percentage in range [0, 1] where 0 returns a and 1 returns b
   * @returns {number}
   */
  static mix(a, b, mix) {
    return a.multiply(1 - mix).add(b.multiply(mix))
  }

  /**
   * @param {Vector2} other 
   * @returns {number}
   */
  distanceTo(other) {
    return Math.sqrt(Math.pow(other.x - this.x, 2) + Math.pow(other.y - this.y, 2))
  }

  /**
   * The euclidean length of the vector
   * @returns {number}
   */
  length() {
    return Math.sqrt(Math.pow(this.x, 2) + Math.pow(this.y, 2))
  }

  /**
   * Dot product
   * @param {Vector2} other 
   */
  dot(other) {
    return this.x * other.x + this.y * other.y
  }

  /**
   * @param {Vector2} other 
   * @returns {number}
   */
  angleTo(other) {
    return Math.atan2(other.y - this.y, other.x - this.x)
  }

  /**
   * @param {Vector2} a
   * @param {Vector2} b 
   * @returns {Vector2}
   */
  static midpoint(a, b) {
    return vec2((a.x + b.x) / 2, (a.y + b.y) / 2)
  }

  /**
   *
   * @param {number} xMin
   * @param {number} xMax
   * @param {number} yMin
   * @param {number} yMax
   * @returns {Vector2}
   */
  static random(xMin, xMax, yMin, yMax, rng) {
    return vec2(random(xMin, xMax, rng), random(yMin, yMax, rng));
  }
}

/**
 * @param {number} x 
 * @param {numbur} y 
 * @returns Vector2
 */
export function vec2(x, y) {
  return new Vector2(x, y)
}

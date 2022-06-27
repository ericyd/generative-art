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
   * @param {number} n
   * @returns {Vector2}
   */
  div(n) {
    return vec2(this.x  / n, this.y / n)
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

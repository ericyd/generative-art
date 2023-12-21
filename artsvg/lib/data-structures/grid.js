/**
 * @typedef {object} GridAttributes
 * @property {number} [xMin=0] the minimum x value (inclusive), when used as an iterator
 * @property {number} [xMax=1] the maximum x value (exclusive), when used as an iterator
 * @property {number} [yMin=0] the minimum y value (inclusive), when used as an iterator
 * @property {number} [yMax=1] the maximum y value (exclusive), when used as an iterator
 * @property {number} [xStep=1] the step size in the x direction
 * @property {number} [yStep=1] the step size in the x direction
 * @property {number} [columns] the number of columns in the grid. This is more commonly defined when using the grid as a data store, but if `columns` is defined it will override `xMax` when used as an iterator.
 * @property {number} [rows] the number of rows in the grid. This is more commonly defined when using the grid as a data store, but if `rows` is defined it will override `yMax` when used as an iterator.
 * @property {'row major' | 'column major'} [order='row major'] of the grid. This is rarely necessary to define, but since the internal representation of the grid is a 1-D array, this defines the layout of the cells.
 * @property {*} [fill] the value to fill the grid with. This is only used when the grid is used as a data store.
 */

import { Vector2, vec2 } from '../vector2.js'

export class Grid {
  /** @type {number} */
  #xMin
  /** @type {number} */
  #xMax
  /** @type {number} */
  #yMin
  /** @type {number} */
  #yMax
  /** @type {number} */
  #xStep
  /** @type {number} */
  #yStep
  /** @type {number} */
  #columns
  /** @type {number} */
  #rows
  /** @type {'row major' | 'column major'} */
  #order
  /** @type {number[]} */
  #grid
  /**
   * @param {GridAttributes} [attributes={}]
   */
  constructor({
    xMin = 0,
    xMax = 1,
    yMin = 0,
    yMax = 1,
    xStep = 1,
    yStep = 1,
    order = 'row major',
    columns,
    rows,
    fill,
  } = {}) {
    this.#xMin = xMin
    this.#xMax = columns ? this.#xMin + columns : xMax
    this.#yMin = yMin
    this.#yMax = rows ? this.#yMin + rows : yMax
    this.#xStep = xStep
    this.#yStep = yStep
    this.#columns =
      columns ?? Math.floor((this.#xMax - this.#xMin) / this.#xStep)
    this.#rows = rows ?? Math.floor((this.#yMax - this.#yMin) / this.#yStep)
    this.#grid = new Array(this.#columns * this.#rows)
    if (fill !== undefined) {
      this.#grid.fill(fill)
    }
    this.#order = order
  }

  /**
   * @overload
   * @param {Vector2} x
   * @returns {Integer}
   */
  /**
   * @overload
   * @param {Integer} x
   * @param {Integer} y
   * @returns {Integer}
   */
  /**
   * @param {Integer | Vector2} x
   * @param {Integer} [y]
   * @returns {Integer}
   */
  #index(x, y) {
    const [i, j] =
      x instanceof Vector2
        ? [x.x, x.y]
        : y !== undefined
          ? [x, y]
          : (() => {
              throw new Error(`invalid arguments ${x}, ${y}`)
            })()
    if (this.#order === 'row major') {
      return this.#columns * j + i
    }
    return this.#rows * i + j
  }

 
  /**
   * @overload
   * @param {Vector2} x
   * @returns {?}
   */
  /**
   * @overload
   * @param {Integer} x
   * @param {Integer} y
   * @returns {?}
   */
  /**
   * @param {Integer | Vector2} x
   * @param {Integer} [y]
   * @returns {?}
   */

  /**
   * nice idea though doesn't seem to work https://austingil.com/typescript-function-overloads-with-jsdoc/
   * @type {{
   * (x: Integer, y: Integer): number;
   * (x: Vector2): number;
   * }}
   */
  get(x, y) {
    return this.#grid[this.#index(x, y)]
  }

  /**
   * @template T
   * @param {[number | Vector2, number, T]} args
   * @returns {void}
   */
  set(...args) {
    // TODO: is this the best way to handle overloading????? ??? ??????? ?? ? ?????
    const [x, y, value] =
      args[0] instanceof Vector2 ? [args[0].x, args[0].y, args[1]] : args
    this.#grid[this.#index(x, y)] = value
  }

  /**
   * @returns {Generator<{x: number, y: number}, void>}
   */
  *[Symbol.iterator]() {
    if (this.#order === 'row major') {
      for (let x = this.#xMin; x < this.#xMax; x += this.#xStep) {
        for (let y = this.#yMin; y < this.#yMax; y += this.#yStep) {
          yield vec2(x, y)
        }
      }
    } else {
      for (let y = this.#yMin; y < this.#yMax; y += this.#yStep) {
        for (let x = this.#xMin; x < this.#xMax; x += this.#xStep) {
          yield vec2(x, y)
        }
      }
    }
  }
}

export function grid(attributes) {
  return new Grid(attributes)
}

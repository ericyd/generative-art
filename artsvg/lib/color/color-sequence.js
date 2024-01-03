import { ColorRgb } from './rgb.js'

/**
 * `number` attribute is value in range [0, 1] that identifies the part of the spectrum where the color is present.
 * @typedef {[number, ColorRgb]} ColorStop
 */

export class ColorSequence {
  /** @type {ColorStop[]} */
  #pairs = []

  /**
   * @param {ColorStop[]} pairs
   */
  constructor(pairs) {
    this.#pairs = pairs
  }

  /**
   * @param {string[]} hexes list of color hex strings
   */
  static fromHexes(hexes) {
    console.log(
      hexes.map((hex, i, array) => [
        i / (array.length - 1),
        ColorRgb.fromHex(hex).toString(),
      ]),
    )
    return new ColorSequence(
      hexes.map((hex, i, array) => [
        i / (array.length - 1),
        ColorRgb.fromHex(hex),
      ]),
    )
  }

  /**
   * @param {number} t
   * @returns {ColorRgb}
   */
  at(t) {
    const stopB = this.#pairs.findIndex(([stopVal]) => stopVal >= t)
    // console.log(this.#pairs)
    console.log({ stopB })
    if (stopB === 0 || this.#pairs.length === 1) {
      return this.#pairs[stopB][1]
    }
    if (stopB === -1) {
      return this.#pairs[this.#pairs.length - 1][1]
    }
    const stopA = stopB - 1
    const range = this.#pairs[stopB][0] - this.#pairs[stopA][0]
    const percentage = (t - this.#pairs[stopA][0]) / range
    console.log({ range, percentage })
    return this.#pairs[stopA][1].mix(this.#pairs[stopB][1], percentage)
  }
}

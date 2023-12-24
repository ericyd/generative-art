import { ColorRgb } from './rgb.js'

export class ColorSequence {
  /**
   * @param {string[]} hexes list of color hex strings
   */
  static fromHexes(hexes) {}

  /**
   * @param {number} t
   * @returns {ColorRgb}
   */
  at(t) {
    const stopA = 0 // TODO: find first stop
    const stopB = 0 // TODO: find second stop
    // return stopA.mix(stopB, ratio of stopA to stopB)
    return new ColorRgb(0, 0, 0)
  }
}

import { warnWithDefault } from '../internal.js'
import { clamp } from '../util.js'

export class ColorRgb {
  /**
   * @param {number} r red, in range [0, 1]
   * @param {number} g green, in range [0, 1]
   * @param {number} b blue, in range [0, 1]
   * @param {number} a alpha, in range [0, 1]
   */
  constructor(r, g, b, a = 1) {
    this.r =
      r > 1 || r < 0
        ? warnWithDefault(`clamping r '${r}' to [0, 1]`, clamp(0, 1, r))
        : r
    this.g =
      g > 1 || g < 0
        ? warnWithDefault(`clamping g '${g}' to [0, 1]`, clamp(0, 1, g))
        : g
    this.b =
      b > 1 || b < 0
        ? warnWithDefault(`clamping b '${b}' to [0, 1]`, clamp(0, 1, b))
        : b
    this.a =
      a > 1 || a < 0
        ? warnWithDefault(`clamping a '${a}' to [0, 1]`, clamp(0, 1, a))
        : a
  }

  /**
   * credit: https://github.com/openrndr/openrndr/blob/d184fed22e191df2860ed47f9f9354a142ad52b6/openrndr-color/src/commonMain/kotlin/org/openrndr/color/ColorRGBa.kt#L84-L131
   * @param {string} hex color hex string, e.g. '#000'
   */
  static fromHex(hex) {
    const raw = hex.replace(/^0x|#/, '')
    /**
     * @param {string} str
     * @param {number} start
     * @param {number} end
     * @returns number
     */
    const fromHex = (str, start, end, multiplier = 1) =>
      (multiplier * parseInt(str.slice(start, end), 16)) / 255

    switch (raw.length) {
      case 3:
        return new ColorRgb(
          fromHex(raw, 0, 1, 17),
          fromHex(raw, 1, 2, 17),
          fromHex(raw, 2, 3, 17),
        )
      case 6:
        return new ColorRgb(
          fromHex(raw, 0, 2),
          fromHex(raw, 2, 4),
          fromHex(raw, 4, 6),
        )
      case 8:
        return new ColorRgb(
          fromHex(raw, 0, 2),
          fromHex(raw, 2, 4),
          fromHex(raw, 4, 6),
          fromHex(raw, 6, 8),
        )
      default:
        throw new Error(`Cannot construct ColorRgb from value ${hex}`)
    }
  }

  toString() {
    return `rgb(${this.r * 255}, ${this.g * 255}, ${this.b * 255}, ${this.a})`
  }
}

ColorRgb.Black = new ColorRgb(0, 0, 0)
ColorRgb.White = new ColorRgb(1, 1, 1)

/**
 * @param {number} r red, in range [0, 1]
 * @param {number} g green, in range [0, 1]
 * @param {number} b blue, in range [0, 1]
 * @param {number} a alpha, in range [0, 1]
 * @returns {ColorRgb}
 */
export function rgb(r, g, b, a = 1) {
  return new ColorRgb(r, g, b, a)
}

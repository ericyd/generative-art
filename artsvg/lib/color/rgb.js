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

  /**
   * This is very homespun, I imagine there are serious optimizations available
   * @param {ColorRgb} other
   * @param {number} [mix=0.5] the mix of the two colors. When 0, returns `this`. When 1, returns `other`
   * @returns {ColorRgb}
   */
  mix(other, mix = 0.5) {
    const r = mixColorComponent(this.r, other.r, mix)
    const g = mixColorComponent(this.g, other.g, mix)
    const b = mixColorComponent(this.b, other.b, mix)
    const a = lerp(this.a, other.a, mix)
    return new ColorRgb(r, g, b, a)
  }

  toString() {
    return `rgb(${this.r * 255}, ${this.g * 255}, ${this.b * 255}, ${this.a})`
  }

  toHex() {
    return [
      '#',
      Math.round(this.r * 255)
        .toString(16)
        .padStart(2, '0'),
      Math.round(this.g * 255)
        .toString(16)
        .padStart(2, '0'),
      Math.round(this.b * 255)
        .toString(16)
        .padStart(2, '0'),
      Math.round(this.a * 255)
        .toString(16)
        .padStart(2, '0'),
    ].join('')
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

/**
 * Mixes two color components (in range [0, 1]) using linear interpolation.
 * Color components mix in an interesting way because the values cycle,
 * i.e. 0.001 is "close" to 0.999 on a color wheel.
 * @param {number} a in range [0, 1]
 * @param {number} b in range [0, 1]
 * @param {number} mix in range [0, 1]. When 0, returns `a`. When 1, returns `b`.
 * @returns {number}
 */
export function mixColorComponent(a, b, mix) {
  const aVal = a < b && b - a > 0.5 ? a + 1 : a
  const bVal = b < a && a - b > 0.5 ? b + 1 : b
  const aPct = 1 - mix
  const bPct = mix
  const result = aVal * aPct + bVal * bPct
  return result > 1 ? result - 1 : result
}

// this should probably go in a utility file
/**
 *
 * @param {number} a in range [0, 1]
 * @param {number} b in range [0, 1]
 * @param {number} mix in range [0, 1]
 * @returns {number}
 */
function lerp(a, b, mix) {
  const aPct = 1 - mix
  const bPct = mix
  const result = a * aPct + b * bPct
  return result
}

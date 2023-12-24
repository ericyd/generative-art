import { warnWithDefault } from '../internal.js'
import { clamp } from '../util.js'

export class ColorHsl {
  /**
   * @param {number} h hue, in range [0, 360]
   * @param {number} s saturation, in range [0, 1]
   * @param {number} l luminocity, in range [0, 1]
   * @param {number} [a=1] alpha, in range [0, 1]
   * @returns
   */
  constructor(h, s, l, a = 1) {
    this.h =
      h > 360 || h < 0
        ? warnWithDefault(`clamping h '${h}' to [0, 360]`, clamp(0, 360, h))
        : h
    this.s =
      s > 1 || s < 0
        ? warnWithDefault(`clamping s '${s}' to [0, 1]`, clamp(0, 1, s))
        : s
    this.l =
      l > 1 || l < 0
        ? warnWithDefault(`clamping l '${l}' to [0, 1]`, clamp(0, 1, l))
        : l
    this.a =
      a > 1 || a < 0
        ? warnWithDefault(`clamping a '${a}' to [0, 1]`, clamp(0, 1, a))
        : a
    return
  }

  toString() {
    return `hsl(${this.h}, ${this.s}%, ${this.l}%, ${this.a})`
  }
}

/**
 * @param {number} h hue, in range [0, 360]
 * @param {number} s saturation, in range [0, 1]
 * @param {number} l luminocity, in range [0, 1]
 * @param {number} [a=1] alpha, in range [0, 1]
 * @returns {ColorHsl} color in hsl format
 */
export function hsl(h, s, l, a = 1) {
  return new ColorHsl(h, s, l, a)
}

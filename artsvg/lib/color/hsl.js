import { warnWithDefault } from '../internal.js'
import { clamp } from '../util.js'
import { ColorRgb } from './rgb.js'

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

  /**
   * Converts a ColorRgb to ColorHsl.
   * Thank you OPENRNDR: https://github.com/openrndr/openrndr/blob/71f233075e01ced7670963194e8730bc5c35c67c/openrndr-color/src/commonMain/kotlin/org/openrndr/color/ColorHSLa.kt#L28
   * And SO: https://stackoverflow.com/questions/39118528/rgb-to-hsl-conversion
   * @param {ColorRgb} rgb
   * @returns {ColorHsl}
   */
  static fromRgb(rgb) {
    const srgb = rgb
    const min = Math.min(srgb.r, srgb.g, srgb.b)
    const max = Math.max(srgb.r, srgb.g, srgb.b)
    const component = max === srgb.r ? 'r' : max === srgb.g ? 'g' : 'b'

    // In the case r == g == b
    if (min === max) {
      return new ColorHsl(0, 0, max, srgb.a)
    }
    const delta = max - min
    const l = (max + min) / 2
    const s = delta / (1 - Math.abs(2 * l - 1))
    const h =
      60 *
      (component === 'r'
        ? ((rgb.g - rgb.b) / delta) % 6
        : component === 'g'
          ? (srgb.b - srgb.r) / delta + 2
          : component === 'b'
            ? (srgb.r - srgb.g) / delta + 4
            : warnWithDefault(
                `Unable to successfully convert value ${rgb} to HSL space. Defaulting hue to 0.`,
                0,
              ))
    return new ColorHsl(h, s, l, srgb.a)
  }

  /**
   * @returns {ColorRgb}
   */
  toRgb() {
    if (this.s === 0.0) {
      return new ColorRgb(this.l, this.l, this.l, this.a)
    }
    const q =
      this.l < 0.5 ? this.l * (1 + this.s) : this.l + this.s - this.l * this.s
    const p = 2 * this.l - q
    const r = hue2rgb(p, q, this.h / 360.0 + 1.0 / 3)
    const g = hue2rgb(p, q, this.h / 360.0)
    const b = hue2rgb(p, q, this.h / 360.0 - 1.0 / 3)
    return new ColorRgb(r, g, b, this.a)
  }

  toString() {
    return `hsl(${this.h}, ${this.s * 100}%, ${this.l * 100}%, ${this.a})`
  }

  /**
   * Mix two colors together.
   * This is very homespun, I imagine there are optimizations available
   * @param {ColorHsl} other
   * @param {number} [mix=0.5] The mix of colors. When 0, returns `this`. When 1, returns `other`
   * @returns {ColorHsl}
   */
  mix(other, mix = 0.5) {
    const h = mixColorComponent(this.h, other.h, mix)
    const s = lerp(this.s, other.s, mix)
    const l = lerp(this.l, other.l, mix)
    const a = lerp(this.a, other.a, mix)
    return new ColorHsl(h, s, l, a)
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
  const aVal = a < b && b - a > 180 ? a + 360 : a
  const bVal = b < a && a - b > 180 ? b + 360 : b
  const aPct = 1 - mix
  const bPct = mix
  const result = aVal * aPct + bVal * bPct
  return result > 360 ? result - 360 : result
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

/**
 * Honestly not sure what this does
 * https://github.com/openrndr/openrndr/blob/71f233075e01ced7670963194e8730bc5c35c67c/openrndr-color/src/commonMain/kotlin/org/openrndr/color/ColorHSLa.kt#L123C10-L130C2
 * @param {number} p
 * @param {number} q
 * @param {number} ut
 * @returns {number}
 */
function hue2rgb(p, q, ut) {
  let t = ut
  while (t < 0) t += 1.0
  while (t > 1) t -= 1.0
  if (t < 1.0 / 6.0) return p + (q - p) * 6.0 * t
  if (t < 1.0 / 2.0) return q
  return t < 2.0 / 3.0 ? p + (q - p) * (2.0 / 3.0 - t) * 6.0 : p
}

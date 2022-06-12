// resources: https://observablehq.com/@makio135/utilities

export const array = (n) => new Array(n).fill(0).map((_zero, i) => i);

// should deprecate this
export { random } from "./random.js";

/**
 *
 * @param {Degrees} degrees
 * @returns Radians
 */
export function degToRad(degrees) {
  return (degrees * Math.PI * 2) / 180;
}

/**
 * https://github.com/openrndr/openrndr/blob/2ca048076f6999cd79aee0d5b3db471152f59063/openrndr-math/src/commonMain/kotlin/org/openrndr/math/Mapping.kt#L8-L33
 * Linearly maps a value, which is given in the before domain to a value in the after domain.
 * @param {number} beforeLeft the lowest value of the before range
 * @param {number} beforeRight the highest value of the before range
 * @param {number} afterLeft the lowest value of the after range
 * @param {number} afterRight the highest value of the after range
 * @param {number} value the value to be mapped
 * @param {boolean} clamp constrain the result to the after range
 * @return {number} a value in the after range
 */
export function map(
  beforeLeft,
  beforeRight,
  afterLeft,
  afterRight,
  value,
  doClamp = false
) {
  const db = beforeRight - beforeLeft;
  const da = afterRight - afterLeft;

  if (db != 0.0) {
    const n = (value - beforeLeft) / db;
    return afterLeft + (doClamp ? clamp(0.0, 1.0, n) : n) * da;
  } else {
    const n = value - beforeLeft;
    afterLeft + (doClamp ? clamp(0.0, 1.0, n) : n) * da;
  }
}

/**
 *
 * @param {number} x
 * @returns number
 */
export const clamp = (min, max, x) => Math.max(min, Math.min(max, x));

/**
 *
 * @param {number} quantum
 * @param {number} value
 * @returns number
 */
export const quantize = (quantum, value) =>
  Math.round(value / quantum) * quantum;

import { clamp } from "./util.js";

/**
 * @param {number} h 
 * @param {number} s 
 * @param {number} l 
 * @returns {string} color in hsl format
 */
export function hsl(h, s, l) {
  const h2 = clamp(0, 360, h);
  const s2 = clamp(0, 100, s);
  const l2 = clamp(0, 100, l);
  if (h < 0 || h > 360) {
    console.warn(`h is outside of [0, 360]: ${h}. Clamped to ${h2}`);
  }
  if (s < 0 || s > 100) {
    console.warn(`s is outside of [0, 100]: ${s}. Clamped to ${s2}`);
  }
  if (l < 0 || l > 100) {
    console.warn(`l is outside of [0, 100]: ${l}. Clamped to ${l2}`);
  }
  return `hsl(${h2}, ${s2}%, ${l2}%)`;
}

/**
 * @param {number} h 
 * @param {number} s 
 * @param {number} l 
 * @param {number} a
 * @returns {string} color in hsla format
 */
export function hsla(h, s, l, a) {
  const h2 = clamp(0, 360, h);
  const s2 = clamp(0, 100, s);
  const l2 = clamp(0, 100, l);
  const a2 = clamp(0, 1, a);
  if (h < 0 || h > 360) {
    console.warn(`h is outside of [0, 360]: ${h}. Clamped to ${h2}`);
  }
  if (s < 0 || s > 100) {
    console.warn(`s is outside of [0, 100]: ${s}. Clamped to ${s2}`);
  }
  if (l < 0 || l > 100) {
    console.warn(`l is outside of [0, 100]: ${l}. Clamped to ${l2}`);
  }
  if (a < 0 || a > 1) {
    console.warn(`a is outside of [0, 1]: ${a}. Clamped to ${a2}`);
  }
  return `hsl(${h2}, ${s2}%, ${l2}%, ${a2})`;
}

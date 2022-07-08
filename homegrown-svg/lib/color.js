import { clamp } from "./util.js";

/**
 * @param {number} h 
 * @param {number} s 
 * @param {number} l 
 * @returns {Hsla} color in hsl format
 */
export function hsl(h, s, l) {
  return new Hsla(h, s, l, 1.0)
}

export class Hsla {
  constructor(h, s, l, a) {
    this.h = clamp(0, 360, h);
    this.s = clamp(0, 100, s);
    this.l = clamp(0, 100, l);
    this.a = clamp(0, 1, a);
    if (h < 0 || h > 360) {
      console.warn(`h is outside of [0, 360]: ${h}. Clamped to ${this.h}`);
    }
    if (s < 0 || s > 100) {
      console.warn(`s is outside of [0, 100]: ${s}. Clamped to ${this.s}`);
    }
    if (l < 0 || l > 100) {
      console.warn(`l is outside of [0, 100]: ${l}. Clamped to ${this.l}`);
    }
    if (a < 0 || a > 1) {
      console.warn(`a is outside of [0, 1]: ${a}. Clamped to ${this.a}`);
    }
    return ;
  }

  toString() {
    return `hsla(${this.h}, ${this.s}%, ${this.l}%, ${this.a})`;
  }
}

/**
 * @param {number} h 
 * @param {number} s 
 * @param {number} l 
 * @param {number} a
 * @returns {Hsla} color in hsla format
 */
export function hsla(h, s, l, a) {
  return new Hsla(h, s, l, a)
}

export const ColorHex = {
  black: '#000000',
  white: '#ffffff'
}
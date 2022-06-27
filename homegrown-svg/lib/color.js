import { clamp } from "./util.js";

export class Hsl {
  constructor(h, s, l) {
    if (h < 0 || h > 360) {
      console.warn(`h is outside of [0, 360]: ${h}. Clamped to ${h2}`);
    }
    if (s < 0 || s > 100) {
      console.warn(`s is outside of [0, 100]: ${s}. Clamped to ${s2}`);
    }
    if (l < 0 || l > 100) {
      console.warn(`l is outside of [0, 100]: ${l}. Clamped to ${l2}`);
    }
    this.h = clamp(0, 360, h);
    this.s = clamp(0, 100, s);
    this.l = clamp(0, 100, l);
  }

  toString() {
    return `hsl(${this.h}, ${this.s}%, ${this.l}%)`;
  }
}

/**
 * @param {number} h 
 * @param {number} s 
 * @param {number} l 
 * @returns {Hsl} color in hsl format
 */
export function hsl(h, s, l) {
  return new Hsl(h, s, l)
}

export class Hsla {
  constructor(h, s, l, a) {
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
    this.h = clamp(0, 360, h);
    this.s = clamp(0, 100, s);
    this.l = clamp(0, 100, l);
    this.a = clamp(0, 1, a);
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
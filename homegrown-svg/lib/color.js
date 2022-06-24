import { clamp } from './util.js'

export function hsl(h, s, l) {
  h = clamp(0, 360, h);
  s = clamp(0, 100, s)
  l = clamp(0, 100, l)
  return `hsl(${h}, ${s}%, ${l}%)`
}

export function hsla(h, s, l, a) {
  h = clamp(0, 360, h);
  s = clamp(0, 100, s)
  l = clamp(0, 100, l)
  a = clamp(0, 1, a)
  return `hsl(${h}, ${s}%, ${l}%, ${a})`
}

// resources: https://observablehq.com/@makio135/utilities

export const array = n => new Array(n).fill(0).map((_zero, i) => i)

export function random(min, max, rng = Math.random) {
  return min + rng() * (max - min)
}

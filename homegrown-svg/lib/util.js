// resources: https://observablehq.com/@makio135/utilities
// consider: https://stackoverflow.com/questions/521295/seeding-the-random-number-generator-in-javascript

export const array = n => new Array(n).fill(0).map((_zero, i) => i)

export function random(min, max) {
  return min + Math.random() * (max - min)
}

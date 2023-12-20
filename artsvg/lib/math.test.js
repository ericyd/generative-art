import assert from 'assert'
import { vec2 } from './vector2.js'
import { smallestAngularDifference, angleOfVertex, isWithin } from './math.js'
import { describe, it } from 'node:test'

// Note: due to floating point calculations, some tests need to test a range

describe('smallestAngularDifference', () => {
  const tests = [
    // "12-o-clock" to "9-o-clock" = quarter turn anti-clockwise
    { angle1: 0, angle2: 0, expected: 0 },
    // "9-o-clock" to "12-o-clock" = quarter turn clockwise
    { angle1: -Math.PI / 2, angle2: Math.PI, expected: -Math.PI / 2},
    // argument order matters!!! 
    { angle1: Math.PI, angle2: -Math.PI / 2, expected: Math.PI / 2},
    // same angle, different sign = 0
    { angle1: -Math.PI,angle2:  Math.PI, expected: 0},
    { angle1: Math.PI, angle2: -Math.PI, expected: 0},
    { angle1: -Math.PI / 2, angle2: Math.PI * 3/2, expected: 0},
    { angle1: Math.PI * 3/2, angle2: -Math.PI / 2, expected: 0},
    { angle1: 0.5, angle2: 2.75, expected: 2.25},
  ]

  for (const { angle1, angle2, expected } of tests) {
    it(`should return ${expected} for ${angle1} and ${angle2}`, () => {
      assert.equal(smallestAngularDifference(angle1, angle2), expected)
    })
  }

  // floating point is hard so some tests need to test a small range
  const approximateTests = [
    { angle1: -Math.PI * 0.99, angle2: Math.PI * 0.99, expected: -Math.PI * 0.02 }
  ]

  for (const { angle1, angle2, expected } of approximateTests) {
    it(`should return ${expected} for ${angle1} and ${angle2}`, () => {
      const actual = smallestAngularDifference(angle1, angle2)
      assert(isWithin(  expected - 0.001,  expected + 0.001,  actual))
    })
  }
})


describe('angleOfVertex', () => {
  const tests = [
    { a: vec2(2, 0), b: vec2(0, 0), c: vec2(4, 4), expected: Math.PI / 4 },
  ]

  for (const { a, b, c, expected } of tests) {
    it(`should return ${expected} for ${a}, ${b}, ${c}`, () => {
      const actual = angleOfVertex(a, b, c)
      assert(isWithin(expected - 0.001, expected + 0.001, actual))
    })
  }
})


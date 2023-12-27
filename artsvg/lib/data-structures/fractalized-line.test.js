import { describe, it } from 'node:test'
import assert from 'node:assert'
import { FractalizedLine } from './fractalized-line.js'
import { vec2 } from '../vector2.js'

/**
 *
 * @param {number} defaultValue
 * @param {number[]} setValues
 * @returns
 */
function fakeRng(defaultValue = 0.5, setValues = []) {
  let i = 0
  return () => {
    i++
    return setValues[i] ?? defaultValue
  }
}

describe('FractalizedLine', () => {
  it('generates one midpoint for every existing point one every subdivision', () => {
    const rng = fakeRng()
    const line = new FractalizedLine([vec2(0, 0), vec2(1, 1)], rng)
    line.perpendicularSubdivide(1)
    assert.strictEqual(line.points.length, 3)
    line.perpendicularSubdivide(1)
    assert.strictEqual(line.points.length, 5)
    line.perpendicularSubdivide(1)
    assert.strictEqual(line.points.length, 9)
  })

  it('returns a new line with points generated from a fractal subdivision pattern', () => {
    const rng = fakeRng()
    const line = new FractalizedLine([vec2(0, 0), vec2(1, 1)], rng)
    line.perpendicularSubdivide(3)
    // these points are all exactly in the middle because the fakeRng returns 0.5. Other test values are hard to mentally grok so just going with simple for now.
    assert.deepStrictEqual(line.points, [
      vec2(0, 0),
      vec2(0.125, 0.125),
      vec2(0.25, 0.25),
      vec2(0.375, 0.375),
      vec2(0.5, 0.5),
      vec2(0.625, 0.625),
      vec2(0.75, 0.75),
      vec2(0.875, 0.875),
      vec2(1, 1),
    ])
  })
})

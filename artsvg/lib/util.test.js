import { describe, it } from 'node:test'
import assert from 'assert'
import {
  array,
range,
rangeWithIndex,
degToRad,
map,
clamp,
quantize,
pickBy } from './util.js'

describe('array', () => {
  it('returns an array of size n filled with the index', () => {
    const actual = array(5)
    assert.deepStrictEqual(actual, [0, 1, 2, 3, 4])
  })
})

describe('range', () => {
  it('returns an array in range [min, max)', () => {
    const actual = range(2, 6)
    assert.deepStrictEqual(actual, [2, 3, 4, 5])
  })

  // probably could be more graceful about this...
  it('throws error if min is greater than max', () => {
    assert.throws(() => range(3, 2))
  })
})

describe('rangeWithIndex', () => {
  it('returns an array in range [min, max) with index attached', () => {
    const actual = rangeWithIndex(2, 6)
    assert.deepStrictEqual(actual, [[2, 0], [3, 1], [4, 2], [5, 3]])
  })

  // probably could be more graceful about this...
  it('throws error if min is greater than max', () => {
    assert.throws(() => rangeWithIndex(3, 2))
  })
})

describe('degToRad', () => {
  const tests = [
    [90, Math.PI / 2],
    [180, Math.PI],
    [270, Math.PI * 3 / 2],
    [360, Math.PI * 2],
  ]
  for (const [degrees, radians] of tests) {
    it(`converts ${degrees} degrees to ${radians} radians`, () => {
      assert.strictEqual(degToRad(degrees), radians)
    })
  }
})

describe('map', () => {
  it('linearly maps values from before range to after range', () => {
    const actual = map(0, 1, 10, 20, 0.5)
    assert.strictEqual(actual, 15)
  })
})

describe('clamp', () => {
  it('clamps to max when value is greater than max', () => {
    const actual = clamp(0, 100, 101)
    assert.strictEqual(actual, 100)
  })

  it('clamps to min when value is less than min', () => {
    const actual = clamp(0, 100, -1)
    assert.strictEqual(actual, 0)
  })

  it('does not clamp when value is in range', () => {
    const actual = clamp(0, 100, 50)
    assert.strictEqual(actual, 50)
  })
})

describe('quantize', () => {
  it('rounds value up to nearest quantum', () => {
    const actual = quantize(5, 4.5)
    assert.strictEqual(actual, 5)
  })

  it('rounds value down to nearest quantum', () => {
    const actual = quantize(5, 5.5)
    assert.strictEqual(actual, 5)
  })

  it('works for arbitrarily large values', () => {
    const actual = quantize(5, 5449440493093209.5)
    assert.strictEqual(actual, 5449440493093210)
  })
})

describe('pickBy', () => {
  it('returns object of same type that matches predicate', () => {
    const obj = { a: 1, b: 2, c: 3 }
    // @ts-ignore
    const predicate = (val, key) => val < 3 && /[ac]/.test(key)
    const actual = pickBy(predicate, obj)
    assert.deepStrictEqual(actual, { a: 1 })
  })
})

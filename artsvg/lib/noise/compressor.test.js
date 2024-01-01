import { describe, it } from "node:test";
import { Compressor } from "./compressor.js";
import assert from "node:assert";

describe('Compressor', () => {
  const compressor = new Compressor({ W: 0.1, T: 0.5, R: 2 })

  describe('belowKnee', () => {
    const tests = [
      [-2.0, false],
      [-1.0, false],
      [-0.7, false],
      [-0.6, false],
      [-0.5, false],
      [-0.45, false],
      // this is the cutoff because the difference between value and threshold is greater than half of "knee" (W)
      [-0.449, true],
      [-0.4, true],
      [-0.3, true],
      [-0.2, true],
      [-0.1, true],
      [0.0, true],
      [0.1, true],
      [0.2, true],
      [0.3, true],
      [0.4, true],
      [0.449, true],
      // cutoff
      [0.45, false],
      [0.5, false],
      [0.6, false],
      [0.7, false],
      [1.0, false],
      [2.0, false],
    ]

    for (const test of tests) {
      it(`returns "${test[1]}" for input ${test[0]} with threshold ${compressor.T} and knee ${compressor.W}`, () => {
        assert.strictEqual(compressor.belowKnee(test[0]), test[1])
      })
    }
  })

  describe('insideKnee', () => {
    const tests = [
      [-2.0, false],
      [-1.0, false],
      [-0.7, false],
      [-0.6, false],
      // cutoff
      [-0.5499, true],
      [-0.5, true],
      [-0.45, true],
      // cutoff
      [-0.449, false],
      [-0.4, false],
      [0.0, false],
      [0.4, false],
      [0.449, false],
      // cutoff
      [0.45, true],
      [0.5, true],
      [0.5499, true],
      // cutoff
      [0.55, false],
      [0.7, false],
      [1.0, false],
      [2.0, false],
    ]

    for (const test of tests) {
      it(`returns "${test[1]}" for input ${test[0]} with threshold ${compressor.T} and knee ${compressor.W}`, () => {
        assert.strictEqual(compressor.insideKnee(test[0]), test[1])
      })
    }
  })

  describe('compressInsideKnee', () => {
    const tests = [
      [-0.5499, -0.5249499750000001], 
      [-0.525, -0.5109375], 
      [-0.5, -0.49375], 
      [-0.475, -0.47343749999999996], 
      [-0.45, -0.45], 
      [0.45, 0.45], 
      [0.475, 0.47343749999999996], 
      [0.5, 0.49375], 
      [0.525, 0.5109375], 
      [0.5499, 0.5249499750000001], 
    ]

    for (const test of tests) {
      it(`returns "${test[1]}" for input ${test[0]} with threshold ${compressor.T} and knee ${compressor.W} and ratio ${compressor.R}`, () => {
        assert.strictEqual(compressor.compressInsideKnee(test[0]), test[1])
      })
    }
  })

  describe('compressAboveKnee', () => {
    const tests = [
      [-2.0, -1.25],
      [-1.0, -0.75],
      [-0.7, -0.6],
      [-0.6, -0.55],
      [-0.55, -0.525],
      // note: the output value is just above the upper end of the "compressInsideKnee" function, which is expected
      [0.55, 0.525],
      [0.6, 0.55],
      [0.7, 0.6],
      [1.0, 0.75],
      [2.0, 1.25],
    ]

    for (const test of tests) {
      it(`returns "${test[1]}" for input ${test[0]} with threshold ${compressor.T} and knee ${compressor.W} and ratio ${compressor.R}`, () => {
        assert.strictEqual(compressor.compressAboveKnee(test[0]), test[1])
      })
    }
  })
})
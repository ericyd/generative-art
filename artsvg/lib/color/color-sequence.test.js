import { describe, it } from 'node:test'
import assert from 'node:assert'
import { ColorSequence } from './color-sequence.js'
import { ColorRgb } from './rgb.js'

describe('ColorSequence', () => {
  describe('fromHexes', () => {
    it('with two colors, creates a color sequence with color stops at 0 and 1', () => {
      const spectrum = ColorSequence.fromHexes(['010101', 'fefefe'])
      assert.strictEqual(spectrum.at(0).toHex(), '#010101ff')
      assert.notStrictEqual(spectrum.at(0.01).toHex(), '#010101ff')
      assert.strictEqual(spectrum.at(1).toHex(), '#fefefeff')
      assert.notStrictEqual(spectrum.at(0.94).toHex(), '#fefefeff')
      assert.strictEqual(spectrum.at(0.5).toHex(), '#808080ff')
    })
  })

  describe('at', () => {
    it('returns lowest stop when `t` is lower than lowest stop is requested', () => {
      const spectrum = new ColorSequence([
        [0.3, ColorRgb.fromHex('#ab85a9ff')],
        [0.7, ColorRgb.fromHex('#97febcff')],
      ])
      assert.strictEqual(spectrum.at(0.3).toHex(), '#ab85a9ff')
      assert.strictEqual(spectrum.at(0).toHex(), '#ab85a9ff')
      assert.strictEqual(spectrum.at(-100).toHex(), '#ab85a9ff')
    })

    it('returns highest stop when `t` is higher than highest stop is requested', () => {
      const spectrum = new ColorSequence([
        [0.3, ColorRgb.fromHex('#ab85a9ff')],
        [0.7, ColorRgb.fromHex('#97febcff')],
      ])
      assert.strictEqual(spectrum.at(0.7).toHex(), '#97febcff')
      assert.strictEqual(spectrum.at(1).toHex(), '#97febcff')
      assert.strictEqual(spectrum.at(100).toHex(), '#97febcff')
    })

    it('mixes colors together', () => {
      const spectrum = ColorSequence.fromHexes(['010101', 'fefefe'])
      const actual = spectrum.at(0.5)
      assert.strictEqual(actual.toHex(), '#808080ff')
    })
  })
})

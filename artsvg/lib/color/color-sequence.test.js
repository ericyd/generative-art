import { describe, it } from 'node:test'
import assert from 'node:assert'
import { ColorSequence } from './color-sequence.js'

describe('ColorSequence', () => {
  describe.skip('fromHexes', () => {
    it('with two colors, creates a color sequence with color stops at 0 and 1', () => {
      const spectrum = ColorSequence.fromHexes(['010101', 'fefefe'])
      assert.strictEqual(spectrum.at(0).toHex(), '#010101ff')
      console.log({
        'spectrum.at(0.05).toString()': spectrum.at(0.05).toString(),
      })
      assert.notStrictEqual(spectrum.at(0.01).toHex(), '#010101ff')
      assert.strictEqual(spectrum.at(1).toHex(), '#fefefeff')
      console.log({
        'spectrum.at(0.94).toString()': spectrum.at(0.94).toString(),
      })
      assert.notStrictEqual(spectrum.at(0.94).toHex(), '#fefefeff')
      assert.strictEqual(
        spectrum.at(0.5).toHex(),
        'rgb(127.5, 127.5, 127.5, 1)',
      )
    })
  })

  describe.skip('at', () => {
    it.todo('returns lowest stop when lower `t` is requested')
    it.todo('returns highest stop when higher `t` is requested')
    it.todo('mixes colors together')
  })
})

import { describe, it } from 'node:test'
import assert from 'node:assert'
import { ColorRgb, rgb } from './rgb.js'

describe('rgb', () => {
  it('returns instanceof ColorRgb', () => {
    assert(rgb(1, 1, 1) instanceof ColorRgb)
  })
})

describe('ColorRgb', () => {
  it('correctly stringifies with interpolation', () => {
    const actual = `${new ColorRgb(0.1, 0.2, 0.4, 1)}`
    assert.strictEqual(actual, 'rgb(25.5, 51, 102, 1)')
  })

  it('correctly stringifies with `toString()`', () => {
    const actual = new ColorRgb(0.1, 0.2, 0.4, 1).toString()
    assert.strictEqual(actual, 'rgb(25.5, 51, 102, 1)')
  })

  describe('fromHex', () => {
    /** @type {[string, {r: number, g: number, b: number, a: number }][]} */
    const tests = [
      ['#000', { r: 0, g: 0, b: 0, a: 1 }],
      ['#fff', { r: 1, g: 1, b: 1, a: 1 }],
      [
        '#888',
        { r: (17 * 8) / 255, g: (17 * 8) / 255, b: (17 * 8) / 255, a: 1 },
      ],
      [
        '#999',
        { r: (17 * 9) / 255, g: (17 * 9) / 255, b: (17 * 9) / 255, a: 1 },
      ],
      ['#ffffff', { r: 1, g: 1, b: 1, a: 1 }],
      ['#ffffff00', { r: 1, g: 1, b: 1, a: 0 }],
      ['#000000', { r: 0, g: 0, b: 0, a: 1 }],
      ['#00000000', { r: 0, g: 0, b: 0, a: 0 }],
      ['#000', { r: 0, g: 0, b: 0, a: 1 }],
      ['#fff', { r: 1, g: 1, b: 1, a: 1 }],
      ['#000000', { r: 0, g: 0, b: 0, a: 1 }],
      ['#ffffff', { r: 1, g: 1, b: 1, a: 1 }],
      ['0x000', { r: 0, g: 0, b: 0, a: 1 }],
      ['0xfff', { r: 1, g: 1, b: 1, a: 1 }],
      [
        '0x888',
        { r: (17 * 8) / 255, g: (17 * 8) / 255, b: (17 * 8) / 255, a: 1 },
      ],
      [
        '0x999',
        { r: (17 * 9) / 255, g: (17 * 9) / 255, b: (17 * 9) / 255, a: 1 },
      ],
      ['0xffffff', { r: 1, g: 1, b: 1, a: 1 }],
      ['0xffffff00', { r: 1, g: 1, b: 1, a: 0 }],
      ['0x000000', { r: 0, g: 0, b: 0, a: 1 }],
      ['0x00000000', { r: 0, g: 0, b: 0, a: 0 }],
    ]

    for (const [hex, { r, g, b, a }] of tests) {
      const actual = ColorRgb.fromHex(hex)

      it(`converts ${hex} red ${r}>`, () => {
        assert.strictEqual(actual.r, r)
      })

      it(`converts ${hex} green ${g}>`, () => {
        assert.strictEqual(actual.g, g)
      })

      it(`converts ${hex} blue ${b}>`, () => {
        assert.strictEqual(actual.b, b)
      })

      it(`converts ${hex} alpha ${a}>`, () => {
        assert.strictEqual(actual.a, a)
      })
    }
  })

  describe('mix', () => {
    it('mixes correctly', () => {
      const a = new ColorRgb(0.0, 0.0, 0.0)
      const b = new ColorRgb(1, 1, 1)
      const actual = a.mix(b)
      assert.strictEqual(actual.toString(), 'rgb(127.5, 127.5, 127.5, 1)')
    })
  })

  describe('toHex', () => {
    const hexes = [
      '#000000ff',
      '#ffffffff',
      '#010101ff',
      '#fefefeff',
      '#ab84f5ff',
    ]
    for (const hex of hexes) {
      it(`converts to and from ${hex}`, () => {
        assert.strictEqual(hex, ColorRgb.fromHex(hex).toHex())
      })
    }
  })
})

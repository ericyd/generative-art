import { describe, it } from 'node:test'
import assert from 'node:assert'
import { ColorRgb, rgb, mixColorComponent } from './rgb.js'
import { isWithinError } from '../math.js'

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

  // damn, I got this confused with HSL... fuck.
  // rgb components don't loop around, I'm thinking of hue...
  // need to convert to hsl or something... arg!
  // https://github.com/openrndr/openrndr/blob/bf2d6eecc1e2d1312da52765d10d668a0e2de7d2/openrndr-color/src/commonMain/kotlin/org/openrndr/color/ColorHSLa.kt#L28
  describe('mix', () => {
    it('mixes correctly across the 0/255 threshold [test 1]', () => {
      const a = new ColorRgb(0.9, 0.9, 0.9)
      const b = new ColorRgb(0.1, 0.1, 0.1)
      const actual = a.mix(b)
      // mixes to 1, which is 255 in hex
      assert.strictEqual(actual.toString(), 'rgb(255, 255, 255, 1)')
    })

    it('mixes correctly across the 0/255 threshold [test 2]', () => {
      const a = new ColorRgb(0.8, 0.8, 0.8)
      const b = new ColorRgb(0.1, 0.1, 0.1)
      const actual = a.mix(b)
      // mixes to 0.95, which is 242.25 in hex
      assert.strictEqual(
        actual.toString(),
        // floating point 🙄
        'rgb(242.25000000000003, 242.25000000000003, 242.25000000000003, 1)',
      )
    })

    it('mixes correctly inside 0-255 range [test 1]', () => {
      const a = new ColorRgb(0.7, 0.7, 0.7)
      const b = new ColorRgb(0.3, 0.3, 0.3)
      const actual = a.mix(b)
      // mixes to 0.5, which is 127.5 in hex
      assert.strictEqual(actual.toString(), 'rgb(127.5, 127.5, 127.5, 1)')
    })

    it('mixes correctly inside 0-255 range [test 2]', () => {
      const a = new ColorRgb(0.6, 0.6, 0.6)
      const b = new ColorRgb(0.2, 0.2, 0.2)
      const actual = a.mix(b)
      // mixes to 0.4, which is 102 in hex
      assert.strictEqual(actual.toString(), 'rgb(102, 102, 102, 1)')
    })

    it('returns input when mixing the same color', () => {
      const a = new ColorRgb(0.6, 0.6, 0.6)
      const b = new ColorRgb(0.6, 0.6, 0.6)
      const actual = a.mix(b)
      // mixes to 0.6, which is 153 in hex
      assert.strictEqual(actual.toString(), 'rgb(153, 153, 153, 1)')
    })

    it('returns input components when mixing the partial same color', () => {
      const a = new ColorRgb(0.6, 0.6, 0.4)
      const b = new ColorRgb(0.6, 0.6, 0.6)
      const actual = a.mix(b)
      // b is the only non-equal component, and it mixes to 0.5
      assert.strictEqual(actual.toString(), 'rgb(153, 153, 127.5, 1)')
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

describe('mixColorComponent', () => {
  const tests = [
    // "wrap" backwards from a to b
    { a: 0.1, b: 0.9, mix: 0.5, output: 1.0 },
    { a: 0.1, b: 0.9, mix: 0.25, output: 0.05 },
    { a: 0.1, b: 0.9, mix: 0.75, output: 0.95 },
    { a: 0.1, b: 0.9, mix: 0.0, output: 0.1 },
    { a: 0.1, b: 0.9, mix: 1.0, output: 0.9 },
    // wrap" forwards from b to a
    { a: 0.9, b: 0.1, mix: 0.5, output: 1.0 },
    { a: 0.9, b: 0.1, mix: 0.25, output: 0.95 },
    { a: 0.9, b: 0.1, mix: 0.75, output: 0.05 },
    { a: 0.9, b: 0.1, mix: 0.0, output: 0.9 },
    { a: 0.9, b: 0.1, mix: 1.0, output: 0.1 },
    // a < b, normal lerp
    { a: 0.3, b: 0.7, mix: 0.5, output: 0.5 },
    { a: 0.3, b: 0.7, mix: 0.25, output: 0.4 },
    { a: 0.3, b: 0.7, mix: 0.75, output: 0.6 },
    { a: 0.3, b: 0.7, mix: 0.0, output: 0.3 },
    { a: 0.3, b: 0.7, mix: 1.0, output: 0.7 },
    // a > b, normal lerp
    { a: 0.7, b: 0.3, mix: 0.5, output: 0.5 },
    { a: 0.7, b: 0.3, mix: 0.25, output: 0.6 },
    { a: 0.7, b: 0.3, mix: 0.75, output: 0.4 },
    { a: 0.7, b: 0.3, mix: 0.0, output: 0.7 },
    { a: 0.7, b: 0.3, mix: 1.0, output: 0.3 },
  ]

  for (const { a, b, mix, output } of tests) {
    it(`mixColorComponent(${a}, ${b}, ${mix}) === ${output}`, () => {
      // floating point math is annoying...
      const actual = mixColorComponent(a, b, mix)
      const result = isWithinError(output, 0.000001, actual)
      assert(result, `actual '${actual}' is not within 0.000001 of ${output}`)
    })
  }
})

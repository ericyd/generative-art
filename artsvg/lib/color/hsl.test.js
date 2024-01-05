import { describe, it } from 'node:test'
import assert from 'node:assert'
import { ColorHsl, mixColorComponent } from './hsl.js'
import { ColorRgb } from './rgb.js'
import { isWithinError } from '../math.js'

describe('ColorHsl', () => {
  describe('fromRgb', () => {
    const exactTests = [
      ['#000000', { h: 0, s: 0, l: 0 }],
      ['#ffffff', { h: 0, s: 0, l: 1 }],
    ]

    for (const [hex, hsl] of exactTests) {
      it(`converts ${hex} to ${JSON.stringify(hsl)}`, () => {
        const actual = ColorHsl.fromRgb(ColorRgb.fromHex(hex))
        assert.strictEqual(actual.h, hsl.h)
        assert.strictEqual(actual.s, hsl.s)
        assert.strictEqual(actual.l, hsl.l)
      })
    }

    const inexactTests = [['#555a99', { h: 236, s: 0.29, l: 0.47 }]]

    for (const [hex, hsl] of inexactTests) {
      it(`converts ${hex} to ${JSON.stringify(hsl)}`, () => {
        const actual = ColorHsl.fromRgb(ColorRgb.fromHex(hex))
        // Not sure why there is so much drift in hue
        assert(
          isWithinError(hsl.h, 0.5, actual.h),
          `actual.h ${actual.h} is not within 0.5 of ${hsl.h}`,
        )
        assert(
          isWithinError(hsl.s, 0.01, actual.s),
          `actual.s ${actual.s} is not within 0.05 of ${hsl.s}`,
        )
        assert(
          isWithinError(hsl.l, 0.01, actual.l),
          `actual.l ${actual.l} is not within 0.05 of ${hsl.l}`,
        )
      })
    }
  })

  describe('mix', () => {
    it('mixes simple colors correctly', () => {
      const a = new ColorHsl(0, 0, 0)
      const b = new ColorHsl(180, 1, 1)
      const actual = a.mix(b)
      assert.strictEqual(actual.toString(), 'hsl(90, 50%, 50%, 1)')
    })

    it('mixes correctly across the 0/360 threshold [test 1]', () => {
      const a = new ColorHsl(1, 0.5, 0.5)
      const b = new ColorHsl(359, 0.5, 0.5)
      const actual = a.mix(b)
      assert.strictEqual(actual.toString(), 'hsl(360, 50%, 50%, 1)')
    })

    it('mixes correctly across the 0/360 threshold [test 2]', () => {
      const a = new ColorHsl(10, 0.5, 0.5)
      const b = new ColorHsl(350, 0.5, 0.5)
      const actual = a.mix(b, 0.25)
      assert.strictEqual(actual.toString(), 'hsl(5, 50%, 50%, 1)')
    })

    it('mixes correctly inside 0-255 range [test 1]', () => {
      const a = new ColorHsl(200, 0.7, 0.7)
      const b = new ColorHsl(100, 0.3, 0.3)
      const actual = a.mix(b)
      assert.strictEqual(actual.toString(), 'hsl(150, 50%, 50%, 1)')
    })

    it('mixes correctly inside 0-255 range [test 2]', () => {
      const a = new ColorHsl(150, 0.6, 0.6)
      const b = new ColorHsl(250, 0.2, 0.2)
      const actual = a.mix(b)
      assert.strictEqual(actual.toString(), 'hsl(200, 40%, 40%, 1)')
    })

    it('returns input when mixing the same color', () => {
      const a = new ColorHsl(60, 0.6, 0.6)
      const b = new ColorHsl(60, 0.6, 0.6)
      const actual = a.mix(b)
      assert.strictEqual(actual.toString(), 'hsl(60, 60%, 60%, 1)')
    })

    it('returns input components when mixing the partial same color', () => {
      const a = new ColorHsl(60, 0.6, 0.4)
      const b = new ColorHsl(60, 0.6, 0.6)
      const actual = a.mix(b)
      // l is the only non-equal component, and it mixes to 0.5
      assert.strictEqual(actual.toString(), 'hsl(60, 60%, 50%, 1)')
    })
  })
})

describe('mixColorComponent', () => {
  const tests = [
    // "wrap" backwards from a to b
    { a: 1, b: 359, mix: 0.5, output: 360 },
    { a: 1, b: 359, mix: 0.25, output: 0.5 },
    { a: 1, b: 359, mix: 0.75, output: 359.5 },
    { a: 1, b: 359, mix: 0.0, output: 1 },
    { a: 1, b: 359, mix: 1.0, output: 359 },
    // wrap" forwards from b to a

    // TODO: fix these, use above as example
    { a: 359, b: 1, mix: 0.5, output: 360 },
    { a: 359, b: 1, mix: 0.25, output: 359.5 },
    { a: 359, b: 1, mix: 0.75, output: 0.5 },
    { a: 359, b: 1, mix: 0.0, output: 359 },
    { a: 359, b: 1, mix: 1.0, output: 1 },
    // a < b, normal lerp
    { a: 100, b: 200, mix: 0.5, output: 150 },
    { a: 100, b: 200, mix: 0.25, output: 125 },
    { a: 100, b: 200, mix: 0.75, output: 175 },
    { a: 100, b: 200, mix: 0.0, output: 100 },
    { a: 100, b: 200, mix: 1.0, output: 200 },
    // a > b, normal lerp
    { a: 200, b: 100, mix: 0.5, output: 150 },
    { a: 200, b: 100, mix: 0.25, output: 175 },
    { a: 200, b: 100, mix: 0.75, output: 125 },
    { a: 200, b: 100, mix: 0.0, output: 200 },
    { a: 200, b: 100, mix: 1.0, output: 100 },
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

import { describe, it } from 'node:test'
import assert from 'node:assert'
import { Rectangle, rect } from './rectangle.js'

describe('rect', () => {
  it('returns a Rectangle', () => {
    const r = rect({})
    assert(r instanceof Rectangle)
  })

  it('can accept attributes', () => {
    const r = rect({
      x: 10,
      y: 11,
      width: 12,
      height: 13,
    })
    assert.strictEqual(r.x, 10)
    assert.strictEqual(r.y, 11)
    assert.strictEqual(r.width, 12)
    assert.strictEqual(r.height, 13)
  })

  it('can accept a builder', () => {
    const r = rect((r) => {
      r.x = 10
      r.y = 11
      r.width = 12
      r.height = 13
    })
    assert.strictEqual(r.x, 10)
    assert.strictEqual(r.y, 11)
    assert.strictEqual(r.width, 12)
    assert.strictEqual(r.height, 13)
  })
})

describe('Rectangle', () => {
  it('has x, y, width, height props', () => {
    const r = new Rectangle()
    assert.notStrictEqual(r.x, undefined)
    assert.notStrictEqual(r.y, undefined)
    assert.notStrictEqual(r.width, undefined)
    assert.notStrictEqual(r.height, undefined)
  })

  it('has a center', () => {
    const r = new Rectangle({ x: 2, y: 2, width: 2, height: 2 })
    assert.strictEqual(r.center().x, 3)
    assert.strictEqual(r.center().y, 3)
  })
})

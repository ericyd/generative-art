import { describe, it } from 'node:test'
import { Grid, grid } from './grid.js'

describe('grid', () => {
  it.todo('should return an instance of Grid')
})

describe('Grid', () => {
  describe('iterator', () => {
    it.todo('should default to range x:[0, 1), y:[0, 1)')

    it.todo('should step x by xStep')

    it.todo('should step y by yStep')

    it.todo('should yield x range of [xMin, xMax)')

    it.todo('should yield y range of [yMin, yMax)')

    it.todo(
      'when columns is set, should yield x range of [xMin, xMin + columns)',
    )

    it.todo('when rows is set, should yield y range of [yMin, yMin + rows)')

    it.todo('should yield in row major order by default')

    it.todo('should yield in column major order when specified in constructor')
  })

  it.todo('should fill data store when fill is specified')

  it.todo(
    'should create data store of correct size when columns and rows are specified',
  )

  it.todo(
    'should create data store of correct size when xMin,xMax,yMin,yMax are specified',
  )

  it.todo('should ignore xStep and yStep when creating data store')

  describe('.get', () => {
    it.todo('should get values from discrete x/y args')

    it.todo('should get values from Vector2 arg')
  })

  describe('.set', () => {
    it.todo('should set values from discrete x/y args')

    it.todo('should set values from Vector2 arg')
  })
})

import assert from 'assert'
import { vec2 } from './vector2.js'
import * as math from './math.js'

/** Describe `smallestAngularDifference` */
// "12-o-clock" to "9-o-clock" = quarter turn anti-clockwise
assert.equal(math.smallestAngularDifference(-Math.PI / 2, Math.PI), -Math.PI / 2)
// argument order matters!!! 
// "9-o-clock" to "12-o-clock" = quarter turn clockwise
assert.equal(math.smallestAngularDifference(Math.PI, -Math.PI / 2), Math.PI / 2)
// same angle, different sign = 0
assert.equal(math.smallestAngularDifference(-Math.PI, Math.PI), 0)
assert.equal(math.smallestAngularDifference(Math.PI, -Math.PI), 0)
assert.equal(math.smallestAngularDifference(-Math.PI / 2, Math.PI * 3/2), 0)
assert.equal(math.smallestAngularDifference(Math.PI * 3/2, -Math.PI / 2), 0)
// very small relative differences, very large absolute differences
// have to use "isWithin" because of floating point rounding issues
let target = -Math.PI * 0.02
assert(math.isWithin(
  target - 0.001,
  target + 0.001,
  math.smallestAngularDifference(-Math.PI * 0.99, Math.PI * 0.99)
))

assert.equal(math.smallestAngularDifference(0.5, 2.75), 2.25)

/** Describe `angleOfVertex` */
target = Math.PI / 4
assert(math.isWithin(target - 0.001, target + 0.001, math.angleOfVertex(vec2(2, 0), vec2(0, 0), vec2(4, 4))))

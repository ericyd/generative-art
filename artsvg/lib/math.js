import { Vector2 } from "./vector2.js";

// some helpers to avoid the `Math.` namespace everywhere
export const cos = Math.cos
export const sin = Math.sin
export const tan = Math.tan
export const atan2 = Math.atan2

/**
 * Hypotenuse of a right triangle
 * @param {number} x 
 * @param {number} y 
 * @returns number
 */
export function hypot(x, y) {
  return Math.sqrt(x * x + y * y);
}

/**
 * Returns if a number is in a range
 * @param {number} min 
 * @param {number} max 
 * @param {number} value 
 * @returns {boolean}
 */
export function isWithin(min, max, value) {
  return value > min && value < max
}

/**
 * Returns if a number is in a range [target-error, target+error]
 * @param {number} target 
 * @param {number} error 
 * @param {number} value 
 * @returns {boolean}
 */
export function isWithinError(target, error, value) {
  return value > (target - error) && value < (target + error)
}

/**
 * For angles in a known range, e.g. [-PI, PI],
 * returns the angle (and direction) of the smallest angular difference between them.
 * The result will always assume traveling from `angle1` to `angle2`.
 * TODO: this function is wild... write more tests and figure out how to simplify
 * @param {Radians} angle1 
 * @param {Radians} angle2 
 * @returns {Radians}
 */
export function smallestAngularDifference(angle1, angle2) {
  // put both angles in -PI,PI range
  while (angle1 < -Math.PI) { angle1 += Math.PI * 2 }
  while (angle2 < -Math.PI) { angle2 += Math.PI * 2 }
  while (angle1 >  Math.PI) { angle1 -= Math.PI * 2 }
  while (angle2 >  Math.PI) { angle2 -= Math.PI * 2 }

  let adjustedAngle1 = angle1
  while (adjustedAngle1 < angle2) {
    adjustedAngle1 += Math.PI * 2
  }
  
  let adjustedAngle2 = angle2
  while (adjustedAngle2 < angle1) {
    adjustedAngle2 += Math.PI * 2
  }

  const min = Math.min(adjustedAngle2 - adjustedAngle1, adjustedAngle1 - adjustedAngle2)
  let directionalMin = adjustedAngle1 > adjustedAngle2 ? min : min * -1

  // the absolute value of the smallest angular rotation should always be < Math.PI
  while (directionalMin > Math.PI) {
    directionalMin -= Math.PI * 2
  }
  while (directionalMin < -Math.PI) {
    directionalMin += Math.PI * 2
  }
  // account for situations where the two values are the same "effective" angle
  return isWithin(Math.PI - 0.001, Math.PI + 0.001, directionalMin) ? 0 : directionalMin
}

/**
 * Three vertices define an angle.
 * Param `point2` is the vertex.
 * Params `point1` and `point3` are the two end points
 * 
 * Two formula for the same thing
 * cos-1 ( (a · b) / (|a| |b|) )
 * sin-1 ( |a × b| / (|a| |b|) )
 * @param {Vector2} point1 
 * @param {Vector2} point2 
 * @param {Vector2} point3 
 */
export function angleOfVertex(point1, point2, point3) {
  const a = point1.subtract(point2)
  const b = point3.subtract(point2)
  const lengthProduct = a.length() * b.length()
  return Math.acos(a.dot(b) / lengthProduct)
}

/**
 * @param {number} number1 
 * @param {number} number2 
 * @returns {boolean}
 */
export function haveSameSign(number1, number2) {
  return number1 < 0 === number2 < 0
}

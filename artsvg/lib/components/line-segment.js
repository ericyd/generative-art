import { Vector2 } from '../vector2.js'
import { path, Path } from './path.js'

/**
 * @param {Vector2} start
 * @param {Vector2} end
 * @returns {Path}
 */
export function lineSegment(start, end) {
  return path((p) => {
    p.moveTo(start)
    p.lineTo(end)
  })
}

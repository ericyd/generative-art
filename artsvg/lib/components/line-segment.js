import { Vector2 } from '../vector2.js'
import { PathInstruction } from './path.js'
import { Tag } from './tag.js'

export class LineSegment extends Tag {
  /**
   * @param {Vector2} start
   * @param {Vector2} end
   */
  constructor(start, end) {
    super('path', {
      d: [new PathInstruction('M', [start]).render(), new PathInstruction('L', [end]).render()].join(' '),
    })
    this.start = start
    this.end = end
  }
}

/**
 * @param {Vector2} start
 * @param {Vector2} end
 * @returns {LineSegment}
 */
export function lineSegment(start, end) {
  return new LineSegment(start, end)
}

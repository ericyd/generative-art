import { Tag } from './tag.js'
import { Circle } from './circle.js'

/**
 * @typedef {object} SvgAttributes
 * @property {number} width
 * @property {number} height
 */

export class Svg extends Tag {
  /**
   * @param {SvgAttributes} attributes
   */
  constructor({ width = 100, height = 100, ...attributes } = {}) {
    super('svg', {
      viewBox: attributes.viewBox ?? `0 0 ${width} ${height}`,
      preserveAspectRatio: attributes.preserveAspectRatio ?? 'xMidYMid meet',
      xmlns: 'http://www.w3.org/2000/svg',
      'xmlns:xlink': 'http://www.w3.org/1999/xlink',
      width,
      height,
      ...attributes,
    })
  }

  // TODO: find a more generic way of expressing this "instance or builder" patterh
  /**
   * @param {Path | (path: Path) => void} pathOrBuilder
   */
  path(pathOrBuilder) {
    return pathOrBuilder instanceof Path
      ? this.addChild(pathOrBuilder)
      : this.childBuilder(pathOrBuilder, Path)
  }

  /**
   * @param {Circle | (circle: Circle) => void} circleOrBuilder
   */
  circle(circleOrBuilder) {
    return circleOrBuilder instanceof Circle
      ? this.addChild(circleOrBuilder)
      : this.childBuilder(circleOrBuilder, Circle)
  }
}

/**
 * @param {SvgAttributes}
 * @param {(svg: Svg) => void}
 */
export function svg(attributes, builder) {
  const s = new Svg(attributes)
  builder(s)
  return s
}

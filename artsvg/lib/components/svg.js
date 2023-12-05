import { Tag } from './tag.js'
import { Circle } from './circle.js'
import { Path } from './path.js'

/**
 * @typedef {object} SvgAttributes
 * @property {number} width
 * @property {number} height
 * @property {number} scale allows the resulting SVG to have larger dimensions, which still keeping the viewBox the same as the `width` and `height` attributes
 */

export class Svg extends Tag {
  /**
   * @param {SvgAttributes} attributes
   */
  constructor({ width = 100, height = 100, scale = 1, ...attributes } = {}) {
    super('svg', {
      viewBox: attributes.viewBox ?? `0 0 ${width} ${height}`,
      preserveAspectRatio: attributes.preserveAspectRatio ?? 'xMidYMid meet',
      xmlns: 'http://www.w3.org/2000/svg',
      'xmlns:xlink': 'http://www.w3.org/1999/xlink',
      width: width * scale,
      height: height * scale,
      ...attributes,
    })
    this.width = width
    this.height = height
  }

  // TODO: find a more generic way of expressing this "instance or builder" patterh
  // TODO: need more overloads, because attributes should actually be first arg... or passed to the callback???
  /**
   * @param {Path | (path: Path) => void} pathOrBuilder
   * @param {import('./path.js').PathAttributes} attributes
   */
  path(pathOrBuilder, attributes) {
    return pathOrBuilder instanceof Path
      ? this.addChild(pathOrBuilder)
      : this.childBuilder(pathOrBuilder, Path, attributes)
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
 * @param {SvgAttributes} attributes
 * @param {(svg: Svg) => void} builder
 */
export function svg(attributes, builder) {
  const s = new Svg(attributes)
  builder(s)
  return s
}

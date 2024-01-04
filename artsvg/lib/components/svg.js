import { Tag } from './tag.js'
import { Circle, circle } from './circle.js'
import { Path, path } from './path.js'
import { Rectangle, rect } from './rectangle.js'
import { LineSegment } from './line-segment.js'
import { ColorRgb } from '../color/rgb.js'

/**
 * @typedef {object} SvgAttributes
 * @property {number} [width=100]
 * @property {number} [height=100]
 * @property {number} [scale=1] Allows the resulting SVG to have larger dimensions, which still keeping the viewBox the same as the `width` and `height` attributes
 * @property {string} [viewBox] Defaults to `0 0 width height`
 * @property {string} [preserveAspectRatio] Defaults to `xMidYMid meet`
 */

/**
 * @class Svg
 * @description The root of any SVG document.
 * Although you can construct this class manually, it's much nicer to the the `svg` builder function,
 * or the `renderSvg` function if you're running this locally or on a server.
 * @example
 * ```js
 * import { svg, vec2 } from 'artsvg'
 *
 * const document = svg({ width: 100, height: 100, scale: 5 }, (doc) => {
 *   doc.fill = null
 *   doc.strokeWidth = 1
 *   doc.path((p) => {
 *     p.fill = '#ab9342'
 *     p.stroke = '#000'
 *     p.strokeWidth = 2
 *     p.moveTo(vec2(0, 0))
 *     p.lineTo(vec2(doc.width, doc.height))
 *     p.lineTo(vec2(doc.width, 0))
 *     p.close()
 *   })
 * })
 *
 * console.log(document.render())
 * ```
 */
export class Svg extends Tag {
  /**
   * @param {SvgAttributes} [attributes={}]
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
    /** @type {Record<string, string | number> | null} */
    this.filenameMetadata = null
  }

  // TODO: find a more generic way of expressing this "instance or builder" pattern
  /**
   * @param {Path | ((path: Path) => void)} pathOrBuilder
   */
  path(pathOrBuilder) {
    return pathOrBuilder instanceof Path
      ? this.addChild(pathOrBuilder)
      : this.addChild(path(pathOrBuilder))
  }

  /**
   * @param {LineSegment} lineSegment
   */
  lineSegment(lineSegment) {
    return this.addChild(lineSegment)
  }

  /**
   * TODO: "or builder" can be anything accepted by the "circle" helper
   * TODO: add overload type defs
   * @param {Circle | ((circle: Circle) => void)} circleOrBuilder
   */
  circle(circleOrBuilder) {
    return circleOrBuilder instanceof Circle
      ? this.addChild(circleOrBuilder)
      : this.addChild(circle(circleOrBuilder))
  }

  /**
   * @param {Rectangle | ((rect: Rectangle) => void)} rectOrBuilder
   */
  rect(rectOrBuilder) {
    return rectOrBuilder instanceof Rectangle
      ? this.addChild(rectOrBuilder)
      : this.addChild(rect(rectOrBuilder))
  }

  /**
   * Generates filename metadata when running in a render loop
   * @returns {string}
   */
  formatFilenameMetadata() {
    return Object.entries(this.filenameMetadata ?? {})
      .map(([key, value]) => `${key}-${value}`)
      .join('-')
  }

  /**
   * @param {string | ColorRgb} color 
   */
  setBackground(color) {
    const rect = new Rectangle({
      x: 0,
      y: 0,
      width: this.width,
      height: this.height,
    })
    rect.stroke = null
    rect.fill = typeof color === 'string' ? color : color.toString()
    this.children.unshift(rect)
  }
}

/**
 * @callback SvgBuilder
 * @param {Svg} svg
 * @returns {void | SvgBuilderPostLoop}
 */

/**
 * @callback SvgBuilderPostLoop
 * @description A callback which will be run after ever render loop.
 * Useful to trigger side-effects like setting up new values for global variables such as seeds.
 * Similar to a "cleanup" function returned from React's useEffect hook.
 * @returns {void}
 */

/**
 * @param {SvgAttributes} attributes
 * @param {SvgBuilder} builder
 */
export function svg(attributes, builder) {
  const s = new Svg(attributes)
  builder(s)
  return s
}

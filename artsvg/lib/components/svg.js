import { Tag } from './tag.js'
import { Circle } from './circle.js'
import { Path } from './path.js'

/**
 * @typedef {object} SvgAttributes
 * @property {number} width
 * @property {number} height
 * @property {number} scale Allows the resulting SVG to have larger dimensions, which still keeping the viewBox the same as the `width` and `height` attributes
 * @property {SvgBuilder?} builder If the SVG is initialized by a builder, it will be run every time `render` is called.
 * This allows the builder to respond to variables that might have changed in a higher scope, such as a seed value.
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
    /** @type {Record<string, string | number>} */
    this.filenameMetadata = null
  }

  /** @param {'none' | string | null} value */
  set fill(value) {
    const fill = value === null ? 'none' : value
    this.setAttributes({ fill })
    return fill
  }

  /** @param {'none' | string | null} value */
  set stroke(value) {
    const stroke = value === null ? 'none' : value
    this.setAttributes({ stroke })
    return stroke
  }

  /** @param {number} value */
  set strokeWidth(value) {
    this.setAttributes({ 'stroke-width': value })
    return value
  }

  // TODO: find a more generic way of expressing this "instance or builder" pattern
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

  /**
   * Generates filename metadata when running in a render loop
   * @returns {string}
   */
  formatFilenameMetadata() {
    return Object.entries(this.filenameMetadata ?? {})
      .map(([key, value]) => `${key}-${value}`)
      .join('-')
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

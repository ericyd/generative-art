import { pickBy } from '../util.js'

/**
 * @property {string} tagName
 * @property {object} attributes
 * @property {Array<Tag>} children
 */
export class Tag {
  constructor(tagName, attributes = {}) {
    this.tagName = tagName
    this.attributes = attributes
    this.children = []
  }

  /**
   * @param {Record<string, unknown>} attributes
   */
  setAttributes(attributes) {
    this.attributes = {
      ...this.attributes,
      ...attributes,
    }
  }

  // TODO: consider using a Proxy to set aribtrary attributes using camelCase kebab-case transitions
  /** @param {'none' | string | null} value */
  set fill(value) {
    const fill = value === null ? 'none' : value
    this.setAttributes({ fill })
  }

  /** @param {'none' | string | null} value */
  set stroke(value) {
    const stroke = value === null ? 'none' : value
    this.setAttributes({ stroke })
  }

  /** @param {number} value */
  set strokeWidth(value) {
    this.setAttributes({ 'stroke-width': value })
  }

  #visualAttributesTestFn(value, key) {
    return (
      ['fill', 'stroke', 'stroke-width'].includes(key) && value !== undefined
    )
  }

  /**
   * @protected
   * Returns an object containing the core "visual styles" that should be inherited
   * as children are added to the document.
   * @returns {Record<string, string}
   */
  visualAttributes() {
    return pickBy(this.#visualAttributesTestFn, {
      fill: this.attributes.fill,
      stroke: this.attributes.stroke,
      'stroke-width': this.attributes['stroke-width'],
    })
  }

  /**
   * @protected
   * Sets visual attributes on the current tag, favoring any values that have been set explicitly
   * @param {Record<string, string>} incoming
   * @returns {void}
   */
  setVisualAttributes(incoming = {}) {
    this.setAttributes({
      ...pickBy(this.#visualAttributesTestFn, incoming),
      ...this.visualAttributes(),
    })
  }

  /**
   * @protected
   * @param {Tag} child
   */
  addChild(child) {
    child.setVisualAttributes(this.visualAttributes())
    this.children.push(child)
    return child
  }

  /**
   * @protected
   * @param {(tag: Tag) => void} builder
   * TODO: not sure if this is correct typing, I want to pass a constructor
   * @param {typeof Tag} ConstructableTag
   * @returns {Tag}
   */
  childBuilder(builder, ConstructableTag) {
    const p = new ConstructableTag()
    builder(p)
    return this.addChild(p)
  }

  #formatAttributes() {
    return Object.entries(this.attributes)
      .map(([key, value]) => `${key}="${value}"`)
      .join(' ')
  }

  render() {
    return [
      `<${this.tagName} ${this.#formatAttributes()}>`,
      this.children.map((child) => child.render()).join(''),
      `</${this.tagName}>`,
    ].join('')
  }
}

/**
 * @param {(tag: Tag) => void} builder
 * @returns {Tag}
 */
export function tag(builder) {
  const t = new Tag()
  builder(t)
  return t
}

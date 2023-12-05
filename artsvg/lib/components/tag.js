/**
 * @property {string} tagName
 * @property {object} attributes
 * @property {Array<Tag>} children
 */
class Tag {
  #attributes = {}
  constructor(tagName, attributes = {}) {
    this.tagName = tagName
    this.#attributes = attributes
    this.children = []
  }

  set attributes(attributes) { this.#attributes = attributes }

  /**
   * @param {Record<string, unknown>} attributes
   */
  setAttributes(attributes) {
    this.#attributes = {
      ...this.#attributes,
      ...attributes
    }
  }

  #formatAttributes() {
    return Object.entries(this.#attributes)
      .map(([key, value]) => `${key}="${value}"`)
      .join(" ");
  }

  render() {
    return [
       `<${this.tagName} ${this.#formatAttributes()}>`,
       this.children.map((child) => child.render()).join(""),
       `</${this.tagName}>`,
     ].join("")
  }
}

/**
 * @typedef {object} SvgAttributes
 * @property {object} attributes
 * @property {number} attributes.width
 * @property {number} attributes.height
 */
export class Svg extends Tag {
  /**
   * @param {SvgAttributes} attributes
   */
  constructor(attributes = { width: 100, height: 100 }) {
    super('svg', {
      viewBox: attributes.viewBox ?? `0 0 ${attributes.width} ${attributes.height}`,
      preserveAspectRatio: attributes.preserveAspectRatio ?? "xMidYMid meet",
      xmlns: "http://www.w3.org/2000/svg",
      "xmlns:xlink": "http://www.w3.org/1999/xlink",
      ...attributes
    })
  }

  /**
   * @param {Tag} child
   */
  #addChild(child) {
    this.children.push(child)
  }
  /**
   * @param {(path: Path) => void} builder
   * TODO: not sure if this is correct typing, I want to pass a constructor
   * @param {typeof Tag} ConstructableTag
   * @returns {Path}
   */
  #childBuilder(builder, ConstructableTag) {
    const p = new ConstructableTag()
    builder(p)
    this.children.push(p)
    return p
  }
  /**
   * @param {Path | (path: Path) => void} pathOrBuilder
   */
  path(pathOrBuilder) {
    return pathOrBuilder instanceof Path
      ? this.#addChild(pathOrBuilder)
      : this.#childBuilder(pathOrBuilder, Path)
  }

  /**
   * @param {Circle | (circle: Circle) => void} circleOrBuilder
   */
  circle(circleOrBuilder) {
    return circleOrBuilder instanceof Circle
      ? this.#addChild(circleOrBuilder)
      : this.#childBuilder(circleOrBuilder, Circle)
  }
}

/**
 * @param {SvgAttributes}
 * @param {(svg: Svg) => void}
 */
export function svg(attributes = {}, builder) {
  const s = new Svg(attributes)
  builder(s)
  return s
}

/**
 * @typedef {object} CircleAttributes
 * @property {object} attributes
 * @property {number} attributes.x
 * @property {number} attributes.y
 * @property {number} attributes.radius
 */
export class Circle extends Tag {
  /**
   * @param {CircleAttributes} attributes
   */
  constructor(attributes = {}) {
    super('circle', {
      cx: attributes.x,
      cy: attributes.y,
      r: attributes.radius,
    })
    this.x = attributes.x
    this.y = attributes.y
    this.radius = attributes.radius
  }

  set x(value) {
    this.setAttributes({ cx: value })
    return value
  }

  set y(value) {
    this.setAttributes({ cy: value })
    return value
  }

  set radius(value) {
    this.setAttributes({ r: value })
    return value
  }
}

/**
 * @param {CircleAttributes | [x: number, y: number, radius: number] | (circle: Circle) => void} circleOrBuilder
 */
export function circle(...args) {
  if (typeof args[0] === 'function') {
    const c = new Circle()
    args[0](c)
    return c
  } else if (args.length === 1) {
    return new Circle(args[0])
  } else if (args.length === 3) {
    return new Circle({ x: args[0], y: args[1], radius: args[2] })
  } else {
    throw new Error(`Unable to construct circle from "${args.join(', ')}"`)
  }
}

/**
 *
 * @param tagName
 * @param {object} attrs
 * @param {tag[]} children
 */
export function tag(tagName, attrs = {}, children = []) {
  const formatAttrs = () =>
    Object.entries(attrs)
      .map(([key, value]) => `${key}="${value}"`)
      .join(" ");
  return {
    appendChild: (child) => tag(tagName, attrs, [...children, child]),
    withChildren: (newChildren) => tag(tagName, attrs, [...children, ...newChildren]),
    withAttrs: (newAttrs) => tag(tagName, {...attrs, ...newAttrs}, children),
    draw: () =>
      [
        `<${tagName} ${formatAttrs()}>`,
        children.map((child) => child.draw()).join(""),
        `</${tagName}>`,
      ].join(""),
  };
}

/**
 * @param {string} tagName
 * @returns function which returns a tag
 */
export function namedTag(tagName) {
  /**
   * @param {object} attrs
   * @param {tag[]} children
   */
  return (attrs = {}, children = []) => tag(tagName, attrs, children)
}

//export const Circle = namedTag('circle')
export const Rect = namedTag('rect')
export const Filter = namedTag('filter')
export const Path = namedTag('path')

// defs don't need attrs
export function Defs(...children) {
  return tag('defs', {}, children)
}

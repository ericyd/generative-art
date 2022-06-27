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

/**
 * @param {object} attrs
 * @param {tag[]} children
 */
export function svg(attrs = {}, children = []) {
  return tag(
    "svg",
    {
      ...attrs,
      viewBox: attrs.viewBox ?? "0 0 100 100",
      preserveAspectRatio: attrs.preserveAspectRatio ?? "xMidYMid meet",
      xmlns: "http://www.w3.org/2000/svg",
      "xmlns:xlink": "http://www.w3.org/1999/xlink"
    },
    children
  );
}

export const Circle = namedTag('circle')
export const Rect = namedTag('rect')
export const Filter = namedTag('filter')
export const Path = namedTag('path')

// defs don't need attrs
export function Defs(...children) {
  return tag('defs', {}, children)
}

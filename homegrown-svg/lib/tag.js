/**
 *
 * @param tagName
 * @param attrs
 * @param children
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

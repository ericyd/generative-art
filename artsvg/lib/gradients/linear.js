import { tag } from '../tag.js'
import { clamp } from '../util.js'

export function linearGradientStop(offset, color, opacity = 1) {
  if (offset < 0 || offset > 100) {
    console.warn(`offset '${offset}' is not in range [0, 100]`)
    offset = clamp(0, 100, offset)
  }
  return tag('stop', {
    offset: `${offset}%`,
    'stop-color': color,
    'stop-opacity': opacity,
  })
}

export function linearGradientDirection(x0, x1, y0, y1) {
  return {
    x0: `${x0}%`,
    x1: `${x1}%`,
    y0: `${y0}%`,
    y1: `${y1}%`,
  }
}

/**
 * @param {object} attrs
 * @param {linearGradientStop[]} stops
 * @returns
 */
export function LinearGradient(attrs = {}, stops = []) {
  return tag('linearGradient', { id: 'linear-gradient', ...attrs }, stops)
}

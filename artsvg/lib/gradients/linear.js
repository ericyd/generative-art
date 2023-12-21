/* TODO: this file was never migrated to the new API
import { tag } from '../tag.js'
import { clamp } from '../util.js'

/**
 * @param {*} offset 
 * @param {*} color 
 * @param {*} opacity 
 * @returns 
 * /
export function linearGradientStop(offset, color, opacity = 1) {
  const clampedOffset =
    offset < 0 || offset > 100
      ? console.warn(`offset '${offset}' is not in range [0, 100]`) ??
        clamp(0, 100, offset)
      : offset
  return tag('stop', {
    offset: `${clampedOffset}%`,
    'stop-color': color,
    'stop-opacity': opacity,
  })
}

/**
 * @param {*} x0 
 * @param {*} x1 
 * @param {*} y0 
 * @param {*} y1 
 * @returns 
 * /
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
 * / 
export function LinearGradient(attrs = {}, stops = []) {
  return tag('linearGradient', { id: 'linear-gradient', ...attrs }, stops)
}
*/

// utility functions intended only for internal use

/**
 * @template T
 * @param {string} message 
 * @param {T} defaultValue
 * @returns {T}
 */
export function warnWithDefault(message, defaultValue) {
  console.warn(message)
  return defaultValue
}

/**
 * more useful than an IIFE
 * @param {string} message 
 */
export function error(message) {
  throw new Error(message)
}

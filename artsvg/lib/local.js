/**
 * Helper functions for running this framework locally as an art platform
 */

import { writeFileSync } from 'node:fs'
import { execSync } from 'node:child_process'
import { basename, extname } from 'node:path'
import { Svg } from './components/index.js'

/**
 * @typedef {object} RenderLoopOptions
 * @property {number} loopCount number of times the render loop will run. Each loop will write the SVG to a file and open it if `open` is true.
 */

/**
 * @param {number} seed 
 * @param {Svg} svg 
 * @param {RenderLoopOptions} options
 */
export function renderLoop(seed, svg, { loopCount = 1, openEverFrame = true } = {}) {
  if (!seed) throw new Error('seed is required')
  if (!svg || !(svg instanceof Svg)) throw new Error('svg is required')
  let loops = 0
  while (loops < loopCount) {
    loops++
    const sketchFilename = basename(process.argv[1], extname(process.argv[1]))
    const filename = `screenshots/svg-${sketchFilename}-seed-${seed}.svg`
    writeFileSync(filename, svg.render())
    if (openEverFrame) {
      execSync(`open "${filename}"`)
    }
    console.log(`Rendered ${filename}`)
  }
}

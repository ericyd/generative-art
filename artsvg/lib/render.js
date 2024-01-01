/**
 * Helper functions for running this framework locally as an art platform.
 * 
 * This file is exported separately from the rest of the lib to isolate Node.js dependencies.
 */

import { mkdirSync, writeFileSync } from 'node:fs'
import { execSync } from 'node:child_process'
import { basename, extname } from 'node:path'
import { Svg } from './components/index.js'

const NOOP = () => {}

/**
 * @typedef {object} RenderLoopOptions
 * @property {number} loopCount number of times the render loop will run. Each loop will write the SVG to a file and open it if `open` is true.
 * @property {boolean} openEveryFrame Opens the rendered SVG after every frame using the system `open` command
 * @property {boolean} logFilename Logs the filename to "console.log" after every frame
 * @property {import('@resvg/resvg-js').Resvg} [Resvg]
 * @property {import('@resvg/resvg-js').ResvgRenderOptions} [ResvgOptions]
 */

/**
 * @param {Date} [d=new Date()] 
 * @returns {string}
 */
export function timestamp(d = new Date()) {
  return d.toISOString().replace(/[^a-zA-Z0-9]/g, '-')
}

/**
 * 
 * @param {import('./components/svg.js').SvgAttributes & RenderLoopOptions} config 
 * @param {import('./components/svg.js').SvgBuilder} builder 
 * @returns {string} the most recent rendered SVG
 */
export function renderSvg({ loopCount = 1, openEveryFrame = true, logFilename = true, Resvg, ResvgOptions, ...svgAttributes}, builder) {
  let loops = 0
  let rendered = ''
  while (loops < loopCount) {
    const svg = new Svg(svgAttributes)
    loops++
    const sketchFilename = basename(process.argv[1], extname(process.argv[1]))
    mkdirSync(`screenshots/${sketchFilename}`, { recursive: true })
    const postLoop = builder(svg) ?? NOOP
    const filenameBase = `screenshots/${sketchFilename}/${timestamp()}-${svg.formatFilenameMetadata()}`
    const filename = (extension = 'svg') => `${filenameBase}.${extension}`
    rendered = svg.render()
    writeFileSync(filename('svg'), rendered)

    if (Resvg) {
      // @ts-expect-error not sure why Resvg type has no constructable signatures
      const resvg = new Resvg(rendered, ResvgOptions ?? {})
      console.log(JSON.stringify(ResvgOptions))
      const pngData = resvg.render()
      const pngBuffer = pngData.asPng()
      writeFileSync(filename('png'), pngBuffer)
    }

    if (openEveryFrame) {
      execSync(`open "${filename('svg')}"`)
    }
    if (logFilename) {
      console.log(`Rendered ${filename('svg')}`)
      if (Resvg) {
        console.log(`Rendered ${filename('png')}`)
      }
    }
    postLoop()
  }
  return rendered
}

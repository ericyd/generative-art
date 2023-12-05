/**
 * This is perhaps not a runnable file right now.
 * The purpose of this is to envision what my "ideal" API could be
 */

// TODO: create a convenience method for "write svg" or something like that
import { writeFileSync } from 'node:fs'
import { basename, extname } from 'node:path'
import { svg, circle, Circle } from 'artsvg'

const config = {
  background: '#fff',
  stroke: '#000',
  width: 100,
  height: 100,
}

//let seed = Math.floor(random(1.0, Number.MAX_SAFE_INTEGER))
let seed = Math.floor(Math.random())

const canvas = svg(config, (canvas) => {
//  const rng = new Random(seed)

//  canvas.strokeWeight = 0.5
//  canvas.rectangle(Rectangle(x, y, width, height))

  // instance pattern
//  const prebuildPath = Path.fromPoints([vec2(0.0, 1.0), vec2(1.0, 2.0)])
//  canvas.path(prebuiltPath)

  // builder pattern
//  canvas.path((path) => {
//    path.moveTo(x, y)
//    for (let x = 0; x < width; x++) {
//      path.lineTo(x, canvas.height / 2)
//    }
//  })

  // add circle with explicit constructor
  canvas.circle(new Circle({ x: 10, y: 10, radius: 10 }))
  // add circle with helper function x,y,radius args
  canvas.circle(circle(30, 30, 10))
  // add circle with helper function CircleAttributes arg
  canvas.circle(circle({ x: 50, y: 50, radius: 10 }))
  // create circle via builder
  const c = circle((c) => {
    c.x = 70
    c.y = 70
    c.radius = 10
  })
  // add circle built via builder
  canvas.circle(c)
  // add circle directly via builder
  canvas.circle((c) => {
    c.x = 90
    c.y = 90
    c.radius = 10
  })
})

// capture screenshot and loop

/**
 * TODO
 * 1. make 2 convenience methods
 *    a. "open preview" - perhaps just `execSync(`open ${base64(svg)}`)`
 *    b. "writeFile" which simplifies the filenaming, and console logs when a file is written 
 * 2. consider using https://github.com/sindresorhus/uint8array-extras instead of buffer
 * 3. structure API better and add more tag classes
 */
while (true) {
  const originalSeed = seed
  const filename = `screenshots/svg-${basename(process.argv[1], extname(process.argv[1]))}-seed-${seed}.svg`
  if (true) {

    writeFileSync(filename, canvas.render())
//    seed = Math.floor(random(1.0, Number.MAX_SAFE_INTEGER))
    console.log(`saved to ${filename}`)
    // TODO: do this only once I think???
    // execSync(`open ${filename}`)
  }
  // preview if not looping and generating variants
  if (seed === originalSeed) {
    // open(svgToPng(canvas.render()))
  }
  // probably would not do this normally
  process.exit(0)
}

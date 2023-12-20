/**
 * Not a very interesting sketch, this was just added so I could experiment with how I wanted the lib API to work
 */

import {
  circle,
  Circle,
  createRng,
  random,
  randomSeed,
} from 'artsvg'
import { renderSvg } from 'artsvg/render'

const config = {
  background: '#fff',
  stroke: '#000',
  width: 100,
  height: 100,
  loopCount: 2,
}

let seed = randomSeed()

renderSvg(config, (canvas) => {
  canvas.filenameMetadata = { seed }
  const rng = createRng(seed)

  // add circle with explicit constructor
  canvas.circle(new Circle({ x: 10, y: 10, radius: 10 }))
  // add circle with helper function x,y,radius args
  canvas.circle(
    circle(
      random(0, canvas.width, rng),
      random(0, canvas.width, rng),
      random(5, 15, rng),
    ),
  )
  // add circle with helper function CircleAttributes arg
  canvas.circle(circle({ x: 50, y: 50, radius: 10 }))

  // create circle via builder
  const c = circle((c) => {
    c.x = random(0, canvas.width, rng)
    c.y = random(0, canvas.width, rng)
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

  return () => {
    seed = randomSeed()
  }
})

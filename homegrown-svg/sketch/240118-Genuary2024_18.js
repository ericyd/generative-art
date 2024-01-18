/**
 * Genuary 2024, Day 18
 * https://genuary.art/prompts
 *
 * """
 * JAN. 18 (credit: Chris Barber)
 *
 * Bauhaus.
 * """
 */
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 10,
  loopCount: 10,
}

let seed = randomSeed()

const colors = [
  '#72AAD2',
  '#D56666',
  '#F6D851',
  '#E77D31',
  '#4C5E9E',
  '#000000',
]

const orientations = {
  NScW: {
    name: 'north-south concave west',
    rotation: PI/2,
  },
  NScE: {
    name: 'north-south concave east',
    rotation: PI*3/2,
  },
  EWcN: {
    name: 'east-west concave north',
    rotation: PI,
  },
  EWcS: {
    name: 'east-west concave south',
    rotation: 0,
  }
}

/**
 * Rules
 * 1. Semi-circles can be oriented in 4 directions: north-south with convex west (NScW), north-south with convex east (NScE), east-west with convex north (EWcN), east-west with convex south (EWcS)
 * 2. Semi-circles can be filled with solid, or lines at fixed radii increments, chosen randomly
 * 3. Semi-circles must be placed touching another semi-circle. The rules for placement are
 *    a. The semi-circle cannot be oriented in the same way as the "parent" semi-circle
 *    b. If the semi-circle is rotated PI from the "parent" orientation, then the edges must touch
 *    c. If the semi-circle is rotation PI/2 or PI*3/2 from the "parent" orientation, then the outer curve must touch the flat edge of the parent semi-circle
 *    d. Semi-circle placement should be based on PHI, phi, or 1-phi of the parent diameter
 */


/*
this is an interesting start,
 but something isn't right

  i think i need to be more structured with the rules, less randomizing the next orientation

  I'm not really sure what the rules are supposed to be though
*/

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)
  const bg = '#F6E2BA'
  svg.setBackground(bg) // #eda is also nice
  svg.numericPrecision = 3

  for (let ii = 0; ii < 3; ii++) {
    let bounds
    if (ii === 0) {
      bounds = new Rectangle({ x: 0, y: 0, width: svg.width * 0.5, height: svg.height * 0.5 })
    } else if (ii === 1) {
      bounds = new Rectangle({ x: svg.width * 0.5, y: 0, width: svg.width * 0.5, height: svg.height * 0.5 })
    } else if (ii === 2) {
      bounds = new Rectangle({ x: 0, y: svg.height * 0.5, width: svg.width * 0.5, height: svg.height * 0.5 })
    } else {
      bounds = new Rectangle({ x: svg.width * 0.5, y: svg.height * 0.5, width: svg.width * 0.5, height: svg.height * 0.5 })
    }
    
    let radius = random(20, 40, rng)
    let orientation = randomFromObject(orientations, rng)
    // let center = svg.center.add(vec2(cos(orientation.rotation + PI*3/2), sin(orientation.rotation + PI*3/2)).multiply(radius / 4))
    let center = bounds.center.jitter(5, rng) // Vector2.random(bounds.x, bounds.x + bounds.width, bounds.y, bounds.y + bounds.height, rng)
    // let center = Vector2.random(0, svg.width, 0, svg.height, rng) //  svg.center.add(vec2(cos(orientation.rotation + PI*3/2), sin(orientation.rotation + PI*3/2)).multiply(radius / 2))
    let shape = semiCircle(center, orientation, radius, rng)
    svg.path(shape.path)
    for (let i = 0; i < 2; i++) {
      const nextRadius = shape.radius > 50 ? shape.radius * PHI/10 : random(0, 1, rng) < 0.9 ? shape.radius : shape.radius * (1 - PHI)

      // "choose an orientation different from the parent"
      // good idea, but not so interesting
      // while (true) {
      //   const next = randomFromObject(orientations, rng)
      //   const isFlipped = Math.abs(orientation.rotation - next.rotation) === PI
      //   if (next.rotation !== orientation.rotation) {
      //     center = center.add(
      //       vec2(cos(orientation.rotation), sin(orientation.rotation)).multiply((shape.radius - nextRadius) / 4)
      //     )
      //     if (!isFlipped) {
      //       center = center.add(
      //         vec2(cos(next.rotation), sin(next.rotation)).multiply(nextRadius)
      //       )
      //     }
      //     orientation = next
      //     break
      //   }
      // }

      const next = randomFromObject(orientations, rng)
      const isFlipped = Math.abs(orientation.rotation - next.rotation) === PI || Math.abs(orientation.rotation - next.rotation) === 0
      
      if (isFlipped) {
        center = center.add(
          vec2(cos(orientation.rotation), sin(orientation.rotation)).multiply((shape.radius - nextRadius) / 4)
        )
      } else {
        center = center.add(
          vec2(cos(next.rotation), sin(next.rotation)).multiply(nextRadius)
        )
      }
      orientation = next
      
      shape = semiCircle(center, orientation, nextRadius, rng)
      svg.path(shape.path)
    }
  }


  // add border
  // svg.rect(rect({ x: 0, y: 0, width: svg.width, height: svg.height, fill: 'none', stroke: bg, 'stroke-width': 10 }))

  return () => { seed = randomSeed() }  
})

function semiCircle(center, orientation, radius, rng) {
  const color = randomFromArray(colors, rng)
  const filled = radius < 5 || random(0, 1, rng) < 0.7
  const vector = vec2(cos(orientation.rotation), sin(orientation.rotation))
  const start = center.subtract(vector.multiply(radius))
  const end = center.add(vector.multiply(radius))
  const p = path(p => {
    p.fill = filled ? color : null
    p.stroke = filled ? null : color

    if (filled) {
      p.moveTo(start)
      p.arc({ rx: radius, ry: radius, end })
      p.close()
    } else {
      for (let r = 5 - (radius % 5); r < (radius + 1); r += 5) {
        p.moveTo(center.subtract(vector.multiply(r)))
        p.arc({ rx: r, ry: r, end: center.add(vector.multiply(r)) })
      }
    }
  })
  return {
    path: p,
    start,
    end,
    radius,
    orientation
  }
}
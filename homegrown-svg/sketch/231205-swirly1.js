/**
 * This is perhaps not a runnable file right now.
 * The purpose of this is to envision what my "ideal" API could be
 */

import { svg, circle, Circle, renderLoop, vec2, map } from 'artsvg'

const config = {
  background: '#fff',
  fill: 'none',
  stroke: '#000',
  width: 100,
  height: 100,
  scale: 10,
}

let seed = Math.floor(Math.random() * Number.MAX_SAFE_INTEGER)

const drawing = svg(config, (canvas) => {

  /**
   * Algorithm:
   * 
   * 1. choose a starting point about which to rotate
   * 2. each line gets placed concentrically around the starting point, with an increasing radius
   * 3. each line gets rotated 180 degrees
   * 4. a new rotation point is chosen based on the largest radius * 2
   * 5. rotate the other direction
   */
  const nLines = 9
  const maxRadius = canvas.height / 10
  const minRadiusRatio = 0.1
  for (let i = 0; i < nLines; i++) {
    let radius = map(0, nLines - 1, maxRadius * minRadiusRatio, maxRadius, i)
    let center = vec2(canvas.width / 2, maxRadius)
    let curveRight = true
    const startRotation = -Math.PI/2
    const rotationDuration = Math.PI

    canvas.path((path) => {
      path.setAttributes({ 'stroke-width': 0.3 })
      path.moveTo(center.add(vec2(Math.cos(startRotation) * radius, Math.sin(startRotation) * radius)))
      while (center.y < canvas.height) {
        if (curveRight) {
          for (let r = startRotation; r < startRotation + rotationDuration; r += 0.1) {
            path.lineTo(center.add(vec2(Math.cos(r) * radius, Math.sin(r) * radius)))
          }
        } else {
          for (let r = startRotation; r > startRotation - rotationDuration; r -= 0.1) {
            path.lineTo(center.add(vec2(Math.cos(r) * radius, Math.sin(r) * radius)))
          }
        }
        // ah, we have to flip the radius so it corresponds to the "opposite" size based on it's position. Otherwise we have a lot of disjointed lines which isn't as cool
        // definitely would like a more generic way to handle this, but for a first attempt it works just fine
        radius = curveRight
          ? map(nLines - 1, 0, maxRadius * 0.1, maxRadius, i)
          : map(0, nLines - 1, maxRadius * 0.1, maxRadius, i)
        curveRight = !curveRight
        center = center.add(vec2(0, maxRadius * (1 + minRadiusRatio)))
      }
    })
  }
})

// capture screenshot and loop
renderLoop(seed, drawing, {
  loopCount: 1,
})

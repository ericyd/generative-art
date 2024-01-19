import { renderSvg, vec2, map, randomSeed, createRng, Vector2, path, random, Circle, angleOfVertex, smallestAngularDifference, haveSameSign, shuffle, circle } from '@salamivg/core'

const config = {
  fill: 'none',
  stroke: '#000',
  width: 100,
  height: 100,
  scale: 5,
  loopCount: 5,
}

let seed =  randomSeed()

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)
  svg.fill = null

  /**
   * Algorithm:
   *
   * 1. Create a list of `n` random points
   * 2. Choose a starting angle (0 is probably fine)
   * 3. Using a known radius, rotate around each point until we are "pointing" at the next point in the sequence
   *    - "pointing at" is defined as being tangent to both the current "circle" (radius) and the next circle/radius
   *    - there are obviously multiple points where this could be true for any 2 circles,
   *      so I'm not sure how we will pick which one to use, perhaps it will be random,
   *      or perhaps just the "first" one we find.
   * 4. Continue until we have approached all points in the sequence.
   */
  const nPoints = 7
  let angle = random(0, Math.PI * 2, rng)
  let previousTangentAngle = angle - Math.PI / 2
  
  const baseRadius = svg.height / 10
  svg.fill = null
  svg.strokeWidth = 0.2


  // pack some circles together, ideally without overlaps
  /** @type {Circle[]} */
  const circles = []
  var failedAttempts = 0
  const maxFailedAttempts = 100
  while (failedAttempts < maxFailedAttempts && circles.length < nPoints) {
    const center = Vector2.random(svg.width * 0.1, svg.width * 0.9, svg.height * 0.1, svg.height * 0.9, rng)
    const radius = random(baseRadius * 0.25, baseRadius * 1.95, rng)
    const c = circle({ x: center.x, y: center.y, radius })

    if (circles.some(other => other.intersectsCircle(c))) {
      failedAttempts++
      continue
    }

    // this is better for some circle packing but it makes this take **forever** and I'm impatient
    // failedAttempts = 0
    circles.push(c)
  }
  
  const contour = path(p => {
    p.moveTo(circles[0].center.add(vec2(Math.cos(angle) * circles[0].radius, Math.sin(angle) * circles[0].radius)))
    for (let i = 1; i < circles.length; i++) {
      const next = circles[i]
      const current = circles[i - 1]

      // creates LOTS of discontinuities
      // const tangents = random(0, 1, rng) > 0.5
      //   ? current.outerTangents(next)
      //   : current.innerTangents(next)
      const tangents = shuffle(current.bitangents(next), rng)
      
      while (angle > Math.PI) { angle -= Math.PI * 2 }
      while (angle < -Math.PI) { angle += Math.PI * 2 }

      let shouldBreak = false

      // Determine if we're going to rotate positive or negative.
      // Simply check the smallest angle for one value, and if the smallest angle is >PI/2,
      // then it is not a "sharp" angle and we can go the other way
      
      // There is an issue where landing on the target circle is not exactly the angle we'd expect.
      // Therefore, to be "extra" sure that we're testing the correct direction, we add 3 degrees to the current angle to test it
      const someDegrees = (Math.PI / 180) * 3
      const nextPositivePoint = current.center.add(vec2(Math.cos(angle + someDegrees) * current.radius, Math.sin(angle + someDegrees) * current.radius))
      const angularDerivative = Math.atan2(nextPositivePoint.y - p.cursor.y, nextPositivePoint.x - p.cursor.x)
      const smallestPositiveRotation = smallestAngularDifference(previousTangentAngle, angularDerivative)
      let rotation = Math.abs(smallestPositiveRotation) < Math.PI / 2
        ? 1
        : -1
      const target = angle + (Math.PI * 2 * rotation)
      let previousPoint = p.cursor
      for (angle; rotation === 1 ? angle < target : angle > target; angle += (0.05 * rotation)) {
        previousPoint = p.cursor
        p.lineTo(
          current.center.add(vec2(Math.cos(angle) * current.radius, Math.sin(angle) * current.radius)),
        )

        const closeTangents = tangents.filter(([smallPoint, largePoint, tangentAngle]) => {
          const [startPoint, _endPoint] = current.radius < next.radius ? [smallPoint, largePoint] : [largePoint, smallPoint]
          const isClose = startPoint.distanceTo(p.cursor) < current.radius * 0.05
          const angularDifference = smallestAngularDifference(angle, tangentAngle) 
          const isPast = rotation === 1 ? angularDifference < 0 && angularDifference > -0.1 : angularDifference > 0 && angularDifference < 0.1
          return isClose && isPast
        })
        for (let t = 0; t < closeTangents.length; t++) {
          const [smallPoint, largePoint, target, tangentType] = closeTangents[t] 
          const [startPoint, endPoint] = current.radius < next.radius ? [smallPoint, largePoint] : [largePoint, smallPoint]
          const angularDerivative = rotation === 1 
            // I really really really really do not understand why "-Math.PI" is necessary here
            ? Math.atan2(previousPoint.y - p.cursor.y, previousPoint.x - p.cursor.x) - Math.PI
            : Math.atan2(p.cursor.y - previousPoint.y, p.cursor.x - previousPoint.x)
          let tangentAngle = Math.atan2(endPoint.y - startPoint.y, endPoint.x - startPoint.x)
          const smallestTangentRotation = smallestAngularDifference(angularDerivative, tangentAngle)
          if (Math.abs(smallestTangentRotation) < Math.PI / 2) {
            // once we've reached the tangent, zip over to the tangent on the next circle
            previousTangentAngle = tangentAngle
            p.lineTo(endPoint)
            shouldBreak = true
            if (tangentType === 'inner') {
              angle -= Math.PI
            }
            break
          }
        }
        if (shouldBreak) break
      }
    }
  })
  svg.path(contour)

  return () => {
    seed = randomSeed()
  }
})

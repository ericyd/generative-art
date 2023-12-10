import { vec2, map, renderSvg, randomSeed, circle, createRng, Vector2, path, random, angleOfVertex, smallestAngularDifference, lineSegment } from 'artsvg'

const config = {
  background: '#fff',
  fill: 'none',
  stroke: '#000',
  width: 100,
  height: 100,
  scale: 5,
  loopCount: 1,
}

let seed = randomSeed()

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)
  svg.fill = null

  const nPoints = 9
  const radius = svg.height / 10
  const circles = new Array(nPoints).fill(0).map(() => {
    const center = Vector2.random(svg.width * 0.1, svg.width * 0.9, svg.height * 0.1, svg.height * 0.9, rng)
    console.log(center)
    return circle({ x: center.x, y: center.y,
      radius: random(radius * 0.25, radius * 1.95, rng),
      // radius: random(radius * 0.95, radius * 0.95, rng),
      fill: 'none', 'stroke-width': 0.2 })
  })
  for (let i = 0; i < circles.length; i++) {
    svg.circle(circles[i])
    if (i === circles.length - 1) {
      continue
    }
    const bitangents = circles[i].bitangents(circles[i + 1])
    for (const [point1, point2] of bitangents) {
      const line = lineSegment(point1, point2)
      line.setAttributes({ 'stroke-width': 0.1})
      svg.path(line)
      svg.circle(circle({ x: point1.x, y: point1.y, radius: 1, fill: 'none', 'stroke-width': 0.1 }))
      svg.circle(circle({ x: point2.x, y: point2.y, radius: 1, fill: 'none', 'stroke-width': 0.1 }))
    }
  }

  return () => {
    seed = randomSeed()
  }
})

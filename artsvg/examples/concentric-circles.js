import { renderSvg, circle, hypot, vec2, map } from '../lib/index.js'

const config = {
  width: 100,
  height: 100,
  scale: 5,
  loopCount: 1,
}

renderSvg(config, (svg) => {
  const center = vec2(svg.width / 2, svg.height / 2).div(2)
  svg.circle(circle({
    x: center.x,
    y: center.y,
    radius: hypot(svg.width, svg.height) * 0.02,
    fill: 'none',
    stroke: '#000',
    'stroke-width': 2
  }))

  const nRings = 10
  for (let i = 1; i <= nRings; i ++) {
    const baseRadius = map(1, Math.log(nRings), hypot(svg.width, svg.height) * 0.09, hypot(svg.width, svg.height) * 0.15, Math.log(i))
    svg.path((p) => {
      p.fill = 'none'
      p.stroke = '#000'
      p.strokeWidth = map(1, nRings, 0.3, 0.05, i)
      let radius = baseRadius + Math.sin(0) * baseRadius * 0.1
      p.moveTo(vec2(Math.cos(0) * radius, Math.sin(0) * radius).add(center))
      for (let angle = 0; angle <= Math.PI * 2; angle += 0.05) {
        radius = baseRadius + Math.sin(angle * 6) * baseRadius * 0.1
        p.lineTo(vec2(Math.cos(angle) * radius, Math.sin(angle) * radius).add(center))
      }
      p.close()
    })
  }
})

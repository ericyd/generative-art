import { renderSvg, circle, hypot, vec2, map } from 'artsvg'

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
    radius: hypot(svg.width, svg.height) * 0.05
  }))

  const nRings = 10
  for (let i = 1; i <= nRings; i ++) {
    const radius = map(1, Math.log(nRings), hypot(svg.width, svg.height) * 0.07, hypot(svg.width, svg.height) * 0.15, Math.log(i))
    svg.path((p) => {
      p.fill = 'none'
      p.stroke = '#000'
      p.strokeWidth = map(1, nRings, 0.3, 0.05, i)
      p.moveTo(vec2(Math.cos(0) * radius, Math.sin(0) * radius).add(center))
      for (let angle = 0; angle <= Math.PI * 2; angle += 0.1) {
        p.lineTo(vec2(Math.cos(angle) * radius, Math.sin(angle) * radius).add(center))
      }
      p.close()
    })
  }
})

import { vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, Oscillator, map } from 'artsvg'
import { renderSvg } from 'artsvg/render'

const config = {
  width: 100,
  height: 100,
  scale: 5,
  loopCount: 1,
}

let seed =  randomSeed()
seed = 0

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }

  svg.fill = null
  svg.stroke = ColorRgb.Black
  svg.strokeWidth = 0.15

  let osc = new Oscillator({ period: svg.width, amplitude: svg.height * 0.1 })
  
  for (let baseline = svg.height * -0.5; baseline < svg.height * 1.5; baseline += 5) {
    osc = osc.clone({ phase: map(svg.height * -0.5, svg.height * 1.5, 0, 2 * Math.PI, baseline) })
    svg.path(path => {
      path.moveTo(vec2(0, osc.output(0, baseline) + baseline))
      for (let x = 0; x < svg.width; x += 0.1) {
        path.lineTo(vec2(x, osc.output(x, baseline) + baseline))
      }
    })
  }

  return () => {
    seed = randomSeed()
  }
})

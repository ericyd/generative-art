import { vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, Oscillator } from 'artsvg'
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

  const generateOsc = (compress) => new Oscillator({ period: svg.width, amplitude: 1, compress })
      .modulateAmplitude(
        new Oscillator({ period: svg.width * 0.13, amplitude: 0.3, phase: Math.PI * 0.25, compress })
          .modulateAmplitude(
            new Oscillator({ period: svg.width * 0.3, amplitude: 0.5, phase: Math.PI * -0.9, compress })
          )
      )

  
  const scale = svg.height * 0.15
  const y = scale

  let baseline = svg.height * 0.5
  svg.path(path => {
    path.moveTo(vec2(0, baseline))
    path.lineTo(vec2(svg.width, baseline))
  })

  let osc = generateOsc(true)
  svg.stroke = 'red'
  svg.path(path => {
    path.moveTo(vec2(0, osc.output(0, y) * scale + baseline))
    for (let x = 0; x < svg.width; x += 0.1) {
      path.lineTo(vec2(x, osc.output(x, y) * scale + baseline))
    }
  })

  osc = generateOsc(false)
  baseline = svg.height * 0.5
  svg.stroke = 'blue'
  svg.path(path => {
    path.moveTo(vec2(0, osc.output(0, y) * scale + baseline))
    for (let x = 0; x < svg.width; x += 0.1) {
      path.lineTo(vec2(x, osc.output(x, y) * scale + baseline))
    }
  })

  return () => {
    seed = randomSeed()
  }
})

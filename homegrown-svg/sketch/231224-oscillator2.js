import { vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, Oscillator } from 'artsvg'
import { renderSvg } from 'artsvg/render'

const config = {
  background: '#fff',
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

  const osc = new Oscillator({ period: svg.width, amplitude: 1 })
    .modulateAmplitude(
      new Oscillator({ period: svg.width * 0.13, amplitude: 0.3, phase: Math.PI * 0.25 })
        .modulateAmplitude(new Oscillator({ period: svg.width * 0.3, amplitude: 0.5, phase: Math.PI * -0.9 }))
    )
  
  const scale = svg.height * 0.15
  for (let baseline = svg.height * -0.5; baseline < svg.height * 1.5; baseline += 5) {
    svg.path(path => {
      // observation:
      // baseline / scale is a little weird looking, but basically I have 2 options
      // 1: if base Oscillator's amplitude is greater than 1, the "osc.output" will be greater than 1.
      //    I'd prefer for this "noise" function to be based on a [-1, 1] range, which makes this not ideal
      // 2. if the base Oscillator's amplitude is 1, then the "yModulation" is much too high frequency for the desired output.
      //    therefore, we need to dampen it to achieve the right look, which involves scaling the y value.
      path.moveTo(vec2(0, osc.output(0, baseline / scale) * scale + baseline))
      for (let x = 0; x < svg.width; x += 0.1) {
        path.lineTo(vec2(x, osc.output(x, baseline / scale) * scale + baseline))
      }
    })
  }

  return () => {
    seed = randomSeed()
  }
})

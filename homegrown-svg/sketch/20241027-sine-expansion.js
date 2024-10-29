import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle, Svg, ColorHsl } from '@salamivg/core'

const config = {
  width: 800,
  height: 800,
  scale: 1,
  loopCount: 1,
  // openEveryFrame: false
}

let seed = randomSeed()
seed = 7772357553070169

const colors = [
  '785A96',
  'E4BF70',
  'B2C566',
  '6887A1',
  'CC7171',
  'E2A554',
  'A4CAC8',
  '9D689C',
].map(h => ColorRgb.fromHex(h).toHsl())

const bg = '#332C2B'
const stroke = '#1E1D1D'

renderSvg(config, (svg) => {

  const rng = createRng(seed)
  svg.filenameMetadata = { seed }
  svg.numericPrecision = 3
  svg.setBackground(bg)
  svg.setAttributes({'stroke-linecap': 'round', style: 'transform: rotate(180deg);'})
  const spectrum = ColorSequence.fromColors(shuffle(colors, rng))

  const nLines = 400

  for (let i = 0; i < nLines; i++) {
    svg.path(p => {
      p.fill = null
      p.strokeWidth = 0.4
      // p.setAttributes({
      //   'stroke-linejoin': 'round', // miter, round, bevel, miter-clip, arcs
      //   style: `filter: drop-shadow( 2px 0px 4px rgb(0.9, 0.9, 0.9, 0.9));`,
      // }) 
      
      const ampStart = random(1, 10, rng)
      const periodStart = random(config.width / 20, config.width / 10, rng)
      const freqStart = (2 * Math.PI) / periodStart
      const phase = random(0, 2 * PI, rng)

      let y = config.height + 1
      let x = config.width / 2 + sin(y * freqStart + phase) * ampStart
      p.moveTo(vec2(x, y))

      // const targetAmp = random(config.width / PHI / 2.5, config.width / PHI / 1.2, rng)
      // const targetPeriod = random(config.height / 0.2, config.height / 0.3, rng)
      
      // experiment with amounts of randomness here
      const targetAmp = random(config.width / PHI / 1.2, config.width / PHI / 1.4, rng)
      const targetPeriod = random(config.height / 0.2, config.height / 0.3, rng)
      
      const exp = random(2.1, 2.5, rng)

      // for some reason it seems like the first x is bad... but the second one is good???
      let spectrumIndex = null

      while (y > 0) {
        y -= 1

        //// exponential curve
        // const amp = map(
        //   0,
        //   Math.pow(config.height, exp),
        //   ampStart,
        //   targetAmp,
        //   Math.pow(y, exp),
        // )
        // const period = map(
        //   0,
        //   Math.pow(config.height, exp),
        //   periodStart,
        //   targetPeriod,
        //   Math.pow(y, exp)
        // )

        //// sinusoidal amplitude curve
        const amp = map(
          Math.sin(-PI/2),
          Math.sin(PI/2),
          ampStart,
          targetAmp,
          Math.sin(PI * (y-config.height/2) / config.height),
        )
        const period = map(
          Math.sin(-PI/2),
          Math.sin(PI/2),
          periodStart,
          targetPeriod,
          Math.sin(PI * (y-config.height/2) / config.height),
        )

        const freq = (2 * Math.PI) / period
        x = config.width / 2 + sin(y * freq + phase) * amp
        spectrumIndex = spectrumIndex ?? map(config.width * 0.25, config.width * 0.75, 0, 1, x)
        p.lineTo(vec2(x, y))
      }

      p.stroke = spectrum.at(spectrumIndex).toString()
    })
  }

  return () => { seed = randomSeed(); }
})

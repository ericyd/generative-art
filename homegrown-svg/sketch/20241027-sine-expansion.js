import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle, Svg, ColorHsl } from '@salamivg/core'

/**
 * There are a few primary "styles", each with some subtle variations:
 * 1. exponential curve, vs sinusoidal curve
 * 2. high vs low terminal randomness
 * 3. low vs high exponent
 */

const config = {
  width: 800,
  height: 800,
  scale: 1,
  loopCount: 15,
  // openEveryFrame: false
}

let seed = randomSeed()
// seed = 8180884357964691
// seed = 7391022712813691
// seed = 5651293227607627
// seed = 92306707012873
// seed = 144379427891799
// seed = 8599043095752115

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

renderSvg(config, (svg) => {

  const rng = createRng(seed)
  svg.filenameMetadata = { seed }
  svg.numericPrecision = 3
  svg.setBackground(bg)
  svg.setAttributes({'stroke-linecap': 'round', style: 'transform: rotate(180deg);'})
  const spectrum = ColorSequence.fromColors(shuffle(colors, rng))

  /**
   * Parameters
   */
  const nLines = 500
  const curviness = random(config.height / 0.07, config.height / 0.4, rng) // randomFromArray([config.height / 0.25, config.height / 0.1, config.height / 0.35], rng)
  const curveType = 'sinusoidal' // randomFromArray(['exponential', 'sinusoidal'], rng)
  const sineCurveAmount = 2 // random(1.8, 2.2, rng)

  for (let i = 0; i < nLines; i++) {
    svg.path(p => {
      p.fill = null
      p.strokeWidth = 0.4
      
      const ampStart = random(1, 10, rng)
      const periodStart = random(config.width / 20, config.width / 10, rng)
      const freqStart = (2 * Math.PI) / periodStart
      const phase = random(0, 2 * PI, rng)

      let y = config.height + 1
      let x = config.width / 2 + sin(y * freqStart + phase) * ampStart
      p.moveTo(vec2(x, y))

      const targetAmp = random(config.width / PHI / 1.5, config.width / PHI / 1.3, rng)
      const targetPeriod = random(curviness * 0.8, curviness * 1.2, rng)
      
      const exp = curveType === 'exponential'
        ? random(1.5, 2.4, rng)
        : random(3.1, 4.5, rng)

      // for some reason it seems like the first x is bad... but the second one is good???
      let spectrumIndex = null
      const end = random(config.height * -0.1, config.height * 0.4, rng)

      while (y > end) {
        y -= 1

        let amp
        let period
        if (curveType === 'exponential') {
          //// exponential curve
          amp = map(
            0,
            Math.pow(config.height, exp),
            ampStart,
            targetAmp,
            Math.pow(y, exp),
          )
          period = map(
            0,
            Math.pow(config.height, exp),
            periodStart,
            targetPeriod,
            Math.pow(y, exp)
          )
        } else {
          //// sinusoidal amplitude curve
          amp = map(
            0,
            1,
            ampStart,
            targetAmp,
            Math.sin(PI * y / config.height / sineCurveAmount) ** exp,
          )
          period = map(
            0,
            1,
            periodStart,
            targetPeriod,
            Math.sin(PI * y / config.height / sineCurveAmount) ** exp,
          )
          //// this curve is interesting because it is has a "double bump", but visually I don't like it as much
          // amp = map(
          //   Math.sin(-PI/2),
          //   Math.sin(PI/2),
          //   ampStart,
          //   targetAmp,
          //   Math.sin(PI * (y-config.height/2) / config.height) ** 3,
          // )
          // period = map(
          //   Math.sin(-PI/2),
          //   Math.sin(PI/2),
          //   periodStart,
          //   targetPeriod,
          //   Math.sin(PI * (y-config.height/2) / config.height) ** 3,
          // )
        }

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

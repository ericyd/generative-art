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
  loopCount: 1,
  // openEveryFrame: false
}

let seed = randomSeed()
// seed = 3629904562570931
// seed = 6903882123970721
// seed = 1446312498313953
// seed = 6672370654924951
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
  svg.strokeWidth = 5
  svg.setAttributes({'stroke-linecap': 'round', style: 'transform: rotate(180deg);'})
  const spectrum = ColorSequence.fromColors(shuffle(colors, rng))
  
  /**
   * Parameters
  */
  const nLines = 500 // randomInt(50, 600, rng) // 500
  const curviness = randomFromArray([config.height / 0.25, config.height / 0.1, config.height / 0.35], rng)
  const sineCurveAmount = 2 // random(1.8, 2.2, rng)
 
  const center = vec2(config.width, config.height).scale(0.5)
  const nCircles = 5
  for (let i = 0; i < nCircles; i++) {
    const radius = map(0, nCircles - 1, hypot(center.x, center.y) / 4, hypot(center.x, center.y) / 2, i)
    svg.circle({ center, radius, stroke: ColorRgb.fromHex('#eedc53'), fill: 'none' })
  }
  const maxAmp = config.width / PHI / 1.3
  const maxPeriod = curviness * 1.2

  for (let ii = 0; ii < 2; ii++) {
    svg.path(p => {
      p.fill = ii === 0 ? bg : 'none'
      p.stroke = ii === 0 ? bg : '#fff'
  
      const ampStart = 10
      const periodStart = config.width / 20 // random(config.width / 20, config.width / 10, rng)
      const phase = 0 // random(0, 2 * PI, rng)
      const targetAmp = maxAmp
      const targetPeriod = maxPeriod
      const exp = 4.5
      let y = config.height + 1
  
      let amp = map(
        0,
        1,
        ampStart,
        targetAmp,
        Math.sin(PI * y / config.height / sineCurveAmount) ** exp,
      )
      let period = map(
        0,
        1,
        periodStart,
        targetPeriod,
        Math.sin(PI * y / config.height / sineCurveAmount) ** exp,
      )
      const freq = (2 * Math.PI) / period
  
      let x = config.width / 2 + sin(y * freq + phase) * amp
      p.moveTo(vec2(x, y))
  
      const end = config.height * -0.1
  
      while (y > end) {
        y -= 1
  
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
        const freq = (2 * Math.PI) / period
        x = config.width / 2 + sin(y * freq + phase) * amp
        if (!Number.isNaN(x)) {
          p.lineTo(vec2(x, y))
        }
      }
  
      if (ii === 0) {
        p.lineTo(vec2(config.width, 0))
        p.close()
      }
  
    })
  }

  return () => { seed = randomSeed(); }
  
})

/**
 * Just a test harness for "osc noise"
 */
import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Oscillator } from '@salamivg/core'

const config = {
  width: 100,
  height: 100,
  scale: 6,
  loopCount: 10,
}

let seed = randomSeed()
// for this specific seed, this is actually a really nicely balanced noise function
// What I should do next is find one that kinda sucks and compare the osc values, to see what is "off" in the bad version

/*
root Oscillator { period: 10.468009451571263, amplitude: 1, phase: 2.235352087935526, compress: true }
amp modulator Oscillator { period: 7.292362666758709, amplitude: 0.4130405454896391, phase: 2.22932942062853, compress: true }
amp modulator Oscillator { period: 2.4776572547852993, amplitude: 0.35973768141120677, phase: -0.057106354929815195, compress: true }
amp modulator Oscillator { period: 3.5478078895714136, amplitude: 0.3294493096880615, phase: 1.5535223546414714, compress: true }
phase modulator Oscillator { period: 8.507173839095048, amplitude: 0.3320154033624567, phase: 2.6161497254205983, compress: true }
phase modulator Oscillator { period: 17.720580648607573, amplitude: 2.738473908104934, phase: 6.370692716615553, compress: true }
*/
// seed = 651304471634959 // good

/*
root Oscillator { period: 3.3158394179526534, amplitude: 1, phase: -0.07072479356010586, compress: true }
amp modulator Oscillator { period: 9.113570483145304, amplitude: 0.4735880738124252, phase: 3.7881454481846637, compress: true }
amp modulator Oscillator { period: 8.425789991742931, amplitude: 0.29337700167670844, phase: 3.64955688890483, compress: true }
amp modulator Oscillator { period: 7.426569890556857, amplitude: 0.31530761029571297, phase: -2.1761605511148865, compress: true }
phase modulator Oscillator { period: 15.643384381500073, amplitude: 0.4618008703016676, phase: -3.0232207076782416, compress: true }
phase modulator Oscillator { period: 13.580103755625895, amplitude: 1.6573581582168118, phase: 0.36789065767574947, compress: true }
phase modulator Oscillator { period: 8.58206548448652, amplitude: 2.93439329354791, phase: -0.8119071822794348, compress: true }
*/
// seed = 1768598204350435 // bad - parts are too aggressive

/*
root Oscillator { period: 5.058088315098156, amplitude: 1, phase: -1.7216498000247111, compress: true }
amp modulator Oscillator { period: 6.852795965713449, amplitude: 0.4682681420817971, phase: -0.5724109082123423, compress: true }
amp modulator Oscillator { period: 8.359707766841165, amplitude: 0.397628552094102, phase: 2.164308053675893, compress: true }
amp modulator Oscillator { period: 2.7930302812950685, amplitude: 0.16503970948979257, phase: -0.629609464881459, compress: true }
phase modulator Oscillator { period: 10.487935031368396, amplitude: 3.144810232357122, phase: -1.9764071875131122, compress: true }
phase modulator Oscillator { period: 12.56897385388147, amplitude: 0.4022113042371348, phase: 0.20148900658773705, compress: true }
phase modulator Oscillator { period: 11.938893816270864, amplitude: 2.641412820261903, phase: -0.3417424188595497, compress: true }
phase modulator Oscillator { period: 19.551363948499784, amplitude: 2.806000940874219, phase: 4.760833408430994, compress: true }
*/
// seed = 4834675582268561 // bad - parts are too aggressive

/*
root Oscillator { period: 10.116619652261388, amplitude: 1, phase: -1.98468021339435, compress: true }
amp modulator Oscillator { period: 8.358894725516437, amplitude: 0.17826097449287773, phase: 2.0759743800934087, compress: true }
amp modulator Oscillator { period: 2.4567219262709843, amplitude: 0.15157505962997675, phase: 0.12546245398061373, compress: true }
phase modulator Oscillator { period: 14.661552848387508, amplitude: 3.054080114925746, phase: 3.2277866428322675, compress: true }
phase modulator Oscillator { period: 14.498460225365125, amplitude: 0.6665029731718823, phase: 4.341319394025422, compress: true }
phase modulator Oscillator { period: 18.537691382132472, amplitude: 1.0888867980916985, phase: -7.137560372596296, compress: true }
*/
// seed = 5090911635876001 // too flat, but at least smooth

/*
root Oscillator { period: 11.100438381837574, amplitude: 1, phase: -0.4118577318934058, compress: true }
amp modulator Oscillator { period: 2.6181466064183043, amplitude: 0.18658878952264787, phase: -1.0557603484524805, compress: true }
amp modulator Oscillator { period: 5.535201201308519, amplitude: 0.30192020451650026, phase: -2.1670695358117, compress: true }
amp modulator Oscillator { period: 3.1815689020557327, amplitude: 0.24659375501796604, phase: -0.8092144187024193, compress: true }
phase modulator Oscillator { period: 14.767062290827743, amplitude: 3.221349505088292, phase: -1.8278093907586532, compress: true }
phase modulator Oscillator { period: 10.90261899337638, amplitude: 3.0387315722857604, phase: 4.681549054971939, compress: true }
phase modulator Oscillator { period: 9.830038831476122, amplitude: 2.1898428575694564, phase: -3.4398059061414488, compress: true }
phase modulator Oscillator { period: 19.97747086514719, amplitude: 3.2404052600683646, phase: -8.217778455372262, compress: true }
*/
// seed = 7019579938823353 // very bad

/*
root Oscillator { period: 8.351781033911895, amplitude: 1, phase: -1.3406414789388064, compress: true }
amp modulator Oscillator { period: 7.59197238467168, amplitude: 0.38838866725564003, phase: -0.43084097876925265, compress: true }
amp modulator Oscillator { period: 8.442819477198645, amplitude: 0.4190368734300137, phase: -3.998692784755749, compress: true }
amp modulator Oscillator { period: 5.130415902915411, amplitude: 0.27783573297783737, phase: 0.10592889672967631, compress: true }
phase modulator Oscillator { period: 15.101474075391888, amplitude: 2.3077556079090575, phase: -6.006378942456498, compress: true }
phase modulator Oscillator { period: 13.700477965455502, amplitude: 0.6179878467833623, phase: 3.039635292346013, compress: true }
*/
// seed = 4395676951776307 // pretty good, not perfect

/*
root Oscillator { period: 4.706246774731456, amplitude: 1, phase: -2.871763142656164, compress: true }
amp modulator Oscillator { period: 8.76368083062116, amplitude: 0.11886019799858333, phase: -3.360731133695168, compress: true }
amp modulator Oscillator { period: 2.5750951087335126, amplitude: 0.292095684632659, phase: -0.012976056418778636, compress: true }
amp modulator Oscillator { period: 3.856675261352211, amplitude: 0.16662539886310695, phase: 1.7858971567796047, compress: true }
amp modulator Oscillator { period: 4.7170992440311235, amplitude: 0.26015236871317027, phase: -1.7843323710077532, compress: true }
phase modulator Oscillator { period: 16.947714168205856, amplitude: 1.7194235902442598, phase: 7.159501748866539, compress: true }
phase modulator Oscillator { period: 19.596587055409326, amplitude: 1.6449870552262293, phase: -9.634585536566554, compress: true }
*/
// seed = 6386795231363267 // interesting, but a little too sharp, but not substantially

/*
root Oscillator { period: 10.543148195938706, amplitude: 1, phase: 3.025036414051714, compress: true }
amp modulator Oscillator { period: 7.54521268166136, amplitude: 0.3525046867318452, phase: 3.753784046979848, compress: true }
amp modulator Oscillator { period: 3.2196190340444444, amplitude: 0.2556452316232026, phase: -0.9000525949809934, compress: true }
amp modulator Oscillator { period: 7.06004582636524, amplitude: 0.272989711444825, phase: 2.2515424127613066, compress: true }
amp modulator Oscillator { period: 3.3222319512628022, amplitude: 0.31779987215995786, phase: -0.527130996608701, compress: true }
phase modulator Oscillator { period: 19.383660305757076, amplitude: 3.139964587783907, phase: -1.417644940934693, compress: true }
phase modulator Oscillator { period: 12.261928851925767, amplitude: 0.23612844847841188, phase: 4.217703969212485, compress: true }
*/
// seed = 7621485007727695 // good

/*
Notes
- All the good-ish ones have only 2 phase modulators, that feels like a hint
- the good ones have one phase oscillator with relatively high amplitude (2-3 ish) and others with very low, e.g. 0.1-0.8
- not seeing any massive patterns around the period on the phase mods
- most of the bad ones become pretty normal/decent if the phase modulators are removed
- this one is still bad even with phase modulators removed 1768598204350435
*/

renderSvg(config, (svg) => {
  svg.filenameMetadata = { seed }
  const rng = createRng(seed)
  svg.setBackground('#fff')

  svg.fill = null
  svg.stroke = ColorRgb.Black
  svg.strokeWidth = 0.25

  const noise = createOscNoise2(seed)
  const nPoints = 1000
  // this is clumsy but works for now
  const points = new Array(nPoints).fill(0).map(() =>
    Vector2.random(0, svg.width, 0, svg.height, rng))

  const scale = random(0.05, 0.13, rng)
  for (const point of points) {
    svg.path(path => {
      path.moveTo(point)
      for (let i = 0; i < 100; i++) {
        let noiseVal = noise(path.cursor.x * scale, path.cursor.y * scale)
        let angle = map(-1, 1, -PI, PI, noiseVal)
        path.lineTo(path.cursor.add(vec2(cos(angle), sin(angle))), 'absolute')
      }
    })
  }

  return () => {
    seed = randomSeed()
  }
})

export function createOscNoise2(seed) {
  const rng = createRng(seed)
  const compress = true
  const osc = new Oscillator({ amplitude: 1, period: random(PI, 4 * PI, rng), phase: random(-PI, PI, rng), compress })
  // console.log(`root ${osc}`)

  const ampModulatorCount = randomInt(2, 5, rng)
  for (let i = 0; i < ampModulatorCount; i++) {
    const period = random(2.1, 9.2, rng)
    const modulator = new Oscillator({ period, amplitude: random(0.1, 0.5, rng), phase: random(-period / 2, period / 2, rng), compress })
    // console.log(`amp modulator ${modulator}`)
    osc.modulateAmplitude(
      modulator
    )
  }

  const phaseModulatorCount = randomInt(0, 2, rng)
  for (let i = 0; i < phaseModulatorCount; i++) {
    const period = random(8.1, 20.2, rng)
    const modulator = new Oscillator({ period, amplitude: random(0.1, 2.5, rng), phase: random(-period / 2, period / 2, rng), compress })
    // console.log(`phase modulator ${modulator}`)
    osc.modulatePhase(
      modulator
    )
  }

  return (x, y = 0) => osc.output(x, y)
}

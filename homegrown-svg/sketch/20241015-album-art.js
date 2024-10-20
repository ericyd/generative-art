import { renderSvg, map, vec2, randomSeed, createRng, Vector2, random, circle, ColorRgb, randomFromArray, rect, hypot, Grid, range, hsl, lineSegment, Rectangle, randomInt, PI, cos, sin, clamp, ColorSequence, shuffle, Polygon, rangeWithIndex, createOscNoise, Hexagon, TAU, path, polyline, Polyline, createOscCurl, randomFromObject, PHI, LineSegment, Circle, Svg, ColorHsl } from '@salamivg/core'

/**
 * - I want a line or sequence of points which "snakes through" the hex grid
 * - Rules for the "snake":
 *    1. Cannot visit a "side" (l, r, t) more than once
 *    2. Can only travel to "adjacent" sides of neighbors, or unoccupied sides of self
 *    3. Adjacent sides of neighbors are defined as
 *      - top is adjacent to left and right of neighbor on top
 *      - left is adjacent to right of left neighbor, and top of bottom neighbor
 *      - right is adjacent to left of right neighbor, and top of bottom neighbor
 *    4. Snake enters a side perpendicular to it's edge, accounting for perspective
 *    5. Snake can change angles anywhere inside of the side/face that it is currently in
 *      - Snake can also go straight, e.g. from left to right of same SplitHex
 *    6. Snake should try to touch all hexes
 *    7. Snake enters from boundary of sketch, and moves straight until it hits the edge of the first hex
 */


const config = {
  width: 800,
  height: 800,
  scale: 1,
  loopCount: 1,
  // openEveryFrame: false
}

let seed = randomSeed()
// seed = 8211922090159339
let snakeSeed = randomSeed()
// snakeSeed = 4992160343199125
// snakeSeed = 2391314494864797

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

const stroke = '#1E1D1D'

/**
 * props: l = left, r = right, t = top
 */
class SplitHex {
  // rounded corners
  // https://stackoverflow.com/a/38118843
  // https://stackoverflow.com/a/32875327

  constructor(center, rng, circumradius, strokeWidth, stroke) {
    this.color = randomFromArray(colors, rng)
    this.center = center
    this.circumradius = circumradius
    this.strokeWidth = strokeWidth
    this.stroke = stroke

    /** @type{('left' | 'right' | 'top')[]} to indicate which faces have been taken */
    this.taken = []

    this.tRotation = PI*7/6;
    this.rRotation = -PI/6;
    this.lRotation = PI/2;

    this.tFaceCenter = this.center.add(Vector2.fromAngle(this.tRotation + PI / 3).scale(this.circumradius / 2));
    this.lFaceCenter = this.center.add(Vector2.fromAngle(this.lRotation + PI / 3).scale(this.circumradius / 2));
    this.rFaceCenter = this.center.add(Vector2.fromAngle(this.rRotation + PI / 3).scale(this.circumradius / 2));

    this.tlEdgeCenter = this.center.add(Vector2.fromAngle(this.tRotation).scale(this.circumradius / 2));
    this.trEdgeCenter = this.center.add(Vector2.fromAngle(this.rRotation).scale(this.circumradius / 2));
    this.lrEdgeCenter = this.center.add(Vector2.fromAngle(this.lRotation).scale(this.circumradius / 2));

    // named because it joins `this` top face with `outer` right face
    this.tOutREdgeCenter = this.tFaceCenter.add(Vector2.fromAngle(this.tRotation).scale(this.circumradius / 2))
    // joins `this` top face with `outer` left face
    this.tOutLEdgeCenter = this.tFaceCenter.add(Vector2.fromAngle(this.rRotation).scale(this.circumradius / 2))
    // joins `this` right face with `outer` left face
    this.rOutLEdgeCenter = this.rFaceCenter.add(Vector2.fromAngle(this.rRotation).scale(this.circumradius / 2))

    const sharpCorners = false
    if (sharpCorners) {
      this.t = this.#drawStraightCubeSide(this.tRotation, this.color)
      this.r = this.#drawStraightCubeSide(this.rRotation, this.color.mix(ColorRgb.fromHex('ffffff').toHsl(), 0.25))
      this.l = this.#drawStraightCubeSide(this.lRotation, this.color.mix(ColorRgb.fromHex('000000').toHsl(), 0.25))
    } else {
      this.t = this.#drawRoundedCubeSide(this.tRotation, this.color)
      this.r = this.#drawRoundedCubeSide(this.rRotation, this.color.mix(ColorRgb.fromHex('ffffff').toHsl(), 0.25))
      this.l = this.#drawRoundedCubeSide(this.lRotation, this.color.mix(ColorRgb.fromHex('000000').toHsl(), 0.25))
    }
  }

  draw(svg) {
    svg.path(this.r)
    svg.path(this.t)
    svg.path(this.l)

    //// debugging
    // svg.circle({ center: this.tFaceCenter, fill: '#ff0000', radius: 5 })
    // svg.circle({ center: this.rFaceCenter, fill: '#880000', radius: 5 })
    // svg.circle({ center: this.lFaceCenter, fill: '#220000', radius: 5 })
    // svg.circle({ center: this.tlEdgeCenter, fill: '#00ff00', radius: 5 })
    // svg.circle({ center: this.trEdgeCenter, fill: '#008800', radius: 5 })
    // svg.circle({ center: this.lrEdgeCenter, fill: '#002200', radius: 5 })
    // svg.circle({ center: this.tOutREdgeCenter, fill: '#0000ff', radius: 5 })
    // svg.circle({ center: this.rOutLEdgeCenter, fill: '#000088', radius: 5 })
    // svg.circle({ center: this.tOutLEdgeCenter, fill: '#ffff00', radius: 5 })
  }

  findFaceCenter(face = 'top') {
    if (face === 'top') {
      return this.tFaceCenter
    } else if (face === 'right') {
      return this.rFaceCenter
    } else if (face === 'left') {
      return this.lFaceCenter
    }
  }

  findInternalConnectionPoint(currentFace = 'right', targetFace = 'left') {
    if (currentFace === 'right') {
      if (targetFace === 'left') {
        return this.lrEdgeCenter
      } else if (targetFace === 'top') {
        return this.trEdgeCenter
      }
    }

    if (currentFace === 'left') {
      if (targetFace === 'right') {
        return this.lrEdgeCenter
      } else if (targetFace === 'top') {
        return this.tlEdgeCenter
      }
    }

    if (currentFace === 'top') {
      if (targetFace === 'left') {
        return this.tlEdgeCenter
      } else if (targetFace === 'right') {
        return this.trEdgeCenter
      }
    }
  }

  findExternalConnectionPoint(currentFace = 'right', targetFace = 'left', otherHex) {
    if (currentFace === 'right') {
      if (targetFace === 'left') {
        return this.rOutLEdgeCenter
      } else if (targetFace === 'top') {
        return otherHex.tOutREdgeCenter
      }
    }

    if (currentFace === 'left') {
      if (targetFace === 'right') {
        return otherHex.rOutLEdgeCenter
      } else if (targetFace === 'top') {
        return otherHex.tOutLEdgeCenter
      }
    }

    if (currentFace === 'top') {
      if (targetFace === 'left') {
        return this.tOutLEdgeCenter
      } else if (targetFace === 'right') {
        return this.tOutREdgeCenter
      }
    }
  }

  #drawStraightCubeSide(rotation, color) {
    return path(p => {
      p.fill = color
      p.stroke = this.stroke
      p.strokeWidth = this.strokeWidth
      p.moveTo(this.center)
      for (let i = 0; i < 3; i++) {
        const angle = (Math.PI / 3) * i + rotation
        p.lineTo(this.center.add(
          Vector2.fromAngle(angle).scale(this.circumradius)
        ))
      }
      p.close()
    })
  }

  #drawRoundedCubeSide(rotation, color) {
    // these values were determined experimentally
    const wideRouding = 0.9
    const wideRadiusScale = 1/5
    const narrowRounding = 0.85
    const narrowRadiusScale = 1/12
    // const narrowRounding = 0.8
    // const narrowRadiusScale = 1/8

    // interesting visual option
    const cutoutCenter = false

    // this code is super ugly but it doesn't really need an abstraction right now, it works
    return path(p => {
      p.fill = color
      p.stroke = this.stroke
      p.strokeWidth = this.strokeWidth

      /// start[1] move to first point (offset from "center"), then line to control point at first corner
      let angle = (Math.PI / 3) * 0 + rotation
      let nextVertex = this.center.add(
        Vector2.fromAngle(angle).scale(this.circumradius)
      )
      const start = Vector2.mix(this.center, nextVertex, 1-(cutoutCenter ? narrowRounding : wideRouding))
      p.moveTo(start)
      let controlPoint = Vector2.mix(this.center, nextVertex, narrowRounding)
      p.lineTo(controlPoint)
      let lastVertex = nextVertex
      // end[1]

      /// start[2] arc across first corner, then line to control point at second corner
      angle = (Math.PI / 3) * 1 + rotation
      nextVertex = this.center.add(
        Vector2.fromAngle(angle).scale(this.circumradius)
      )
      controlPoint = Vector2.mix(lastVertex, nextVertex, 1-narrowRounding)
      p.arc({ rx: this.circumradius*narrowRadiusScale, ry: this.circumradius*narrowRadiusScale, xAxisRotation: 0, largeArcFlag: 0, sweepFlag: 1, end: controlPoint })
      controlPoint = Vector2.mix(lastVertex, nextVertex, wideRouding)
      p.lineTo(controlPoint)
      lastVertex = nextVertex
      // end[2]

      /// start[3] arc across second corner, then line to control point at third corner
      angle = (Math.PI / 3) * 2 + rotation
      nextVertex = this.center.add(
        Vector2.fromAngle(angle).scale(this.circumradius)
      )
      controlPoint = Vector2.mix(lastVertex, nextVertex, 1-wideRouding)
      p.arc({ rx: this.circumradius*wideRadiusScale, ry: this.circumradius*wideRadiusScale, xAxisRotation: 0, largeArcFlag: 0, sweepFlag: 1, end: controlPoint })
      controlPoint = Vector2.mix(lastVertex, nextVertex, narrowRounding)
      p.lineTo(controlPoint)
      lastVertex = nextVertex
      // end[3]

      /// start[4] arc across third corner, then line to control point on other side of "center"
      nextVertex = this.center
      controlPoint = Vector2.mix(lastVertex, nextVertex, 1-narrowRounding)
      p.arc({ rx: this.circumradius*narrowRadiusScale, ry: this.circumradius*narrowRadiusScale, xAxisRotation: 0, largeArcFlag: 0, sweepFlag: 1, end: controlPoint })
      controlPoint = Vector2.mix(lastVertex, nextVertex, cutoutCenter ? narrowRounding : wideRouding)
      p.lineTo(controlPoint)
      // end[4]

      // start[5] arc across "center"
      p.arc({ rx: this.circumradius*wideRadiusScale, ry: this.circumradius*wideRadiusScale, xAxisRotation: 0, largeArcFlag: 0, sweepFlag: !Boolean(cutoutCenter), end: start })
      // end[5]

      p.close() // probably not necessary but that's fine
    })
  }
}

const Dir = {
  l: 'left',
  r: 'right',
  t: 'top',
}

function buildGrid(strokeWidth, nWide) {
  const rng = createRng(seed)
  const xStart = config.width * 0.15
  const xEnd = config.width * 0.85
  const xRange = xEnd - xStart
  const apothem = xRange / (nWide - 1) / 2 // trial-and-error 🤷
  const circumradius = (apothem * 2) / Math.sqrt(3) // from salamivg Hexagon.js

  const grid = []

  // top row
  let row = []
  for (let i = 0; i < nWide-1; i++) {
    // offset x by "apothem" so they are staggered appopriately
    const x = map(0, nWide-1, xStart + apothem, xEnd + apothem, i)
    // offset y by "circumradius * 1.5" so the edges line up
    row.push(new SplitHex(vec2(x, config.height/2 - circumradius * 1.5), rng, circumradius, strokeWidth, stroke))
  }
  grid.push(row)

  // center row - has one extra
  row = []
  for (let i = 0; i < nWide; i++) {
    const x = map(0, nWide-1, xStart, xEnd, i)
    row.push(new SplitHex(vec2(x, config.height/2), rng, circumradius, strokeWidth, stroke))
  }
  grid.push(row)

  row = []
  for (let i = 0; i < nWide-1; i++) {
    const x = map(0, nWide-1, xStart + apothem, xEnd + apothem, i)
    row.push(new SplitHex(vec2(x, config.height/2 + circumradius * 1.5), rng, circumradius, strokeWidth, stroke))
  }
  grid.push(row)
  return grid
}

renderSvg(config, (svg) => {
  const grad = svg.defineLinearGradient({
    stops: [
      [0.2, ColorRgb.fromHex('#332C2B')],
      // [0.5, ColorRgb.fromHex('#332C2B')],
      // [0.51, ColorRgb.fromHex('#332C2B').mix(ColorRgb.fromHex('#ffffff'), 0.3)],
      [0.8, ColorRgb.fromHex('#332C2B').mix(ColorRgb.fromHex('#ffffff'), 0.35)],
    ]
  })

  const rng = createRng(snakeSeed)
  svg.filenameMetadata = { seed, snakeSeed }
  svg.numericPrecision = 3
  // svg.fill = grad
  // svg.stroke = grad
  svg.setBackground(grad)
  svg.setAttributes({'stroke-linecap': 'round'})

  // primary parameters
  const strokeWidth = 4
  const nWide = 5
  const allowDoubleVisits = snakeSeed % 3 === 0
  const grid = buildGrid(strokeWidth, nWide)

  // for test, start in top left hext, `l` face
  const snake = path(p => {
    p.fill = null
    // p.stroke = stroke
    // const shadow = new ColorRgb(0.9, 0.9, 0.9, 0.99).toString()
    p.stroke = '#eeeeee'
    const shadow = new ColorRgb(0.05, 0.05, 0.05, 0.99).toString()
    p.strokeWidth = strokeWidth

    // hm, might need to play with these more
    const blur = 5
    const yOffset = 0
    const xOffset = 0
    p.setAttributes({
      'stroke-linejoin': 'round', // miter, round, bevel, miter-clip, arcs
      style: `filter: drop-shadow( ${xOffset}px ${yOffset}px ${blur}px ${shadow});`,
    }) 

    // start position
    let pos = startPos(grid, rng)
    if (pos.y === 2) { console.log(pos) }
    p.moveTo(grid[pos.y][pos.x].findFaceCenter(pos.face))
    grid[pos.y][pos.x].taken.push(pos.face)
    
    let count = 0
    const max = allowDoubleVisits ? 100 : 300
    while (count++ < max) {
      const next = randomFromArray(eligibleFaces(grid, pos, allowDoubleVisits), rng)
      if (!next) break;
      
      const hex = grid[pos.y]?.[pos.x]
      const nextHex = grid[next?.y]?.[next?.x]

      // console.log(count)
      // console.log('pos', pos)
      // console.log('next', next)

      if (next.y === pos.y && next.x === pos.x) {
        p.lineTo(hex.findInternalConnectionPoint(pos.face, next.face))
      } else {
        p.lineTo(hex.findExternalConnectionPoint(pos.face, next.face, nextHex))
      }
      p.lineTo(nextHex.findFaceCenter(next.face))

      hex.taken.push(pos.face)
      nextHex.taken.push(next.face)

      pos = next
    }
  })

  // draw everything
  for (const row of grid) {
    for (const hex of row) {
      hex.draw(svg)
    }
  }
  svg.path(snake)


  return () => { seed = randomSeed(); snakeSeed = randomSeed(); }
})

function startPos(grid, rng) {
  // return {x:0, y:1, face: Dir.l}
  const y = randomInt(0, grid.length, rng)
  if (y === 0 || y === grid.length - 1) {
    const x = randomInt(0, grid[y].length, rng)
    const dirOptions = y === 0 ? [Dir.l, Dir.r, Dir.t] : [Dir.l, Dir.r]
    const face = randomFromArray(dirOptions, rng)
    return { x, y, face }
  } else {
    const x = randomFromArray([0, grid[y].length - 1], rng)
    const dirOptions = x === 0 ? [Dir.l, Dir.t] : [Dir.r, Dir.t]
    const face = randomFromArray(dirOptions, rng)
    return { x, y, face }
  }
}

function eligibleFaces(grid, pos, allowDoubleVisits, sort = true) {
  // it's a little hard to explain why this is necessary - it has to do with the hexagon grid and transforming it to a square grid
  // every 2 rows get "virtually offset" by 1 to the right
  const xAdjust = (y) => Math.floor(y / 2)

  let maybe = []

  if (pos.face === Dir.l) {
    maybe = [
      { x: pos.x, y: pos.y, face: Dir.r },
      { x: pos.x, y: pos.y, face: Dir.t },
      { x: pos.x - xAdjust(pos.y + 1), y: pos.y + 1, face: Dir.t },
      { x: pos.x - 1, y: pos.y, face: Dir.r },
    ]
  }

  if (pos.face === Dir.r) {
    maybe = [
      { x: pos.x, y: pos.y, face: Dir.l },
      { x: pos.x, y: pos.y, face: Dir.t },
      { x: pos.x + 1 - xAdjust(pos.y + 1), y: pos.y + 1, face: Dir.t },
      { x: pos.x + 1, y: pos.y, face: Dir.l },
    ]
  }

  if (pos.face === Dir.t) {
    maybe = [
      { x: pos.x, y: pos.y, face: Dir.l },
      { x: pos.x, y: pos.y, face: Dir.r },
      { x: pos.x + xAdjust(pos.y), y: pos.y - 1, face: Dir.l },
      { x: pos.x - 1 + xAdjust(pos.y), y: pos.y - 1, face: Dir.r },
    ]
  }

  let results = maybe
    .filter(m => {
      const cell = grid[m.y]?.[m.x]

      if (allowDoubleVisits) {
        return !!cell
      } else {
        return !(cell?.taken.includes(m.face) ?? true)
      }
    });

  if (!sort || allowDoubleVisits) return results
  results = results
    .map(m => ({ ...m, count: eligibleFaces(grid, m, allowDoubleVisits, false).length }))
    .sort((a,b) =>  a.count > b.count ? -1 : a.count < b.count ? 1 : 0);
  return results.filter(m => m.count >= results[0].count)
}

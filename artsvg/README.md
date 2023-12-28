# ArtSVG

A place to play with SVGs.

## Why? 

I love [OPENRNDR](https://openrndr.org/) and wanted to see if I could make a generative art framework that ran in an interpretted language. I've never been a JVM guy, and even though I like Kotlin, it sounded appealing to me to be able to write generative art in a language I used every day: JavaScript.

Of course you may (reasonably) ask why I'm not just using p5.js, the dominant JavaScript framework for writing generative art. Well, I don't have a good answer to that. I suppose this is really "just for fun" ¯\\_(ツ)_/¯

## Examples

See [the /sketch folder in my personal generative art repo](https://github.com/ericyd/generative-art/tree/ce4536cffea702ccf4050e389c1b9882561732c2/homegrown-svg/sketch) for some examples.

Here's a minimal example:

```js
import { circle, hypot, vec2, map } from 'artsvg'
import { renderSvg } from 'artsvg/render'

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

```

Which can just be run as a normal Node script, assuming you have configured your `package.json` to declare `"type": "module"`

```js
node example.js
```

Here's the output of the above script:

![concentric circles example output](./examples/concentric-circles.svg)

## API docs

Coming soon. For the time being, I tried to be pretty accurate with adding JSDoc typings to the functions and classes, so your editor should be able to help with auto-complete.

## Design Philosophy

This lib is heavily inspired by [OPENRNDR](https://openrndr.org/), which means it utilizes the builder pattern extensively. My first attempt at writing my own SVG "framework" attempted to be much more functional, and I found the scripts to be really verbose and hard to follow. I think for the purpose of making art, imperative builder patterns are really nice.

## TODO:

1. ~Migrate JSDocs to `.d.ts` files~
2. ~Add `types` to package.json `exports` prop~
3. Add `rollup` to compile a common JS output (needed?)
4. ~Add tsc to type check~
4.a. ~Fix typescript errors~
5. ~Fix Biome.js lints~
6. ~Finish grid specs~
7. Decide on a package name (artsvg, coalesvg, progressvg, gressvg, gress, coalesce, accessvg, access, egressvg, iridesvg, yesvg (damn @yesvg is squatted), finessvg, simpledraw, drawsvg, algoart, simplesvg, createsvg, SalamiVG)
8. Describe why not https://www.npmjs.com/package/svg.js, or https://dmitrybaranovskiy.github.io/raphael/
9. ~Add `Path.fromPoints` static function~
10. Add comparisons to other frameworks (see https://openrndr.org/ for example)
11. ~Add "Rectangle" 🤣~
12. ~Add "style inheritance" -- e.g. when svg.fill is set, all child elements should inherit the current value, unless it is explicitly set on the child.~
13. Add "oscillator" noise
14. Add color module, with colorSequence
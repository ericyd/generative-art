# Homegrown SVG

Some experiments with drawing SVGs using no framework no nothing (I might add some dependencies later 🤷)

# How to run

```
node homegrown-svg/scripts/my-script.js
```

# How to edit

* Just change the `script` that is imported from `index.html`.
* Everything in the `scripts` directory should export a `draw` function which takes no arguments and returns an SVG string

# Future considerations

* Use [resvg](https://github.com/RazrFalcon/resvg/blob/master/examples/minimal.rs) to render the final output. Not sure how to efficiently generate PNGs otherwise
* use [librsvg](https://www.npmjs.com/package/librsvg) npm package to render

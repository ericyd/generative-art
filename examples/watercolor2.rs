// Nearly identical to watercolor1, with tweaks to the color palettes.
// Basically a direct implementation of the technique described by Tyler Hobbes in his essay:
// https://tylerxhobbs.com/essays/2017/a-generative-approach-to-simulating-watercolor-paints
//
// The major differences between this an watercolor1 are the way the colors are assigned.
// In watercolor1, the colors are randomly pulled from the palette,
// whereas here they are pulled from the palette based on their spatial position,
// bottom of the screen being the first color in the palette, and top of the screen is the last.
// In addition, the luminosity is modified based on the y-position too. As you move upwards
// towards the next color in the palette, the luminosity increases for the given hue.
//
// Algorithm in a nutshell:
// 1. Generate a bunch of hexagons that are distorted several times
//    with the `meander` algorithm.
// 2. Select color from palette based on y-position
// 3. Loop of those shapes several times and draw them each, distorted several
//    more times with the same algorithm.
//    Draw each layer fairly transparent to give a nice overlapping appearance.
//
// cargo run --release --example watercolor2
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::color::palette_to_hsl;
use util::draw_paper_texture;
use util::meander;
use util::{capture_model, captured_frame_path};

const PALETTE: [&str; 4] = [
  "#814F46", // rusty brown
  "#E6992D", // orange
  "#F8EF77", // yellow
  "#C84A3C", // red
];

fn main() {
  nannou::app(model).update(update).run();
}

#[derive(Debug)]
struct Model {
  n_polygons: usize,
  n_layers: usize,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  app
    .new_window()
    .size(args.get("w", 768), args.get("h", 1024))
    .title(app.exe_name().unwrap())
    .view(view)
    .build()
    .unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));

  Model {
    n_polygons: args.get("n-polygons", 30),
    n_layers: args.get("n-layers", 20),
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  let args = ArgParser::new();
  model.n_polygons = args.get("n-polygons", 40);
  model.n_layers = args.get("n-layers", 20);
}

fn view(app: &App, model: &Model, frame: Frame) {
  let bg = IVORY;
  frame.clear(bg);
  let draw = app.draw();
  draw.background().color(bg);
  let win = app.window_rect();

  let shapes = generate_shapes(model, &win);

  // This triple nested loop is a bit awkward, but the intention is to
  // cycle through each shape and draw them so they all overlap/intersect with each other.
  // Drawing several per cycle (the "layers" value) gives a bit more definition
  // to each shape while it's being blended.
  let layers = 3;
  let cycles = model.n_layers / layers;
  for _ in 0..cycles {
    for (shape, color) in &shapes {
      for __ in 0..layers {
        draw.polygon().color(*color).points(meander(&shape, 4, 0.5));
      }
    }
  }

  draw_paper_texture(&draw, &win, 4000, 0.01);

  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
  capture_model(app, &frame, model);
}

fn shape(n_verticies: i32, radius: f32, x: f32, y: f32) -> Vec<Point2> {
  (0..=n_verticies)
    .map(|n| {
      let angle = map_range(n, 0, n_verticies, 0.0, 2.0 * PI);
      pt2(angle.cos() * radius + x, angle.sin() * radius + y)
    })
    .collect()
}

fn generate_shapes(model: &Model, win: &Rect) -> Vec<(Vec<Point2>, Hsla)> {
  let palette = palette_to_hsl(PALETTE.to_vec());
  let n_colors = palette.len();
  (0..model.n_polygons)
    .map(|_| {
      let n_verticies = random_range(3, 10);
      let y_min = win.bottom() * 1.3;
      let y_max = win.top() * 1.3;
      let y = random_range(y_min, y_max);
      let polygon = shape(
        n_verticies,
        random_range(win.w() / 5.0, win.w() / 2.0),
        random_range(win.left() * 1.3, win.right() * 1.3),
        y,
      );
      // polygon's y-position as a fraction of total height
      let y_pos = map_range(y, y_min, y_max, 0.0, 1.0);

      // map the y_pos to the length of the
      let color_index = map_range(y, y_min, y_max, 0, n_colors);

      // for each "color_index", the polygons should slightly fade from dark to light
      // as it gets closer to the next color
      let color_frac = 1.0 / n_colors as f32;
      let color_adjust = map_range(
        y_pos,
        color_frac * color_index as f32,
        color_frac * (color_index as f32 + 1.0),
        0.8,
        1.1,
      );

      // fetch color and modify it's components
      let (h, s, l) = palette[color_index].into_components();
      let h = h.to_positive_degrees() / 360.0;
      let color = hsla(
        random_range(h * 0.9, h * 1.1),
        random_range(s * 0.9, s * 1.1),
        l * color_adjust,
        1.0 / model.n_layers as f32,
      );
      (meander(&polygon, 12 - n_verticies, 0.8), color)
    })
    .collect()
}

// Experimenting with watercolor textures.
// Basically a direct implementation of the technique described by Tyler Hobbes in his essay:
// https://tylerxhobbs.com/essays/2017/a-generative-approach-to-simulating-watercolor-paints
//
// Algorithm in a nutshell:
// 1. Generate a bunch of hexagons that are distorted several times
//    with the `meander` algorithm
// 2. Loop of those shapes several times and draw them each, distorted several
//    more times with the same algorithm.
//    Draw each layer fairly transparent to give a nice overlapping appearance.
// 3. Draw some "mountains"
//
// cargo run --release --example watercolor1 -- --loops 5 --n-blobs 40 --n-layers 20 --w 768 --h 1024
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::noise::{NoiseFn, Perlin};
use nannou::prelude::*;
use nannou::Draw;

mod util;
use util::args::ArgParser;
use util::color::{palette_to_hsl, rgb_from_hex, select_random};
use util::meander;
use util::{capture_model, captured_frame_path};

const PALETTE: [&str; 9] = [
  "#DF4E8F", // pink
  "#C899F4", // lavender
  "#7485E7", // lilac
  "#2C18C3", // dark blue
  "#133B5E", // dark gray-blue
  "#F0C666", // orange
  "#F78250", // salmon-y
  "#922A33", // dark red
  "#F16A83", // another pink
];

fn main() {
  nannou::app(model).update(update).run();
}

#[derive(Debug)]
struct Model {
  n_blobs: usize,
  n_layers: usize,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  app
    .new_window()
    .size(args.get("w", 1024), args.get("h", 1024))
    .title(app.exe_name().unwrap())
    .view(view)
    .build()
    .unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));

  Model {
    n_blobs: args.get("n-blobs", 30),
    n_layers: args.get("n-layers", 100),
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  let args = ArgParser::new();
  model.n_blobs = args.get("n-blobs", random_range(15, 45));
  model.n_layers = args.get("n-layers", random_range(15, 55));
}

fn view(app: &App, model: &Model, frame: Frame) {
  frame.clear(rgb_from_hex("#98C5F5"));
  let draw = app.draw();
  draw.background().color(rgb_from_hex("#98C5F5"));
  let win = app.window_rect();

  let shapes = generate_shapes(model, &win);

  for _ in 0..model.n_layers {
    for (shape, color) in &shapes {
      draw.polygon().color(*color).points(meander(&shape, 6, 0.8));
    }
  }

  draw_mountains(&draw, &win);

  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame_threaded(captured_frame_path(app, &frame));
  capture_model(app, &frame, model);
}

fn hexagon(radius: f32, x: f32, y: f32) -> Vec<Point2> {
  (0..=8)
    .map(|n| {
      let angle = map_range(n, 0, 8, 0.0, 2.0 * PI);
      pt2(angle.cos() * radius + x, angle.sin() * radius + y)
    })
    .collect()
}

fn generate_shapes(model: &Model, win: &Rect) -> Vec<(Vec<Point2>, Hsla)> {
  let palette = palette_to_hsl(PALETTE.to_vec());
  (0..model.n_blobs)
    .map(|_| {
      let hex = hexagon(
        random_range(win.w() / 10.0, win.w() / 3.0),
        random_range(win.left() * 1.3, win.right() * 1.3),
        random_range(win.bottom() * 1.3, win.top() * 1.3),
      );
      let (h, s, l) = select_random(&palette).into_components();
      let h = h.to_positive_degrees() / 360.0;
      let color = hsla(
        random_range(h * 0.9, h * 1.1),
        random_range(s * 0.9, s * 1.1),
        random_range(l * 0.9, l * 1.1),
        1.0 / (model.n_layers / 2) as f32,
      );
      (meander(&hex, 4, 0.5), color)
    })
    .collect()
}

fn draw_mountains(draw: &Draw, win: &Rect) {
  let mut mountains = vec![pt2(win.left() - 10.0, win.bottom() - 10.0)];
  let seed = random_f64() * 1000.0;
  let noise_scale = (win.w() / 10.0) as f64;
  let noisefn = Perlin::new();
  let points = (0..win.w() as u32).map(|n| {
    let x = map_range(n, 0, win.w() as u32, win.left(), win.right());
    let y = (x / 700.0).sin();
    let noise = noisefn.get([x as f64 / noise_scale, y as f64 / noise_scale, seed]) as f32;
    let y = y * noise * win.h() / 15.0 + win.h() / 20.0 + win.bottom();
    pt2(x, y)
  });
  mountains.extend(points);
  mountains.extend(vec![pt2(win.right() + 10.0, win.bottom() - 10.0)].iter());

  draw.polygon().hsl(0.0, 0.3, 0.05).points(mountains);
}

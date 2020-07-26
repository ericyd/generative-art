// cargo run --release --example dripping_noise
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::noise::{NoiseFn, OpenSimplex};
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::blob::Blob;
use util::capture_model;
use util::captured_frame_path;
use util::Line2;

fn main() {
  nannou::app(model).run();
}

#[derive(Debug)]
struct Model {
  // dimensions of blob grid
  nx: usize,
  ny: usize,
  // Number of blobs in "grid"
  n_blobs: usize,
  // length of flow lines
  line_length: usize,
  // noise seed
  seed: f64,
  // noise "scale" - higher is longer wavelength
  noise_scale: f64,
  // line stroke weight
  stroke_weight: f32,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();

  app
    .new_window()
    .title(app.exe_name().unwrap())
    .view(view)
    .size(1024, 1024)
    .build()
    .unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));

  Model {
    nx: args.get("nx", 7),
    ny: args.get("ny", 5),
    n_blobs: args.get("n-blobs", 40),
    line_length: args.get("line-length", 200),
    noise_scale: args.get("noise-scale", random_range(50.0, 400.0)),
    seed: args.get("seed", random_range(1.0, 10.0.powi(7))),
    stroke_weight: args.get("stroke-weight", 1.0),
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  let win = app.window_rect();

  // Prepare to draw.
  let draw = app.draw();
  draw.background().color(IVORY);

  let blobs = gen_blobs(
    model,
    &Rect::from_corners(
      pt2(win.left() * 0.8, win.bottom() * 0.4),
      pt2(win.right() * 0.8, win.top() * 0.6),
    ),
  );

  for blob in blobs {
    // blob.draw(&draw);
    for point in blob.points() {
      let line = gen_line(&point, model.line_length, model.noise_scale, model.seed);
      draw
        .polyline()
        .caps_round()
        .color(BLACK)
        .stroke_weight(model.stroke_weight)
        .points(line);
    }
  }

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
  capture_model(app, &frame, model);
}

fn gen_blobs(model: &Model, bounds: &Rect) -> Vec<Blob> {
  let Model { nx, ny, .. } = *model;
  let wh = 20.;
  (0..model.n_blobs)
    .map(|i| {
      let i_y = i % ny;
      let i_x = (i - i_y) / ny;
      // TODO: randomize start points slightly
      let x = map_range(i_x, 0, nx, bounds.left(), bounds.right());
      let y = map_range(i_y, 0, ny, bounds.bottom(), bounds.top());
      Blob::new()
        .x_y(x, y)
        .w_h(wh, wh)
        .noise_scale(0.8)
        .seed(random_range(1., 10000.))
        .resolution(100)
    })
    .collect()
}

fn gen_line(point: &Point2, length: usize, noise_scale: f64, seed: f64) -> Line2 {
  let mut x = point.x as f64;
  let mut y = point.y as f64;
  let noise = OpenSimplex::new();
  (0..length)
    .map(|_i| {
      x += noise.get([x / noise_scale, y / noise_scale, seed]);
      y += noise.get([x / noise_scale, y / noise_scale, seed]);
      pt2(x as f32, y as f32)
    })
    .collect()
}

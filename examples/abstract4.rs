// cargo run --release --example abstract4
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::noise::{Fbm, NoiseFn};
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::brush::Brush;
use util::{captured_frame_path, Line2};

fn main() {
  nannou::app(model).run();
}

struct Model {
  max_radius: f32,
  min_radius: f32,
  width: f32,
  n_rings: i32,
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
  app.set_loop_mode(LoopMode::loop_ntimes(args.get_usize("loops", 1)));

  Model {
    max_radius: args.get("max-radius", 400.0),
    min_radius: args.get("min-radius", 50.0),
    width: args.get("width", 30.0),
    n_rings: args.get("rings", 5),
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  let win = app.window_rect();

  draw_paper_texture(&draw, model, &win);
  draw_circles(&draw, model);

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

// randomly generate short squiggles that are distored by Fbm noise (fractal brownian motion)
fn draw_paper_texture(draw: &Draw, _model: &Model, win: &Rect) {
  draw.background().color(hsl(0.15, 0.4, 0.965));
  let fbm = Fbm::new();
  let seed = random_f64();
  let noise_scale = 60.0;
  for _ in 0..=200 {
    let y = win.y.lerp(random_f32());
    let mut x = win.x.lerp(random_f32());
    let points = (0..=200).map(|_| {
      x += 1.;
      let noise = fbm.get([x as f64 / noise_scale, y as f64 / noise_scale, seed]) as f32;
      let y = y + noise * 10.;
      pt2(x, y)
    });
    draw.polyline().color(hsl(0., 0., 0.895)).points(points);
  }
}

fn draw_circles(draw: &Draw, model: &Model) {
  for i in 0..=model.n_rings {
    let points = points_for_ring(i, model);
    Brush::new()
      .width(model.width)
      .hsla(0., 0., 0.14, 1.0)
      .path(points, draw);
  }
}

// returns a closure suitable for a `map` function too convert degrees to points
fn point_mapper(radius: f32) -> impl Fn(i32) -> Point2 {
  move |deg| {
    let angle = deg_to_rad(deg as f32);
    let x = angle.cos() * radius;
    let y = angle.sin() * radius;
    pt2(x, y)
  }
}

// generates a line suitable for a polygon defining a "ring"
fn points_for_ring(i: i32, model: &Model) -> Line2 {
  let Model {
    max_radius,
    min_radius,
    n_rings,
    ..
  } = *model;
  let inner_radius = map_range(i, 0, n_rings, min_radius, max_radius);
  let start_degree = i * 20;
  let end_degree = start_degree + 330;
  (start_degree..end_degree)
    .map(point_mapper(inner_radius))
    .collect()
}

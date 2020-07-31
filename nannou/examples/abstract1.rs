// cargo run --release --example abstract1
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::noise::{Fbm, NoiseFn};
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::{captured_frame_path, smooth_by, Line2};

fn main() {
  nannou::app(model).run();
}

struct Model {
  max_radius: f32,
  min_radius: f32,
  padding: f32,
  seed: f64,
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
    max_radius: args.get("max-radius", 300.0),
    min_radius: args.get("min-radius", 50.0),
    padding: args.get("padding", 20.0),
    seed: args.get("seed", 10.0),
    n_rings: args.get("rings", 5),
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  let win = app.window_rect();

  draw_blue(&draw, &win);
  draw_sun(&draw);
  draw_orange(&draw, &win);
  draw_circles(&draw, model);

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

fn draw_blue(draw: &Draw, win: &Rect) {
  let n = 200;
  for i in 0..=n {
    let color = hsla(
      0.52,
      0.5,
      0.6 + map_range(i, 0, n, 0.0, 0.2),
      map_range(i, 0, n / 3, 0.01, 1.0),
    );
    draw
      .rect()
      .x_y(0.0, win.y.lerp(map_range(i, 0, n, 0.0, 0.7) + 0.3))
      .w_h(win.x.magnitude(), win.y.magnitude() / n as f32)
      .color(color);
  }
}

fn draw_sun(draw: &Draw) {
  let n = 50;
  for i in 0..=n {
    let color = hsla(0.13, 0.9, 0.8 + map_range(i, 0, n, 0.0, 0.2), 0.05);
    draw
      .ellipse()
      .x_y(100.0, 0.0)
      .radius(map_range(i, 0, n, 20.0, 400.0))
      .color(color);
  }
}

fn draw_orange(draw: &Draw, win: &Rect) {
  let n = 200;
  for i in 0..=n {
    let color = hsla(
      0.05,
      0.5,
      0.5 + map_range(i, 0, n, 0.0, 0.33),
      map_range(i, n / 3, n, 1.0, 0.01),
    );
    draw
      .rect()
      .x_y(0.0, win.y.lerp(map_range(i, 0, n, 0.0, 0.98)))
      .w_h(win.x.magnitude(), win.y.magnitude() / n as f32)
      .color(color);
  }
}

fn draw_circles(draw: &Draw, model: &Model) {
  for i in 0..=model.n_rings {
    let points = points_for_ring(i, model);
    draw
      .polygon()
      .stroke_weight(2.0)
      .stroke(hsl(0.0, 0.0, 0.2))
      .color(hsla(0.13, 0.9, 0.8, 0.15))
      .points(points);
  }
}

// returns a closure suitable for a `map` function too convert degrees to points
fn point_mapper(radius: f32, seed: f64) -> impl Fn(i32) -> Point2 {
  move |deg| {
    let angle = deg_to_rad(deg as f32);
    let x = angle.cos() * radius;
    let y = angle.sin() * radius;
    // commenting out the noise generation because it doesn't really add a lot.
    // I was going for a "hand drawn" look, which it kind of worked but wasn't really appropriate for this design
    // let fbm = Fbm::new();
    // let x = x + fbm.get([x as f64, x as f64, seed]) as f32 * 5.0;
    // let y = y + fbm.get([y as f64, y as f64, seed]) as f32 * 5.0;
    pt2(x, y)
  }
}

// generates a line suitable for a polygon defining a "ring"
fn points_for_ring(i: i32, model: &Model) -> Line2 {
  let Model {
    max_radius,
    min_radius,
    padding,
    n_rings,
    ..
  } = *model;
  let inner_radius = map_range(i, 0, n_rings, min_radius, max_radius);
  let outer_radius = map_range(i, 0, n_rings, min_radius, max_radius)
    + (max_radius - min_radius) / n_rings as f32
    - padding;
  let start_degree = i * 20;
  let end_degree = start_degree + 330;
  let inner_points = (start_degree..end_degree)
    .map(point_mapper(inner_radius, model.seed))
    .rev();
  let outer_points = (start_degree..end_degree).map(point_mapper(outer_radius, model.seed));
  inner_points.chain(outer_points).collect()
}

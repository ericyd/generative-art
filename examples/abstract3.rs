// Simple abstract drawing using a brush pattern
// cargo run --release --example abstract3 -- --palette colorlovers5
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::brush::Brush;
use util::color::*;
use util::{captured_frame_path, Line2};

fn main() {
  nannou::app(model).run();
}

struct Model {
  radius: f32,
  n_rings: i32,
  palette: String,
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
    radius: args.get("radius", 300.0),
    n_rings: args.get("rings", 20),
    palette: args.get_string("palette", "random"),
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
    let color = hsla(0.13, 1.0, 0.95 + map_range(i, 0, n, 0.0, 0.2), 0.05);
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
  let palette = get_palette(&model.palette);
  for i in 0..=model.n_rings {
    let points = points_for_ring(i, model);
    let (h, s, l) = random_color(palette).into_components();
    let h = h.to_positive_degrees() / 360.0;
    Brush::new()
      .width(random_range(20.0, 30.0))
      .hsla(h, s, l, 1.0)
      .path(points, &draw);
  }
}

// returns a closure suitable for a `map` function too convert degrees to points
fn point_mapper(i: i32, ring_radius: f32, model: &Model) -> impl Fn(i32) -> Point2 {
  // The goal here is to create an "offset" so the segments are rotated slightly which makes them lay together nicely.
  let start_rotation = 2.0 * PI / 3.0;
  let origin_x = map_range(
    i,
    0,
    model.n_rings,
    start_rotation,
    start_rotation + 2.0 * PI,
  )
  .cos()
    * model.radius
    / 4.0;
  let origin_y = map_range(
    i,
    0,
    model.n_rings,
    start_rotation,
    start_rotation + 2.0 * PI,
  )
  .sin()
    * model.radius
    / 4.0;
  move |deg| {
    let angle = deg_to_rad(deg as f32);
    let x = angle.cos() * ring_radius + origin_x;
    let y = angle.sin() * ring_radius + origin_y;
    pt2(x, y)
  }
}

// generates a line suitable for a polygon defining a "ring"
fn points_for_ring(i: i32, model: &Model) -> Line2 {
  let Model {
    radius, n_rings, ..
  } = *model;
  let inner_radius = radius;
  let start_degree = map_range(i, 0, n_rings, 0, 360 - rad_to_deg(PI / 3.0) as i32);
  let end_degree = start_degree + rad_to_deg(PI / 3.0) as i32;
  let inner_points = (start_degree..end_degree)
    .map(point_mapper(i, inner_radius, model))
    .rev();
  inner_points.collect()
}

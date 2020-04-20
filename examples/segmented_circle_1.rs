// cargo run --release --example segmented_circle_1 -- --hue 0.308 --sat 0.39 --lum 0.14
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::captured_frame_path;

fn main() {
  nannou::app(model).run();
}

struct Model {
  min_radius: f32,
  max_radius: f32,
  hue: f32,
  sat: f32,
  lum: f32,
  n_segments: i32,
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
    min_radius: args.get("min-radius", 50.),
    max_radius: args.get("max-radius", 500.),
    hue: args.get("hue", 0.35),
    sat: args.get("sat", 0.4),
    lum: args.get("lum", 0.4),
    n_segments: args.get("segments", 70),
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  // named colors https://docs.rs/nannou/0.13.1/nannou/color/named/index.html
  // FLORALWHITE
  // GHOSTWHITE
  // IVORY
  // MINTCREAM
  // WHITE
  // WHITESMOKE
  draw.background().color(IVORY);

  draw_segments(&draw, model);

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

/// draw all segments around the circle
fn draw_segments(draw: &Draw, model: &Model) {
  // TODO: explore having a random radians_per_segment
  let radians_per_segment = (2. * PI) / model.n_segments as f32;
  for n in 0..model.n_segments {
    let start_theta = n as f32 * radians_per_segment;
    let end_theta = (n + 1) as f32 * radians_per_segment;
    draw_segment(draw, start_theta, end_theta, model);
  }
}

/// draw single segment
/// Segments are polygons with an arc on top and bottom
fn draw_segment(draw: &Draw, start: f32, end: f32, model: &Model) {
  let min_radius = random_range(model.min_radius, (model.max_radius - model.min_radius) / 2.);
  let inner_points = (0..20)
    .map(|n| {
      // to make border-less, change to ` / 19.`
      let factor = n as f32 / 20.;
      let theta = start + factor * (end - start);
      let x = theta.cos() * min_radius;
      let y = theta.sin() * min_radius;
      pt2(x, y)
    })
    // reverse so the polygon is concave, since inner and outer points are drawn in the same direction
    .rev();

  let max_radius = random_range((model.max_radius - model.min_radius) / 2., model.max_radius);
  let outer_points = (0..20).map(|n| {
    // to make border-less, change to ` / 19.`
    let factor = n as f32 / 20.;
    let theta = start + factor * (end - start);
    let x = theta.cos() * max_radius;
    let y = theta.sin() * max_radius;
    pt2(x, y)
  });

  let points: Vec<Point2> = inner_points.chain(outer_points).collect();

  let hue = random_range(model.hue * 0.95, model.hue * 1.05);
  let sat = random_range(model.sat * 0.9, model.sat * 1.1);
  let lum = random_range(model.lum * 0.9, model.lum * 1.1);
  draw.polygon().color(hsl(hue, sat, lum)).points(points);
}

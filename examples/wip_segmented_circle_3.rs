// cargo run --release --example segmented_circle_2 -- --segments 80 --hue1 0.09 --hue2 0.20 --hue3 0.29 --hue4 0.98
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::captured_frame_path;
use util::interp::lerp;

fn main() {
  nannou::app(model).run();
}

struct Model {
  min_radius: f32,
  max_radius: f32,
  hue1: f32,
  hue2: f32,
  hue3: f32,
  hue4: f32,
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
    max_radius: args.get("max-radius", 150.),
    hue1: args.get("hue1", 0.05),
    hue2: args.get("hue2", 0.15),
    hue3: args.get("hue3", 0.35),
    hue4: args.get("hue4", 0.989),
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
  draw.background().color(GHOSTWHITE);

  draw_segments(&draw, model, -200., 0., 0.);
  draw_segments(&draw, model, 200., 0., PI);

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

/// draw all segments around the circle
fn draw_segments(draw: &Draw, model: &Model, x: f32, y: f32, rotation: f32) {
  // TODO: explore having a random radians_per_segment
  let radians_per_segment = (2. * PI) / model.n_segments as f32;
  for n in 0..model.n_segments {
    let factor = n as f32 / model.n_segments as f32;
    let start_theta = n as f32 * radians_per_segment;
    let end_theta = (n + 1) as f32 * radians_per_segment;

    let max_radius = model.max_radius;
    let min_radius = lerp(model.min_radius, model.max_radius, factor * 2.);
    // interesting alternative
    // let min_radius = lerp(model.min_radius, model.max_radius, factor);

    draw_segment(
      model.hue4,
      model.sat,
      model.lum,
      min_radius,
      max_radius,
      start_theta,
      end_theta,
      x,
      y,
      rotation,
      draw,
    );
  }
}

// Not sure if it's better to return the closure with this `impl Fn` notation
// as recommended by this
// https://doc.rust-lang.org/stable/rust-by-example/fn/closures/output_parameters.html
// or by wrapping in a Box as recommended by this
// https://doc.rust-lang.org/book/ch19-05-advanced-functions-and-closures.html
//
// I think the impl Fn is a bit cleaner looking and also a little less verbose
// so I'm going with that style here
fn point_mapper(
  radius: f32,
  start_theta: f32,
  end_theta: f32,
  x: f32,
  y: f32,
  n_points: f32,
) -> impl Fn(i32) -> Point2 {
  move |n| {
    let factor = n as f32 / n_points;
    let theta = start_theta + factor * (end_theta - start_theta);
    let x = theta.cos() * radius + x;
    let y = theta.sin() * radius + y;
    pt2(x, y)
  }
}

fn draw_segment(
  hue: f32,
  sat: f32,
  lum: f32,
  min_radius: f32,
  max_radius: f32,
  start: f32,
  end: f32,
  x: f32,
  y: f32,
  rotation: f32,
  draw: &Draw,
) {
  let total_points = 20;
  // when this is 1 less than total_points, it results in a "borderless" feel.
  // when it is equal, there is a "borders" feel
  let points_per_segment = 20.;
  // reverse these points so the resulting polygon is concave
  let inner_points = (0..total_points)
    .map(point_mapper(
      min_radius,
      start,
      end,
      x,
      y,
      points_per_segment,
    ))
    .rev();
  let outer_points = (0..total_points).map(point_mapper(
    max_radius,
    start,
    end,
    x,
    y,
    points_per_segment,
  ));
  let points: Vec<Point2> = inner_points
    .chain(outer_points)
    .map(|pt| {
      let (rotated_x, rotated_y) = rotate(pt.x, pt.y, x, y, rotation);
      pt2(rotated_x, rotated_y)
    })
    .collect();

  let hue = random_range(hue * 0.95, hue * 1.05);
  let sat = random_range(sat * 0.9, sat * 1.1);
  let lum = random_range(lum * 0.9, lum * 1.1);
  draw.polygon().color(hsl(hue, sat, lum)).points(points);
}

// SO FTW https://stackoverflow.com/a/2259502
fn rotate(x: f32, y: f32, origin_x: f32, origin_y: f32, radians: f32) -> (f32, f32) {
  let sin = radians.sin();
  let cos = radians.cos();

  // translate point back to origin:
  let x = x - origin_x;
  let y = y - origin_y;

  // rotate point
  let xnew = x * cos - y * sin;
  let ynew = x * sin + y * cos;

  // translate point back:
  let x = xnew + origin_x;
  let y = ynew + origin_y;
  (x, y)
}

fn draw_frame(draw: &Draw, win: Rect) {
  draw
    .rect()
    .color(hsla(0., 0., 0., 0.))
    .stroke(hsl(0., 0., 0.))
    .stroke_weight(15.0)
    .w_h(win.w() * 0.85, win.h() * 0.85);

  draw
    .rect()
    .color(hsla(0., 0., 0., 0.))
    .stroke(hsl(0., 1., 1.))
    .stroke_weight(300.0)
    .w_h(win.w() * 0.85 + 300., win.h() * 0.85 + 300.);
}

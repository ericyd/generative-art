// cargo run --release --example segmented_circle_2 -- --segments 80 --hue1 0.09 --hue2 0.20 --hue3 0.29 --hue4 0.98
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
    max_radius: args.get("max-radius", 500.),
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
  draw.background().color(BLACK);

  draw_segments(&draw, model);
  draw_frame(&draw, app.window_rect());

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
    // draw_segments_for_radians(draw, start_theta, end_theta, model);
    for (min_rad, max_rad, hue) in calculate_segments_for_radians(model) {
      draw_segment(
        hue,
        model.sat,
        model.lum,
        min_rad,
        max_rad,
        start_theta,
        end_theta,
        draw,
      );
    }
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
  n_points: f32,
) -> impl Fn(i32) -> Point2 {
  move |n| {
    let factor = n as f32 / n_points;
    let theta = start_theta + factor * (end_theta - start_theta);
    let x = theta.cos() * radius;
    let y = theta.sin() * radius;
    pt2(x, y)
  }
}

/// draw single segment
/// Segments are polygons with an arc on top and bottom
fn calculate_segments_for_radians(model: &Model) -> Vec<(f32, f32, f32)> {
  let spacing = 1.;
  // It seems weird to calculate the "3rd" ring first,
  // but it leads to better visual balance in the end result.
  // The ring number corresponds to it's level from the center (center is 1)
  let ring3_min_radius = random_range(model.min_radius, (model.max_radius - model.min_radius) / 2.);
  let ring3_max_radius = random_range((model.max_radius - model.min_radius) / 2., model.max_radius);
  let ring3_props = (ring3_min_radius, ring3_max_radius, model.hue3);

  let ring2_min_radius = random_range(0.1, ring3_min_radius / 2.);
  let ring2_max_radius = ring3_min_radius - spacing;
  let ring2_props = (ring2_min_radius, ring2_max_radius, model.hue2);

  let ring1_min_radius = 0.;
  let ring1_max_radius = ring2_min_radius - spacing;
  let ring1_props = (ring1_min_radius, ring1_max_radius, model.hue1);

  let ring4_min_radius = ring3_max_radius + spacing;
  let ring4_max_radius = ring3_max_radius * 3.;
  let ring4_props = (ring4_min_radius, ring4_max_radius, model.hue4);
  vec![ring1_props, ring2_props, ring3_props, ring4_props]
}

fn draw_segment(
  hue: f32,
  sat: f32,
  lum: f32,
  min_radius: f32,
  max_radius: f32,
  start: f32,
  end: f32,
  draw: &Draw,
) {
  let total_points = 20;
  // when this is 1 less than total_points, it results in a "borderless" feel.
  // when it is equal, there is a "borders" feel
  let points_per_segment = 20.;
  // reverse these points so the resulting polygon is concave
  let inner_points = (0..total_points)
    .map(point_mapper(min_radius, start, end, points_per_segment))
    .rev();
  let outer_points =
    (0..total_points).map(point_mapper(max_radius, start, end, points_per_segment));
  let points: Vec<Point2> = inner_points.chain(outer_points).collect();

  let hue = random_range(hue * 0.95, hue * 1.05);
  let sat = random_range(sat * 0.9, sat * 1.1);
  let lum = random_range(lum * 0.9, lum * 1.1);
  draw.polygon().color(hsl(hue, sat, lum)).points(points);
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

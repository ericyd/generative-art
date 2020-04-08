// cargo run --release --example tree_stump
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::noise::{NoiseFn, Perlin};
use nannou::prelude::*;
use std::collections::HashMap;
use std::env;

mod util;
use util::blob::Blob;
use util::captured_frame_path;
use util::interp::{lerp, Interp, Interpolate};

fn main() {
  nannou::app(model).view(view).size(1024, 1024).run();
}

struct Model {
  _a: WindowId,
  radius: f32,
  seed: f32,
}

fn model(app: &App) -> Model {
  // simple argument collector
  let args: Vec<String> = env::args().collect();
  let arg_map = args
    .iter()
    .enumerate()
    .fold(HashMap::new(), |mut map, (i, arg)| match arg.get(0..2) {
      Some(slice) if Some(slice) == Some("--") => {
        if i >= env::args().len() - 1 {
          map
        } else {
          map.insert(arg.get(2..).unwrap(), args[i + 1].clone());
          map
        }
      }
      _ => map,
    });

  let radius = match arg_map.get("radius") {
    Some(num) => num.parse::<f32>().unwrap(),
    None => 900.,
  };

  let seed = match arg_map.get("seed") {
    Some(num) => num.parse::<f32>().unwrap(),
    None => 35.,
  };

  let _a = app.new_window().title("window a").build().unwrap();

  Model { _a, radius, seed }
}

// the only thing this should do is draw the model.grid (or maybe model.next)
fn view(app: &App, model: &Model, frame: Frame) {
  let win = app.window_rect();
  let perlin = Perlin::new();
  app.set_loop_mode(LoopMode::loop_ntimes(3));

  // Prepare to draw.
  let draw = app.draw();

  // Set background
  draw.background().color(BLACK);

  let noise_scale = 1.0;

  // draw concentric blobs
  (0..30).rev().for_each(|n| {
    let factor = n as f32 / 30.;
    let radius = Interp::reverse_exp(model.radius as f32, 1.0, factor);
    let hue = lerp(290. / 360., 388. / 360., factor);
    let sat = lerp(0.62, 0.77, factor);
    let noise = lerp(0., noise_scale, factor);
    let points = Blob::new()
      .x_y(0., 0.)
      .radius(radius)
      .noise_scale(noise)
      .seed(model.seed)
      .points();
    draw
      .polygon()
      .hsla(hue, sat, 0.5, 1.0)
      .stroke(hsl(hue, 0.7, 0.4))
      .stroke_weight(1.0)
      .points(points);
  });

  // draw top edge
  // polygon that extends outside the bounds of the window, with bottom edge that is sinusoidal
  let num_points = 360;
  let seed = random_f64();
  let mut top_polygon_points: Vec<Vector2> = (0..num_points)
    .map(|n| {
      let factor = n as f32 / num_points as f32;
      let x = lerp(win.x.start - 100., win.x.end + 100., factor);
      let y_base = (x / 300.).sin();
      let noise = perlin.get([x as f64 / 500., y_base as f64 / 300., seed]) as f32 * 1.8;
      let y = y_base * noise * 100. + 250.;
      pt2(x, y)
    })
    .collect(); // ends at middle right
  top_polygon_points.push(pt2(win.x.end + 100., win.y.end + 100.)); // top right
  top_polygon_points.push(pt2(win.x.start - 100., win.y.end + 100.)); // top left
  top_polygon_points.push(pt2(win.x.start - 100., (win.x.start - 100.).sin() * 100.)); // middle left

  // draw bottom edge
  // polygon that extends outside the bounds of the window, with top edge that is sinusoidal
  let seed = random_f64();
  let mut bottom_polygon_points: Vec<Vector2> = (0..num_points)
    .map(|n| {
      let factor = n as f32 / num_points as f32;
      let x = lerp(win.x.start - 100., win.x.end + 100., factor);
      let y_base = (x / 300.).sin();
      let noise = perlin.get([x as f64 / 300., y_base as f64 / 300., seed]) as f32;
      let y = y_base * noise * 100. - 250.;
      pt2(x, y)
    })
    .collect(); // ends at middle right
  bottom_polygon_points.push(pt2(win.x.end + 100., win.y.start - 100.)); // bottom right
  bottom_polygon_points.push(pt2(win.x.start - 100., win.y.start - 100.)); // bottom left
  bottom_polygon_points.push(pt2(win.x.start - 100., (win.x.start - 100.).sin() * 100.)); // middle left

  // top polygon and lines
  draw
    .polygon()
    .hsla(271. / 360., 0.35, 0.15, 1.0)
    .points(top_polygon_points.clone());

  // don't draw lines for last 3 points because they are the edges of the polygon, not the sine wave
  &top_polygon_points[0..(&top_polygon_points.len() - 3)]
    .iter()
    .for_each(|pt| {
      draw
        .line()
        .start(*pt)
        .end(pt2(pt.x - 500., pt.y + 500.))
        .color(hsla(271. / 360., 0.25, 0.10, 1.0));
    });

  // bottom polygon and lines
  draw
    .polygon()
    .hsla(228. / 360., 0.35, 0.19, 1.0)
    .points(bottom_polygon_points.clone());

  // don't draw lines for last 3 points because they are the edges of the polygon, not the sine wave
  &bottom_polygon_points[0..(bottom_polygon_points.len() - 3)]
    .iter()
    .for_each(|pt| {
      draw
        .line()
        .start(*pt)
        .end(pt2(pt.x + 500., pt.y - 500.))
        .color(hsla(228. / 360., 0.25, 0.10, 1.0));
    });

  // Write to the window frame.
  draw.to_frame(app, &frame).unwrap();

  let file_path = captured_frame_path(app, &frame);
  app.main_window().capture_frame(file_path);
}

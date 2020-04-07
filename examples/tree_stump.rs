// cargo run --release --example tree_stump
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::prelude::*;
use std::collections::HashMap;
use std::env;

mod util;
use util::blob::Blob;
use util::captured_frame_path;
use util::interp::lerp;

fn main() {
  nannou::app(model).view(view).size(1024, 1024).run();
}

struct Model {
  _a: WindowId,
  x_origin: f32,
  y_origin: f32,
  radius: i32,
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

  let x_origin = match arg_map.get("x-origin") {
    Some(num) => num.parse::<f32>().unwrap(),
    None => -400.,
  };

  let y_origin = match arg_map.get("y-origin") {
    Some(num) => num.parse::<f32>().unwrap(),
    None => -340.,
  };

  let radius = match arg_map.get("radius") {
    Some(num) => num.parse::<i32>().unwrap(),
    None => 1300,
  };

  let seed = match arg_map.get("seed") {
    Some(num) => num.parse::<f32>().unwrap(),
    None => 35.,
  };

  let _a = app.new_window().title("window a").build().unwrap();

  Model {
    _a,
    x_origin,
    y_origin,
    radius,
    seed,
  }
}

// the only thing this should do is draw the model.grid (or maybe model.next)
fn view(app: &App, model: &Model, frame: Frame) {
  app.set_loop_mode(LoopMode::NTimes {
    // two frames are necessary for capture_frame to work properly
    number_of_updates: 2,
  });

  // Prepare to draw.
  let draw = app.draw();

  // Set background
  let bg = hsl(7. / 255., 0.68, 0.15); // burnt umber
  let bg = hsl(208. / 360., 0.34, 0.51); // nice blue
  let bg = hsl(280. / 255., 0.64, 0.53); // nice yellow
  draw.background().color(bg);

  let noise_scale = 0.8;
  (0..model.radius).step_by(10).rev().for_each(|n| {
    let factor = n as f32 / model.radius as f32;
    let hue = lerp(27. / 255., 35. / 255., factor);
    let sat = lerp(0.42, 0.34, factor);
    let lightness = lerp(0.74, 0.53, factor);
    let noise = lerp(noise_scale * 0.98, noise_scale * 1.02, random_f32());
    // set a few props uniquely if it's the outermost ring
    let is_outer_ring = n >= model.radius - 10;
    let fuzziness = if is_outer_ring { 10.0 } else { 0.0 };
    let stroke_weight = if is_outer_ring {
      6.
    } else {
      lerp(1.0, 2.0, factor)
    };
    let stroke_sat = if is_outer_ring { 0.7 } else { 0.34 };
    let stroke_lightness = if is_outer_ring { 0.24 } else { 0.43 };
    let points = Blob::new()
      .x_y(model.x_origin, model.y_origin)
      .radius(n as f32)
      .noise_scale(noise)
      .seed(model.seed)
      .fuzziness(fuzziness)
      .points();
    draw
      .polygon()
      .hsla(hue, sat, lightness, 1.0)
      .stroke(hsl(27. / 255., stroke_sat, stroke_lightness))
      .stroke_weight(stroke_weight)
      .points(points);
  });

  // Write to the window frame.
  draw.to_frame(app, &frame).unwrap();

  let file_path = captured_frame_path(app, &frame);
  app.main_window().capture_frame_threaded(file_path);
}

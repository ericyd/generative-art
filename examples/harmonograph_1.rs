// cargo run --release --example harmonograph_1 -- --radius 1
//
// some nice designs based on the concept of a harmonograph
// ref: https://en.wikipedia.org/wiki/Harmonograph
//
// examples with arguments (after being built)
//  dots
//    ./target/release/examples/harmonograph_1 --radius 7000 --y-origin 5900 --x-mod-depth 16.001 -y-mod-depth 15.998 --resolution 5 --line-length 50 --line-weight 1 --solid false
//  solid line
//    ./target/release/examples/harmonograph_1 --radius 7000 --y-origin 5900 --x-mod-depth 16.001 -y-mod-depth 15.998 --resolution 50 --line-length 50 --line-weight 2
//  with different oscillator
//    ./target/release/examples/harmonograph_1 --x-oscillator sin --y-oscillator sin --x-mod-depth 1.750001 --line-length 2000
//  with different color spectrum
//    ./target/release/examples/harmonograph_1 --y-oscillator cos --x-mod-depth 1.750001 --line-length 200 --color-start 0.2 --color-end 0.7

extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::prelude::*;
use std::collections::HashMap;
use std::env;

mod util;
use util::captured_frame_path;
use util::interp::lerp;

type Oscillator = fn(f32) -> f32;

fn osc_sin(t: f32) -> f32 {
  t.sin()
}

fn osc_cos(t: f32) -> f32 {
  t.cos()
}

fn osc_half_sin(t: f32) -> f32 {
  (t.sin() + 1.) / 2.
}

fn osc_collapse(t: f32) -> f32 {
  t.sin() * t.powf(-0.35)
}

fn main() {
  nannou::app(model).view(view).size(1024, 1024).run();
}

struct Model {
  _a: WindowId,
  radius: f32,
  line_weight: f32,
  line_length: i32,
  x_mod_depth: f32,
  y_mod_depth: f32,
  x_origin: f32,
  y_origin: f32,
  resolution: f32,
  solid: bool,
  x_oscillator: String,
  y_oscillator: String,
  color_start: f32,
  color_end: f32,
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

  // set default values for drawing, using args if available
  let radius = match arg_map.get("radius") {
    Some(num) => num.parse::<f32>().unwrap(),
    None => 300.,
  };

  let line_weight = match arg_map.get("line-weight") {
    Some(num) => num.parse::<f32>().unwrap(),
    None => 1.,
  };

  let line_length = match arg_map.get("line-length") {
    Some(num) => num.parse::<i32>().unwrap(),
    None => 100,
  };

  let x_mod_depth = match arg_map.get("x-mod-depth") {
    Some(num) => num.parse::<f32>().unwrap(),
    None => 2.002,
  };

  let y_mod_depth = match arg_map.get("y-mod-depth") {
    Some(num) => num.parse::<f32>().unwrap(),
    None => 2.002,
  };

  let x_origin = match arg_map.get("x-origin") {
    Some(num) => num.parse::<f32>().unwrap(),
    None => 0.,
  };

  let y_origin = match arg_map.get("y-origin") {
    Some(num) => num.parse::<f32>().unwrap(),
    None => 0.,
  };

  let resolution = match arg_map.get("resolution") {
    Some(num) => num.parse::<f32>().unwrap(),
    None => 1.,
  };

  let solid = match arg_map.get("solid") {
    Some(thing) => thing.parse::<bool>().unwrap(),
    None => true,
  };

  let x_oscillator = match arg_map.get("x-oscillator") {
    Some(thing) => thing,
    None => "sin",
  }
  .to_string();

  let y_oscillator = match arg_map.get("y-oscillator") {
    Some(thing) => thing,
    None => "collapse",
  }
  .to_string();

  let color_start = match arg_map.get("color-start") {
    Some(num) => num.parse::<f32>().unwrap(),
    None => 0.0,
  };

  let color_end = match arg_map.get("color-end") {
    Some(num) => num.parse::<f32>().unwrap(),
    None => 1.0,
  };

  let _a = app.new_window().title("window a").build().unwrap();
  Model {
    _a,
    radius,
    line_weight,
    line_length,
    x_mod_depth,
    y_mod_depth,
    x_origin,
    y_origin,
    resolution,
    solid,
    x_oscillator,
    y_oscillator,
    color_start,
    color_end,
  }
}

// the only thing this should do is draw the model.grid (or maybe model.next)
fn view(app: &App, model: &Model, frame: Frame) {
  let win = app.window_rect();
  app.set_loop_mode(LoopMode::NTimes {
    // two frames are necessary for capture_frame to work properly
    number_of_updates: 2,
  });

  // Prepare to draw.
  let draw = app.draw();

  // Clear the background to black.
  draw.background().color(BLACK);

  // not sure how to make this work the way I want it
  let mut oscillator_map: HashMap<String, Oscillator> = HashMap::new();
  oscillator_map.insert(String::from("sin"), osc_sin);
  oscillator_map.insert(String::from("cos"), osc_cos);
  oscillator_map.insert(String::from("half-sin"), osc_half_sin);
  oscillator_map.insert(String::from("collapse"), osc_collapse);

  let x_modulator = match oscillator_map.get(&model.x_oscillator) {
    Some(osc) => osc,
    None => {
      println!(
        "Oscillator value '{}' is invald. Valid options are:",
        &model.x_oscillator
      );
      oscillator_map.keys().for_each(|key| println!("{}", key));
      std::process::exit(0);
    }
  };
  let y_modulator = match oscillator_map.get(&model.y_oscillator) {
    Some(osc) => osc,
    None => {
      println!(
        "Oscillator value '{}' is invald. Valid options are:",
        &model.y_oscillator
      );
      oscillator_map.keys().for_each(|key| println!("{}", key));
      std::process::exit(0);
    }
  };

  let points = (0..=(model.line_length * 360 * model.resolution as i32)).map(|i| {
    let theta: f32 = deg_to_rad(i as f32) / model.resolution;
    let x = theta.cos() * model.radius * x_modulator(theta * model.x_mod_depth) + model.x_origin;
    let y = theta.sin() * model.radius * y_modulator(theta * model.y_mod_depth) + model.y_origin;
    let point = pt2(x, y);
    let hue = lerp(
      model.color_start,
      model.color_end,
      i as f32 / 360. / model.line_length as f32 / model.resolution,
    );
    let color = hsl(hue, 0.5, 0.5);
    (point, color)
  });

  if model.solid {
    // do not filter points that are offscreen because it can cause unexpected line artifacts
    draw
      .polyline()
      .start_cap_round()
      .weight(model.line_weight)
      .colored_points(points);
  } else {
    // filter points that are offscreen for quicker rendering
    points
      .filter(|(pt, _color)| {
        pt.x > win.x.start && pt.x < win.x.end && pt.y > win.y.start && pt.y < win.y.end
      })
      .for_each(|(pt, color)| {
        draw
          .ellipse()
          .x_y(pt.x, pt.y)
          .color(color)
          .radius(model.line_weight);
      })
  }

  // Write to the window frame.
  draw.to_frame(app, &frame).unwrap();

  let file_path = captured_frame_path(app, &frame);
  app.main_window().capture_frame_threaded(file_path);
}

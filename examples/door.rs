// cargo run --release --example door
//
// Notes
// I made this as part of #the100DayProject so I'm intentionally trying to keep it minimalist.
// I may iterate on it later if time/interest permits.
// Things I'd like to add/change:
//   - clean up code: tons of duplicate code, and probably some poorly written stuff in there somewhere
//   - have flowlines start from more places than just the edge of the door. Ideally would
//      radiate outwards
//   - more interesting background than all black. Though, not exactly sure what it should be
//
// Also for some reason it's crashing repeatedly due to out of memory errors when looping 2 or more times
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::noise::{NoiseFn, Perlin};
use nannou::prelude::*;
use std::collections::HashMap;
use std::env;

mod util;
use util::captured_frame_path;
use util::interp::lerp;

const WIDTH: u32 = 1024;
const HEIGHT: u32 = 1024;

fn main() {
  nannou::app(model).view(view).size(WIDTH, HEIGHT).run();
}

struct Model {
  _a: WindowId,
  seed: f32,
  noise_scale: f32,
  num_steps: i32,
  default_length: f32,
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

  let seed = match arg_map.get("seed") {
    Some(num) => num.parse::<f32>().unwrap(),
    None => random_f32() * 100.,
  };

  let noise_scale = match arg_map.get("noise") {
    Some(num) => num.parse::<f32>().unwrap(),
    None => 300.,
  };

  // number of vertices in each polyline
  let num_steps = match arg_map.get("steps") {
    Some(num) => num.parse::<i32>().unwrap(),
    None => 500,
  };

  // distance between each vertex in polyline
  let default_length = match arg_map.get("length") {
    Some(num) => num.parse::<f32>().unwrap(),
    None => 2.,
  };

  let _a = app.new_window().title("window a").build().unwrap();

  Model {
    _a,
    seed,
    noise_scale,
    num_steps,
    default_length,
  }
}

const DOOR_TOP_RIGHT: [f32; 2] = [300., -100.];
const DOOR_TOP_LEFT: [f32; 2] = [DOOR_TOP_RIGHT[0] - 160., DOOR_TOP_RIGHT[1]];
const DOOR_BOTTOM_LEFT: [f32; 2] = [DOOR_TOP_LEFT[0], DOOR_TOP_RIGHT[1] - 200.];

// points to outline a door
fn door(win: &Rect) -> Vec<Vector2> {
  vec![
    pt2(DOOR_TOP_RIGHT[0], DOOR_TOP_RIGHT[1]),
    pt2(DOOR_TOP_LEFT[0], DOOR_TOP_LEFT[1]),
    pt2(DOOR_BOTTOM_LEFT[0], DOOR_BOTTOM_LEFT[1]),
    pt2(win.x.start, win.y.start + 45.), // ray 1
    pt2(win.x.start, win.y.start + 20.),
    pt2(DOOR_BOTTOM_LEFT[0] + 5., DOOR_BOTTOM_LEFT[1]),
    pt2(win.x.start - 35., win.y.start + 0.), // ray 2
    pt2(win.x.start + 25., win.y.start),
    pt2(DOOR_BOTTOM_LEFT[0] + 10., DOOR_BOTTOM_LEFT[1]),
    pt2(win.x.start + 50., win.y.start), // ray 3
    pt2(win.x.start + 100., win.y.start),
    pt2(DOOR_BOTTOM_LEFT[0] + 15., DOOR_BOTTOM_LEFT[1]),
    pt2(DOOR_TOP_LEFT[0] + 15., DOOR_TOP_LEFT[1] - 10.), // inner top left
    pt2(DOOR_TOP_RIGHT[0], DOOR_TOP_RIGHT[1]),           // close shape
  ]
}

fn field(perlin: &Perlin, x: f32, y: f32, seed: f32) -> f32 {
  perlin.get([x as f64, y as f64, seed as f64]) as f32
}

// the only thing this should do is draw the model.grid (or maybe model.next)
fn view(app: &App, model: &Model, frame: Frame) {
  let Model {
    seed,
    noise_scale,
    num_steps,
    default_length,
    ..
  } = *model;
  let win = app.window_rect();
  let perlin = Perlin::new();
  app.set_loop_mode(LoopMode::loop_ntimes(1));

  // Prepare to draw.
  let draw = app.draw();

  // Set background
  draw.background().color(BLACK);

  let nx = 100;
  let ny = 100;

  let randomize_start = { || (random_f32() * 7.0) - 3.5 };

  // door
  draw
    .polygon()
    .hsla(41. / 360., 1., 0.68, 1.0)
    .points(door(&win));

  // draw the flow lines from the left border
  for _i in 0..nx {
    for j in 0..ny {
      let y_factor = j as f32 / ny as f32;

      let mut x = DOOR_TOP_LEFT[0];
      let mut y = lerp(DOOR_BOTTOM_LEFT[1], DOOR_TOP_LEFT[1], y_factor) + randomize_start();

      let hue = lerp(273. / 360., 170. / 360., y_factor);
      let color = hsla(hue, 0.5, 0.38, 0.03);
      let line_weight = 1.0;

      // TODO: could this method of generating points (opposed to a mutable vector) be the cause of the out of memory issues?
      let mut points = vec![(pt2(x, y), color)];
      (0..num_steps).for_each(|_| {
        let theta = PI * 2. / 3.;

        // noisy weighted gradient pointing away from center
        let noise_factor = 1.0;
        let theta_factor = 1.0;
        let angle = field(&perlin, x / noise_scale, y / noise_scale, seed) * noise_factor
          + theta * theta_factor;

        x = angle.cos() * default_length + x;
        y = angle.sin() * default_length + y;

        points.push((pt2(x, y), color));
      });

      draw
        .polyline()
        .start_cap_round()
        .weight(line_weight)
        .colored_points(points);
    }
  }

  // draw the flow lines from the top border
  //
  // this is such ridiculous duplicate code, but... also it's fine
  for i in 0..nx {
    let x_factor = i as f32 / nx as f32;

    for _j in 0..ny {
      let mut x = lerp(DOOR_TOP_LEFT[0], DOOR_TOP_RIGHT[0], x_factor) + randomize_start();
      let mut y = DOOR_TOP_LEFT[1];

      let hue = lerp(170. / 360., 273. / 360., x_factor);
      let color = hsla(hue, 0.5, 0.38, 0.03);
      let line_weight = 1.0;

      let mut points = vec![(pt2(x, y), color)];
      (0..num_steps).for_each(|_| {
        let theta = PI * 2. / 3.;

        // noisy weighted gradient pointing away from center
        let noise_factor = 1.0;
        let theta_factor = 1.0;
        let angle = field(&perlin, x / noise_scale, y / noise_scale, seed) * noise_factor
          + theta * theta_factor;

        x = angle.cos() * default_length + x;
        y = angle.sin() * default_length + y;

        points.push((pt2(x, y), color));
      });

      draw
        .polyline()
        .start_cap_round()
        .weight(line_weight)
        .colored_points(points);
    }
  }

  // Write to the window frame.
  draw.to_frame(app, &frame).unwrap();

  let file_path = captured_frame_path(app, &frame);
  app.main_window().capture_frame(file_path);
}

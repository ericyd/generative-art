// cargo run --release --example brush_1 -- --nx 8 --ny 23 --frequency 0.5 --persistence 0.8 --lacunarity 1.32 --seed 12 --steps 300
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::noise::{Fbm, MultiFractal, NoiseFn};
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::brush::*;
use util::captured_frame_path;
use util::color::*;
use util::interp::lerp;

const HEIGHT: u32 = 1024;
const WIDTH: u32 = 1024;

fn main() {
  nannou::app(model).run();
}

struct Model {
  seed: f64,
  octaves: usize,
  frequency: f64,
  lacunarity: f64,
  persistence: f64,
  nx: i32,
  ny: i32,
  max_steps: i32,
  max_width: f32,
  palette: String,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();

  app
    .new_window()
    .title(app.exe_name().unwrap())
    .view(view)
    .size(WIDTH, HEIGHT)
    .build()
    .unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));

  Model {
    seed: args.get("seed", 64.),
    // These are the defaults from the source for Fbm struct
    // pub const DEFAULT_OCTAVE_COUNT: usize = 6;
    // pub const DEFAULT_FREQUENCY: f64 = 1.0;
    // pub const DEFAULT_LACUNARITY: f64 = std::f64::consts::PI * 2.0 / 3.0;
    // pub const DEFAULT_PERSISTENCE: f64 = 0.5;
    // pub const MAX_OCTAVES: usize = 32;
    //
    // Notes to self
    // low persistence w/ low lacunarity = longer "wavelength"
    octaves: args.get("octaves", 6),
    frequency: args.get("frequency", 0.7),
    lacunarity: args.get("lacunarity", std::f64::consts::PI / 2.0),
    persistence: args.get("persistence", 0.3),
    nx: args.get("nx", 20),
    ny: args.get("ny", 20),
    max_steps: args.get("steps", 150),
    max_width: args.get("width", 30.),
    palette: args.get_string("palette", "muzli16"),
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  let Model {
    nx,
    ny,
    octaves,
    frequency,
    lacunarity,
    persistence,
    max_steps,
    max_width,
    ..
  } = *model;
  let draw = app.draw();
  let cream = hsl(47. / 360., 1., 0.98);
  draw.background().color(cream);
  let win = app.window_rect();

  let fbm = Fbm::new()
    .set_octaves(octaves)
    .set_frequency(frequency)
    .set_lacunarity(lacunarity)
    .set_persistence(persistence);

  let palette = get_palette(&model.palette);

  let default_length = 1.;
  for i in 0..nx {
    let x_factor = i as f32 / nx as f32;

    for j in 0..ny {
      let y_factor = j as f32 / ny as f32;
      let n_steps = random_range(75, max_steps);

      let mut x = lerp(win.x.start + 100., win.x.end - 100., x_factor) + random_range(-15., 15.);
      let mut y = lerp(win.y.start + 150., win.y.end - 150., y_factor) + random_range(-15., 15.);

      let mut points = Vec::new();

      let (hue, sat, lum) = random_color(palette).into_components();
      points.push(pt2(x, y));

      for _n in 0..n_steps {
        let angle = fbm.get([x as f64 / 100., y as f64 / 100., model.seed]) as f32;

        x = angle.cos() * default_length + x;
        y = angle.sin() * default_length + y;

        points.push(pt2(x, y));
      }

      let brush_width = random_range(10., max_width);
      Brush::new()
        .width(brush_width)
        .hsla(hue.to_positive_degrees() / 360., sat, lum, 1.0)
        .path(points, &draw);
    }
  }

  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

// experimenting with paper like textures
// cargo run --release --example falling_lines -- --n-lines 373 --max-weight 3.296289 --min-weight 0.5323126 --noise-scale 211.4442238894469 --seed 1678.9039968960299
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::noise::{NoiseFn, OpenSimplex};
use nannou::prelude::*;
use nannou::Draw;

mod util;
use util::args::ArgParser;
use util::draw_paper_texture;
use util::interp::{Interp, Interpolate};
use util::{capture_model, captured_frame_path};

const WIDTH: u32 = 1024;
const HEIGHT: u32 = 1024;

fn main() {
  nannou::app(model).update(update).run();
}

#[derive(Debug)]
struct Model {
  n_lines: usize,
  max_weight: f32,
  min_weight: f32,
  noise_scale: f64,
  seed: f64,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  app
    .new_window()
    .size(WIDTH, HEIGHT)
    .title(app.exe_name().unwrap())
    .view(view)
    .build()
    .unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));

  Model {
    n_lines: args.get("n-lines", 50),
    max_weight: args.get("max-weight", 4.0),
    min_weight: args.get("min-weight", 0.8),
    seed: args.get("seed", random_range(1.0, 10000.0)),
    noise_scale: args.get("noise-scale", random_range(50.0, 250.0)),
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  let args = ArgParser::new();
  model.n_lines = args.get("n-lines", random_range(40, 400));
  model.max_weight = args.get("max-weight", random_range(3.0, 6.0));
  model.min_weight = args.get("min-weight", random_range(0.5, 3.0));
  model.seed = args.get("seed", random_range(1.0, 10000.0));
  model.noise_scale = args.get("noise-scale", random_range(50.0, 250.0));
}

fn view(app: &App, model: &Model, frame: Frame) {
  let draw = app.draw();
  draw.background().color(IVORY);
  let win = app.window_rect();

  draw_paper_texture(&draw, &win, 10000);

  draw_lines(&draw, &win, model);

  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame_threaded(captured_frame_path(app, &frame));
  capture_model(app, &frame, model);
}

fn draw_lines(draw: &Draw, win: &Rect, model: &Model) {
  let noisefn = OpenSimplex::new();
  for i in 0..=model.n_lines {
    let color = hsl(0.0, 0.0, random_range(0.02, 0.1));
    let frac = i as f32 / model.n_lines as f32;
    let x_start = Interp::cos(win.left() * 0.75, win.right() * 0.75, frac);
    let divergence = Interp::cos(0.0, 1.0, (0.5 - frac).abs() * 2.0);
    let mut x = x_start;
    let mut y = random_range(win.top() - 75.0, win.top() - 25.0);
    let mut weight = random_range(model.min_weight, model.max_weight);

    let line_length = random_range(win.h() * 0.8, win.h() * 0.9) as usize;
    for _ in 0..line_length {
      let x_new = x
        + noisefn.get([
          x as f64 / model.noise_scale,
          y as f64 / model.noise_scale,
          model.seed,
        ]) as f32
          * divergence;
      let y_new = y - 1.0;
      draw
        .line()
        .start(pt2(x, y))
        .end(pt2(x_new, y_new))
        .color(color)
        .caps_round()
        .stroke_weight(weight);
      y = y_new;
      x = x_new;

      // randomly modulate weight of stroke, based on previous weight
      weight += if weight < model.min_weight {
        0.1
      } else if weight > model.max_weight {
        -0.1
      } else if chance() {
        0.1
      } else {
        -0.1
      };
    }
  }
}

fn chance() -> bool {
  random_f32() < 0.5
}

// experimenting with paper like textures

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
    n_lines: args.get("n-lines", 30),
    max_weight: args.get("max-weight", 4.0),
    min_weight: args.get("min-weight", 0.8),
    seed: args.get("seed", random_range(1.0, 10000.0)),
    noise_scale: args.get("noise-scale", random_range(50.0, 250.0)),
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  let args = ArgParser::new();
  model.n_lines = args.get("n-lines", random_range(15, 75));
  model.max_weight = args.get("max-weight", random_range(4.0, 6.0));
  model.min_weight = args.get("min-weight", random_range(0.5, 1.5));
  model.seed = args.get("seed", random_range(1.0, 10000.0));
  model.noise_scale = args.get("noise-scale", random_range(50.0, 250.0));
}

fn view(app: &App, model: &Model, frame: Frame) {
  let draw = app.draw();
  draw.background().color(FLORALWHITE);
  let win = app.window_rect();

  draw_paper_texture(&draw, &win, 5000, 0.05);

  draw_lines(&draw, &win, model);

  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame_threaded(captured_frame_path(app, &frame));
  capture_model(app, &frame, model);
}

fn draw_lines(draw: &Draw, win: &Rect, model: &Model) {
  let noisefn = OpenSimplex::new();
  let left = win.left() * 0.75;
  let right = win.right() * 0.75;
  for i in 0..=model.n_lines {
    let color = hsl(0.0, 0.0, random_range(0.02, 0.1));
    let frac = i as f32 / model.n_lines as f32;
    let x_start = Interp::lin(left, right, frac);
    let divergence = Interp::cos(0.0, 0.15, (0.5 - frac).abs() * 2.0);
    let mut x = x_start;
    let mut y = random_range(win.top() - 75.0, win.top() - 25.0);
    let mut weight = random_range(model.min_weight, model.max_weight);
    let line_length = random_range(win.h() * 0.8, win.h() * 0.9) as usize;

    // collect points and varying weights
    let weighted_points: Vec<(Point2, f32)> = (0..line_length)
      .map(|_| {
        // draw main line
        let x_new = x
          + noisefn.get([
            x as f64 / model.noise_scale,
            y as f64 / model.noise_scale,
            model.seed,
          ]) as f32
            * divergence;
        let y_new = y - 1.0;
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
        (pt2(x, y), weight)
      })
      .collect();

    // draw main vertical line
    for (i, (point, weight)) in weighted_points.iter().enumerate() {
      if i > line_length - 2 {
        continue;
      }
      let end_point = weighted_points[i + 1].0;
      draw
        .line()
        .start(*point)
        .end(end_point)
        .color(color)
        .caps_round()
        .stroke_weight(*weight);
    }

    // draw many small horizontal lines along the vertical line,
    // but skip the left most line
    if i > 0 {
      let n_horiz_lines = model.n_lines * 3;
      let horiz_line_length = (right - left) / model.n_lines as f32;
      for h in 0..n_horiz_lines {
        let index = map_range(h, 0, n_horiz_lines - 1, 0, line_length - 1);
        let (start, _) = weighted_points[index];
        let noise = noisefn.get([start.x as f64, start.y as f64, model.seed]) as f32;
        let start = pt2(start.x, start.y + noise * 50.0);
        let end = pt2(start.x - horiz_line_length, start.y);

        // only draw line if it's within bounds of the vertical line.
        // Comparisons feel backwards becaues line is drawn top (positive y) to bottom (negative y)
        if start.y < weighted_points[0].0.y && start.y > weighted_points.last().unwrap().0.y {
          draw
            .line()
            .start(start)
            .end(end)
            .color(color)
            .caps_round()
            .stroke_weight(1.0);
        }
      }
    }
  }
}

fn chance() -> bool {
  random_f32() < 0.5
}

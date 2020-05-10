extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::captured_frame_path;

fn main() {
  nannou::app(model).view(view).run();
}

struct Model {
  nx: i32,
  ny: i32,
  n_steps: i32,
  stroke_weight: f32,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  app.new_window().size(1024, 1024).build().unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));

  Model {
    nx: args.get("nx", 50),
    ny: args.get("ny", 50),
    n_steps: args.get("steps", 200),
    stroke_weight: args.get("weight", 1.0),
  }
}

fn field(_model: &Model, x: f32, y: f32) -> f32 {
  (x / 100.0).sin() * (y / 100.0).cos()
}

fn view(app: &App, model: &Model, frame: Frame) {
  let draw = app.draw();
  let win = app.window_rect();

  draw.background().color(WHITE);

  for i in 0..model.nx {
    let x_factor = i as f32 / model.nx as f32;
    for j in 0..model.ny {
      let y_factor = j as f32 / model.ny as f32;

      // slightly randomize starting position
      let mut x = win.x.lerp(x_factor) + random_range(-3.0, 3.0);
      let mut y = win.y.lerp(y_factor) + random_range(-3.0, 3.0);

      let color = hsla(x_factor.hypot(y_factor), 0.5, 0.38, 1.0);

      let points: Vec<(Point2, Hsla)> = (0..model.n_steps)
        .map(|_n| {
          let angle = field(model, x, y);
          x = x + angle.cos();
          y = y + angle.sin();
          (pt2(x, y), color)
        })
        .collect();

      draw
        .polyline()
        .caps_round()
        .weight(model.stroke_weight)
        .points_colored(points);
    }
  }

  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame_threaded(captured_frame_path(app, &frame));
}

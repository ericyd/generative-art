// A simple grid where the size and color of the box is determined by a Perlin noise field.
// cargo run --release --example drifting_boxes
extern crate chrono;
extern crate nannou;

use nannou::noise::{NoiseFn, Perlin};
use nannou::prelude::*;
use nannou::Draw;

mod util;
use util::args::ArgParser;
use util::formatted_frame_path;

fn main() {
  nannou::app(model).view(view).update(update).run();
}

struct Model {
  nx: i32,
  ny: i32,
  stroke_weight: f32,
  offset: f32,
  seed: f64,
  size_scale: f32,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  app.new_window().size(1024, 1024).build().unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));

  Model {
    nx: args.get("nx", 15),
    ny: args.get("ny", 15),
    stroke_weight: args.get("weight", 2.5),
    offset: args.get("offset", 0.0),
    seed: args.get("seed", random_range(1000.0, 100000.0)),
    size_scale: args.get("size", 1.1),
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  let args = ArgParser::new();
  model.seed = args.get("seed", random_range(1000.0, 100000.0));
}

fn view(app: &App, model: &Model, frame: Frame) {
  let draw = app.draw();
  let win = app.window_rect();
  draw.background().color(SNOW);

  draw_boxes(&draw, model, &win);

  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame_threaded(formatted_frame_path(
      app,
      &frame,
      format!("seed-{}", model.seed),
    ));
}

fn draw_boxes(draw: &Draw, model: &Model, win: &Rect) {
  let noisefn = Perlin::new();
  for i in 0..=model.nx {
    for j in 0..=model.ny {
      let x_offset = if model.offset == 0.0 {
        0.0
      } else {
        random_range(-model.offset, model.offset)
      };
      let y_offset = if model.offset == 0.0 {
        0.0
      } else {
        random_range(-model.offset, model.offset)
      };
      let x = map_range(i, 0, model.nx, win.left(), win.right()) + x_offset;
      let y = map_range(j, 0, model.ny, win.bottom(), win.top()) + y_offset;

      let noise = noisefn.get([x as f64 / 400.0, y as f64 / 400.0, model.seed]) as f32;
      let size = map_range(
        noise,
        -1.0,
        1.0,
        4.0,
        win.w() / model.nx as f32 * model.size_scale,
      );
      let hue = map_range(noise, -1.0, 1.0, 10.0, 230.0) / 360.0;

      draw
        .rect()
        .stroke_weight(model.stroke_weight)
        .stroke(hsl(hue, 0.4, 0.05))
        .color(hsl(hue, 0.4, 0.3))
        .x_y(x, y)
        .w_h(size, size);
    }
  }
}

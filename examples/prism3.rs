// prism
//   Drawing prisms in the most simple, brute-force way I could imagine!
//   Define a vertex, then the three dimensions for the prism,
//   Then, draw the quadrilateral polygons that would be seen as the top, left, and right
//   based on a set angle (which is related to the perspective, but extremely simplified)
//
// What's different in prism2?
//   The dimensions and position of the prisms are quantized to a grid
//   so that they naturally form more geometric shapes
//
//   Also, the Prism struct was abstracted to a utility module
//
// What's different in prism3?
//   Not sure yet.
//
// cargo run --release --example prism3
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::captured_frame_path;
use util::Prism;

fn main() {
  nannou::app(model).update(update).run();
}

struct Model {
  n_prisms: usize,
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
    n_prisms: args.get("n-prisms", 400),
  }
}

fn update(_app: &App, _model: &mut Model, _update: Update) {}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let win = app.window_rect();
  let draw = app.draw();
  draw.background().color(WHITE);

  // need to quantize on 3D grid?
  let angle = PI / 9.;
  let quantizer = 20.;
  let quantize = quantize_to(quantizer);

  let mut prisms = (0..model.n_prisms)
    .map(|_i| {
      let x = quantize(map_range(
        random_f32(),
        0.,
        1.,
        win.left() * 0.8,
        win.right() * 0.8,
      ));
      let y = quantize(map_range(
        random_f32(),
        0.,
        1.,
        win.bottom() * 0.8,
        win.top() * 0.8,
      ));

      let y_frac = map_range(y, win.bottom() * 0.8, win.top() * 0.8, 0.0, 1.0);
      let w = quantize(random_range(quantizer, 100. * y_frac + quantizer));
      let h = quantize(random_range(quantizer, 100. * y_frac + quantizer));
      let d = quantize(random_range(quantizer, 100. * y_frac + quantizer));

      let chance = random_f32() < 0.93;
      let w = if chance { 20. } else { 60. };
      let h = if chance { 20. } else { 60. };
      let d = if chance { 20. } else { 60. };

      let hue = map_range(y - x, -win.w() * 0.8, win.w() * 0.8, 0.0, 1.0);

      Prism::new(pt2(
        x + quantizer * angle.cos(),
        y + quantizer * angle.sin(),
      ))
      .w(w)
      .h(h)
      .d(d)
      .angle(angle)
      .top_color(hsla(hue, 0.5, 0.7, 1.0).into())
      .left_color(hsla(hue, 0.5, 0.45, 1.0).into())
      .right_color(hsla(hue, 0.5, 0.1, 1.0).into())
    })
    .collect::<Vec<Prism>>();

  // sort top to bottom, left to right
  prisms.sort_by(|a, b| a.vertex.y.partial_cmp(&b.vertex.y).unwrap());

  // draw
  for prism in prisms {
    prism.draw(&draw);
  }

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

// TODO: there's gotta be a better implementation...
fn quantize_to(quantizer: f32) -> impl Fn(f32) -> f32 {
  move |input| {
    let remainder = input % quantizer;
    match (quantizer / 2. < remainder.abs(), input > 0.) {
      (true, true) => input + (quantizer - remainder.abs()),
      (true, false) => input - (quantizer - remainder.abs()),
      (false, true) => input - remainder.abs(),
      (false, false) => input + remainder.abs(),
    }
  }
}

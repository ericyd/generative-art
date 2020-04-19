// cargo run --release --example harmonograph
//
// While "harmonograph_ish" example is __inspired__ by a harmonograph,
// this example is an actual harmonograph implementation.
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::prelude::*;

mod util;
use std::f64::consts::E;
use util::args::ArgParser;
use util::captured_frame_path;
use util::interp::lerp;

// https://en.wikipedia.org/wiki/Harmonograph
struct Pendulum {
  amplitude: f32,
  frequency: f32,
  phase: f32,
  damping: f32,
}

impl Pendulum {
  pub fn new(amplitude: f32, frequency: f32, phase: f32, damping: f32) -> Self {
    Pendulum {
      amplitude,
      frequency,
      phase,
      damping,
    }
  }

  pub fn eval(&self, t: f32) -> f32 {
    self.amplitude * (t * self.frequency + self.phase).sin() * (E as f32).powf(-self.damping * t)
  }
}

struct Harmonograph {
  // origin of harmonograph
  x: f32,
  y: f32,
  x_pendulums: Vec<Pendulum>,
  y_pendulums: Vec<Pendulum>,
  resolution: f32,
  length: f32,
  start: f32,
}

impl Harmonograph {
  pub fn new() -> Self {
    Harmonograph {
      x: 0.0,
      y: 0.0,
      x_pendulums: vec![],
      y_pendulums: vec![],
      resolution: 100.,
      length: 100.,
      start: PI / 12.,
    }
  }

  pub fn x_y(mut self, x: f32, y: f32) -> Self {
    self.x = x;
    self.y = y;
    self
  }

  pub fn x_pendulums(mut self, pendulums: Vec<Pendulum>) -> Self {
    self.x_pendulums = pendulums;
    self
  }

  pub fn y_pendulums(mut self, pendulums: Vec<Pendulum>) -> Self {
    self.y_pendulums = pendulums;
    self
  }

  pub fn resolution(mut self, resolution: f32) -> Self {
    self.resolution = resolution;
    self
  }

  pub fn length(mut self, length: f32) -> Self {
    self.length = length;
    self
  }

  pub fn start(mut self, start: f32) -> Self {
    self.start = start;
    self
  }

  pub fn points(&self) -> Vec<Point2> {
    let n_points = (self.length * self.resolution).floor() as i32;
    (0..n_points)
      .map(|n| {
        let t = self.start + n as f32 / self.resolution;
        let x = self
          .x_pendulums
          .iter()
          .fold(0.0, |sum, pendulum| sum + pendulum.eval(t))
          + self.x;
        let y = self
          .y_pendulums
          .iter()
          .fold(0.0, |sum, pendulum| sum + pendulum.eval(t))
          + self.y;
        pt2(x, y)
      })
      .collect()
  }
}

fn main() {
  nannou::app(model).view(view).size(1024, 1024).run();
}

struct Model {
  line_weight: f32,
  color_start: f32,
  color_end: f32,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  app.new_window().title("window a").build().unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));
  Model {
    line_weight: args.get("line-weight", 1.),
    color_start: args.get("color-start", 0.),
    color_end: args.get("color-end", 1.),
  }
}

// the only thing this should do is draw the model.grid (or maybe model.next)
fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();

  // Clear the background to black.
  draw.background().color(hsl(242. / 360., 0.47, 0.02));

  // Harmonograph 1
  let harmonograph_1 = Harmonograph::new()
    .x_pendulums(vec![Pendulum::new(532.2, 48.06, 2.22, 0.03)])
    .y_pendulums(vec![
      Pendulum::new(524.2, 48.06, 0.85, 0.03),
      Pendulum::new(452.4, 8.07, 0.85, 0.03),
    ])
    .resolution(2200.)
    .length(26. * PI)
    .start(PI * 2.);

  let points = harmonograph_1.points();
  let n_points = points.len();

  let colored_points = points.iter().enumerate().map(|(i, pt)| {
    let factor = i as f32 / n_points as f32;
    let hue = lerp(model.color_start, model.color_end, factor);
    let lum = lerp(0.5, 0.7, factor);
    (pt.clone(), hsla(hue, 0.5, lum, 0.25))
  });

  colored_points.for_each(|(pt, color)| {
    draw
      .ellipse()
      .x_y(pt.x, pt.y)
      .w_h(0.15, 0.15)
      .color(color)
      .radius(model.line_weight);
  });

  // Harmonograph 2
  let harmonograph_2 = Harmonograph::new()
    .x_pendulums(vec![
      Pendulum::new(318.9, 8.01, 3.19, 0.06),
      Pendulum::new(255.1, 64.13, 2.22, 0.03),
    ])
    .y_pendulums(vec![
      Pendulum::new(337.5, 64.13, 0.85, 0.03),
      Pendulum::new(252.4, 8.07, 0.85, 0.03),
    ])
    .resolution(1000.)
    .length(5. * PI)
    .start(PI * 4.)
    .x_y(-500., 500.);

  let points = harmonograph_2.points();
  let n_points = points.len();

  let colored_points = points.iter().enumerate().map(|(i, pt)| {
    let factor = i as f32 / n_points as f32;
    let hue = lerp(model.color_start + 0.4, model.color_end + 0.4, factor);
    let lum = lerp(0.4, 0.6, factor);
    (pt.clone(), hsla(hue, 0.5, lum, 0.7))
  });

  // do not filter points that are offscreen because it can cause unexpected line artifacts
  // draw
  //   .polyline()
  //   .start_cap_round()
  //   .weight(model.line_weight)
  //   .colored_points(colored_points);

  // Harmonograph 3
  let harmonograph_3 = Harmonograph::new()
    .x_pendulums(vec![
      Pendulum::new(318.9, 8.01, 3.19, 0.06),
      Pendulum::new(255.1, 64.13, 2.22, 0.03),
    ])
    .y_pendulums(vec![
      Pendulum::new(337.5, 64.13, 0.85, 0.03),
      Pendulum::new(252.4, 8.07, 0.85, 0.03),
    ])
    .resolution(1000.)
    .length(5. * PI)
    .start(PI * 4.)
    .x_y(500., -500.);

  let points = harmonograph_3.points();
  let n_points = points.len();

  let colored_points = points.iter().enumerate().map(|(i, pt)| {
    let factor = i as f32 / n_points as f32;
    let hue = lerp(model.color_start + 0.4, model.color_end + 0.4, factor);
    let lum = lerp(0.4, 0.6, factor);
    (pt.clone(), hsla(hue, 0.5, lum, 0.7))
  });

  // do not filter points that are offscreen because it can cause unexpected line artifacts
  // draw
  //   .polyline()
  //   .start_cap_round()
  //   .weight(model.line_weight)
  //   .colored_points(colored_points);

  // Write to the window frame.
  draw.to_frame(app, &frame).unwrap();

  let file_path = captured_frame_path(app, &frame);
  app.main_window().capture_frame_threaded(file_path);
}

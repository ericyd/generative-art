// cargo run --release --example spirograph_1

extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::noise::{NoiseFn, Perlin};
use nannou::prelude::*;

mod util;
use util::captured_frame_path;
use util::interp::{Interp, Interpolate};

fn main() {
  nannou::sketch(view).size(1024, 1024).run();
}

fn view(app: &App, frame: Frame) {
  let win = app.window_rect();
  let pi2 = PI * 2.0;
  app.set_loop_mode(LoopMode::NTimes {
    // two frames are necessary for capture_frame to work properly
    number_of_updates: 1,
  });

  // Prepare to draw.
  let draw = app.draw();

  // Clear the background to black.
  draw.background().color(BLACK);

  // and this: http://www.jonathan.lansey.net/pastimes/pendulum/index.html
  let primary_radius = 300.;
  let secondary_radius = 100.;
  let cusps = 0.9;
  let resolution = 40.;
  let line_weight = 1.0;
  let line_length = 1000;

  // https://en.wikipedia.org/wiki/Epicycloid
  let points = (0..=(360 * line_length)).map(|i| {
    let theta: f32 = deg_to_rad(i as f32) / resolution;
    let x = secondary_radius * (cusps + 1.) * theta.cos()
      - secondary_radius * 1.2 * ((cusps + 1.) * theta).cos();
    let y = secondary_radius * (cusps + 1.) * theta.sin()
      - secondary_radius * 1.2 * ((cusps + 1.) * theta).sin();

    // let x = (primary_radius - secondary_radius) * theta.cos()
    //   + secondary_radius
    //     * (((primary_radius - secondary_radius) / secondary_radius) * theta).cos();
    // let y = (primary_radius - secondary_radius) * theta.sin()
    //   - secondary_radius
    //     * (((primary_radius - secondary_radius) / secondary_radius) * theta).sin();

    // // https://en.wikipedia.org/wiki/Hypocycloid
    // let x = (primary_radius - secondary_radius) * theta.cos()
    //   + secondary_radius
    //     * (((primary_radius - secondary_radius) / secondary_radius) * theta).cos();
    // let y = (primary_radius - secondary_radius) * theta.sin()
    //   - secondary_radius
    //     * (((primary_radius - secondary_radius) / secondary_radius) * theta).sin();
    let point = pt2(x, y);
    let color = hsl(i as f32 / 360. / line_length as f32 / resolution, 0.5, 0.5);
    (point, color)
  });
  draw
    .polyline()
    .start_cap_round()
    .weight(line_weight)
    .points_colored(points);

  // Write to the window frame.
  draw.to_frame(app, &frame).unwrap();

  // let file_path = captured_frame_path(app, &frame);
  // app.main_window().capture_frame_threaded(file_path);
}

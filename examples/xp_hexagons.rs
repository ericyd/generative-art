extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::prelude::*;

mod util;
use util::hexagon::*;
use util::interp::{Interp, Interpolate};

fn main() {
  nannou::sketch(view).size(1024, 768).run();
}

fn view(app: &App, frame: Frame) {
  app.set_loop_mode(LoopMode::NTimes {
    // two frames are necessary for capture_frame to work properly
    number_of_updates: 1,
  });

  let draw = app.draw();
  draw.background().color(BLACK);

  honeycomb_hex(40.0, 6, 1.01, false).iter().for_each(|hex| {
    let hue = Interp::lin(0.02, 0.09, random_f32());
    let color = hsla(hue, 0.75, 0.5, 1.0);
    draw
      .polygon()
      .stroke(color)
      .stroke_weight(0.1)
      .points(hex.points())
      .color(color);
  });

  // Write to the window frame.
  draw.to_frame(app, &frame).unwrap();
}

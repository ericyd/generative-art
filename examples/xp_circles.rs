extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::prelude::*;

mod util;
use util::circle::*;
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

  honeycomb_circles(20.0, 6, 1.0, false)
    .iter()
    .for_each(|circle| {
      let hue = Interp::lin(0.95, 1.05, random_f32());
      if random_f32() < 0.35 {
        // skip drawing - create some holes
        draw
          .ellipse()
          .x_y(circle.x, circle.y)
          .radius(circle.radius)
          .hsla(hue, 1.0, 0.5, 1.0);
      } else if random_f32() < 0.2 {
        // attributes.filter = "url('#glow')"
        draw
          .ellipse()
          .x_y(circle.x, circle.y)
          .radius(circle.radius)
          .hsla(hue, 1.0, 0.5, 1.0);
      } else {
        // attributes.filter = "url('#blur')"
        // attributes.opacity = randomFloat(0.5, 1)

        draw
          .ellipse()
          .x_y(circle.x, circle.y)
          .radius(circle.radius)
          .hsla(hue, 0.9, 0.63, 0.77);
      }
    });

  // Write to the window frame.
  draw.to_frame(app, &frame).unwrap();
}

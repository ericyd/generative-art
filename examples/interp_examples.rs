// Cargo.toml
// ===============
// [package]
// name = "flow-field-1"
// version = "0.1.0"
// authors = ["ericyd <eric@ericyd.com>"]
//
// [dependencies]
// nannou = "0.13"
//
//
// rustfmt.toml
// ===============
// tab_spaces = 2
//
//
// Notes
// ===============
// - ALWAYS run with `cargo run --release`
// - huge thanks to this resource: https://tylerxhobbs.com/essays/2020/flow-fields?format=amp
// - some references
//
//
// This is really neat, but it blows up around the center.
// which, in some ways sounds expected since radius gets very small.
// perhaps... needs to scale differently based on position ?

extern crate nannou;

use nannou::prelude::*;

mod util;

use util::interp::{Interp, Interpolate};

fn main() {
  nannou::sketch(view).run();
}

fn view(app: &App, frame: Frame) {
  app.set_loop_mode(LoopMode::NTimes {
    number_of_updates: 1,
  });

  // Prepare to draw.
  let draw = app.draw();

  // Clear the background to black.
  draw.background().color(BLACK);

  let num_circles = 40;
  for i in 0..num_circles {
    let factor = i as f64 / (num_circles as f64 - 1.0) as f64;
    // let start_radius = lerp(10.0, 800.0, radius_factor);
    let x_lin = Interp::lin(-400.0, 400.0, factor);
    let x_exp = Interp::exp(-400.0, 400.0, factor);
    let x_sin = Interp::sin(-400.0, 400.0, factor);
    let x_sin3 = Interp::sin_3(-400.0, 400.0, factor);
    let x_cos = Interp::cos(-400.0, 400.0, factor);
    let x_inv = Interp::inv(-400.0, 400.0, factor);
    let x_log = Interp::log(-400.0, 400.0, factor);
    let x_euler = Interp::euler(-400.0, 400.0, factor);
    let x_reverse_exp = Interp::reverse_exp(-400.0, 400.0, factor);
    let x_sin4 = Interp::sin_4(-400.0, 400.0, factor);

    println!("-------
x_lin: {}
x_exp: {}
x_sin: {}
x_sin3: {}
x_cos: {}
x_inv: {}
x_log: {}
x_euler: {}
x_reverse_exp: {}", x_lin, x_exp, x_sin, x_sin3, x_cos, x_inv, x_log, x_euler, x_reverse_exp);

    draw.line().start(pt2(-400.0, -400.0)).end(pt2(-400.0, 400.0)).weight(1.0).hsla(255.0, 1.0, 1.0, 1.0);
    draw.line().start(pt2(400.0, -400.0)).end(pt2(400.0, 400.0)).weight(1.0).hsla(255.0, 1.0, 1.0, 1.0);

    let ys = [-200.0, -150.0, -100.0, -50.0, 0.0, 50.0, 100.0];
    draw.ellipse().x_y(x_lin as f32, ys[0]).radius(5.0).hsla(100.0, 0.5, 0.5, 1.0);
    draw.ellipse().x_y(x_exp as f32, ys[1]).radius(5.0).hsla(100.0, 0.5, 0.5, 1.0);
    draw.ellipse().x_y(x_sin as f32, ys[2]).radius(5.0).hsla(100.0, 0.5, 0.5, 1.0);
    draw.ellipse().x_y(x_sin3 as f32, ys[3]).radius(5.0).hsla(100.0, 0.5, 0.5, 1.0);
    draw.ellipse().x_y(x_cos as f32, ys[4]).radius(5.0).hsla(100.0, 0.5, 0.5, 1.0);
    draw.ellipse().x_y(x_reverse_exp as f32, ys[5]).radius(5.0).hsla(100.0, 0.5, 0.5, 1.0);
    draw.ellipse().x_y(x_sin4 as f32, ys[6]).radius(5.0).hsla(100.0, 0.5, 0.5, 1.0);

    // these are outside the bounds of the mix/max for interpolation,
    // or do not meet the min/max of the bounds, therefore they are not very useful to us.
    // draw.ellipse().x_y(x_inv as f32, 66.0).radius(5.0).hsla(100.0, 0.5, 0.5, 1.0);
    // draw.ellipse().x_y(x_log as f32, 100.0).radius(5.0).hsla(100.0, 0.5, 0.5, 1.0);
    // draw.ellipse().x_y(x_euler as f32, 133.0).radius(5.0).hsla(100.0, 0.5, 0.5, 1.0);
  }

  // Write to the window frame.
  draw.to_frame(app, &frame).unwrap();
}

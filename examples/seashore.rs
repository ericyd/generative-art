// cargo run --release --example seashore
//
// I'd be curious to see how much code I could eliminate if
// I was more clever with drawing my blobs.
// I have recently found the artistic side of me bumping into
// the part of me that wants to write super clean software all the time.
// I think it's really nice to remember that software doesn't have to be
// clean to be elegant, or to achieve a goal.
//
// In the future, I'd like to expand on this technique and use it to create more textures.
// Ideally, I would also like to find a way that doesn't require ~20k blobs,
// because it maxes my memory if I try to loop it
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::noise::{NoiseFn, Perlin};
use nannou::prelude::*;
use nannou::Draw;

mod util;
use util::blob::Blob;
use util::interp::{lerp, nextf, Interp, Interpolate};

fn main() {
  nannou::sketch(view).size(1024, 1024).run();
}

fn view(app: &App, frame: Frame) {
  let win = app.window_rect();
  app.set_loop_mode(LoopMode::loop_ntimes(1));

  // Prepare to draw.
  let draw = app.draw();

  // set background color
  let bg = hsla(36. / 360., 0.59, 0.90, 1.0);
  draw.background().color(bg);

  yellow_blobs(&win, &draw);
  blue_blobs(&win, &draw);
  red_blobs(&win, &draw);
  circular_frame(&win, &draw);

  // Write to the window frame.
  draw.to_frame(app, &frame).unwrap();

  // save image
  app
    .main_window()
    .capture_frame_threaded(util::captured_frame_path(app, &frame));
}

// calculates the y boundary between yellow and blue
fn boundary(x: f32) -> f32 {
  let perlin = Perlin::new();
  let seed = 15.; // 10

  let y_base = (x / 600.).sin();
  let noise = perlin.get([x as f64 / 400., y_base as f64, seed]) as f32 * 1.8;
  y_base * noise * 200.
}

fn blue_blobs(win: &Rect, draw: &Draw) {
  // dark blue
  for _i in 0..5000 {
    let hue = nextf(218., 242.) / 360.;
    let sat = nextf(0.2, 0.65);
    let alpha = nextf(0.02, 0.05);
    let width = nextf(10.0, 50.0);
    let height = nextf(10.0, 50.0);
    let x = nextf(win.x.start, win.x.end);
    let y = Interp::exp(win.y.start - 10., boundary(x) - 140., random_f32());
    let lightness = nextf(0.15, 0.35);
    Blob::new()
      .x_y(x, y)
      .width(width)
      .height(height)
      .rotate_rad(nextf(0., PI * 2.))
      .noise_scale(nextf(0.4, 0.95))
      .hsla(hue, sat, lightness, alpha)
      .fuzziness(nextf(0., 0.8))
      .draw(draw);
  }

  // light blue
  for _i in 0..3300 {
    let hue = nextf(190., 216.) / 360.;
    let sat = nextf(0.3, 0.7);
    let alpha = nextf(0.02, 0.05);
    let width = nextf(5.0, 50.0);
    let height = nextf(5.0, 50.0);
    let x = nextf(win.x.start, win.x.end);
    let y = nextf(win.y.start + 170., boundary(x) + 25.);
    let lightness = nextf(0.35, 0.58);
    // let lightness = lerp(0.035, 0.59, (y / win.y.magnitude() * 2.7));
    Blob::new()
      .x_y(x, y)
      .width(width)
      .height(height)
      .rotate_rad(nextf(0., PI * 2.))
      .noise_scale(nextf(0.4, 0.95))
      .hsla(hue, sat, lightness, alpha)
      .fuzziness(nextf(0., 0.8))
      .draw(draw);
  }

  // nearly white
  for _i in 0..1800 {
    let hue = nextf(166., 200.) / 360.;
    let sat = nextf(0.6, 0.99);
    let lightness = nextf(0.75, 0.93);
    let alpha = nextf(0.02, 0.05);
    let width = nextf(5.0, 20.0);
    let height = nextf(5.0, 30.0);
    let x = nextf(win.x.start, win.x.end);
    let y = Interp::exp(boundary(x) + 25., win.y.start + 200., random_f32());
    Blob::new()
      .x_y(x, y)
      .width(width)
      .height(height)
      .rotate_rad(nextf(0., PI * 2.))
      .noise_scale(nextf(0.4, 0.95))
      .hsla(hue, sat, lightness, alpha)
      .fuzziness(nextf(0., 0.8))
      .draw(draw);
  }
}

// draw a bunch of yellow blobs in the top left corner
fn yellow_blobs(win: &Rect, draw: &Draw) {
  for _i in 0..30000 {
    let hue = nextf(25., 53.) / 360.;
    let sat = nextf(0.3, 0.7);
    let alpha = nextf(0.08, 0.12);
    let width = nextf(7.0, 15.0);
    let height = nextf(7.0, 15.0);
    let x = nextf(win.x.start, win.x.end);
    let y = nextf(win.y.end + 10., boundary(x) - 25.);
    let lightness = lerp(0.235, 0.88, y / win.y.magnitude() * 1.75);
    Blob::new()
      .x_y(x, y)
      .width(width)
      .height(height)
      .rotate_rad(nextf(0., PI * 2.))
      .noise_scale(nextf(0.64, 0.8))
      .hsla(hue, sat, lightness, alpha)
      .fuzziness(nextf(0., 5.))
      .draw(&draw);
  }
}

// draw a bunch of red blobs in wavy pattern
fn red_blobs(win: &Rect, draw: &Draw) {
  for _i in 0..3000 {
    let hue = nextf(350., 399.) / 360.;
    let sat = nextf(0.4, 0.8);
    let lightness = nextf(0.25, 0.48);
    let alpha = nextf(0.04, 0.07);
    let height = nextf(2.0, 5.0);
    let width = nextf(height, height * 2.5);
    let x = Interp::reverse_exp(50., win.x.end + 10., random_f32());
    let y = (x / 75.).sin() * 50. + 175.;
    let y_spread = lerp(0.01, 0.5, x / win.x.end);
    let y = nextf(y * (1. - y_spread), y * (1. + y_spread));
    Blob::new()
      .x_y(x, y)
      .width(width)
      .height(height)
      .rotate_rad(nextf(0., PI * 2.))
      .noise_scale(nextf(0.4, 0.8))
      .hsla(hue, sat, lightness, alpha)
      .fuzziness(nextf(2., 4.))
      .draw(&draw);
  }

  for _i in 0..3000 {
    let hue = nextf(4., 40.) / 360.;
    let sat = nextf(0.4, 0.6);
    let lightness = nextf(0.45, 0.58);
    let alpha = nextf(0.04, 0.07);
    let height = nextf(2.0, 5.0);
    let width = nextf(height, height * 2.5);
    let x = lerp(win.x.start - 10., win.x.middle() - 50., random_f32());
    let y = (x / 95.).sin() * 40. + 375.;
    let y_spread = lerp(0.01, 0.5, x / win.x.end);
    let y = nextf(y * (1. - y_spread), y * (1. + y_spread));
    Blob::new()
      .x_y(x, y)
      .width(width)
      .height(height)
      .rotate_rad(nextf(0., PI * 2.))
      .noise_scale(nextf(0.4, 0.8))
      .hsla(hue, sat, lightness, alpha)
      .fuzziness(nextf(2., 4.))
      .draw(&draw);
  }
}

fn circular_frame(win: &Rect, draw: &Draw) {
  let stroke_weight = 300.;
  draw
    .ellipse()
    .x_y(win.x.middle(), win.y.middle())
    // add the stroke width to the w/h so inner stroke is the width of the frame
    .w(win.x.magnitude() + stroke_weight)
    .h(win.y.magnitude() + stroke_weight)
    .color(hsla(0., 0., 0., 0.))
    .stroke(hsla(50. / 360., 1., 0.97, 1.))
    .stroke_weight(stroke_weight)
    .resolution(720); // higher resolution is better for such a large ellipse
}

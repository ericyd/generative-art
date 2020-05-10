// The challenge here was appropriately mapping 3D points to a 2D canvas.
// It's a very simple drawing, but I wanted to explore this functionality of nannou
// to open more possibilities.
//
// Algorithm in a nutshell
// 1. start with n_lines evenly spaces points along to the top of the x axis.
// 2. for each point, add a small length to the line.
//    The z position of the line is determined by a simple cosine curve.
//    The x and y positions are slightly distorted based on the z value,
//    to simulate a cloth being folded.
// 3. Once the 3D lines are drawn, transform them to 2D and draw them
// 4. Repeat with horizontally-oriented lines
//
// cargo run --release --example cloth
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::math::Matrix4;
use nannou::noise::{NoiseFn, Perlin};
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::captured_frame_path;
use util::interp::*;

fn main() {
  nannou::app(model).run();
}

struct Model {
  // borders of cloth
  top: f32,
  right: f32,
  bottom: f32,
  left: f32,
  // how many lines to draw
  n_lines: i32,
  // resolution of the lines
  n_steps: i32,
  // noise seed
  seed: f64,
  // controls "frequency" of noise "waves"
  noise_scale: f64,
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
  let win = app.window_rect();

  Model {
    top: args.get("top", win.top()),
    right: args.get("right", win.right() - 100.0),
    bottom: args.get("bottom", win.bottom()),
    left: args.get("left", win.left() + 100.0),
    n_lines: args.get("lines", 200),
    n_steps: args.get("steps", 300),
    seed: args.get("seed", 15.0),
    noise_scale: args.get("noise_scale", 350.0),
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  draw.background().color(FLORALWHITE);

  draw_lines(&draw, model);

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

fn draw_lines(draw: &Draw, model: &Model) {
  // `from_angle_N` is from an angle around that axis.
  // which means to adjust our "z" perspective, we actually
  // need to spin around the x axis
  //
  // This allows us to project a 3D point onto a 2D drawing surface.
  // references:
  //   https://stackoverflow.com/questions/724219/how-to-convert-a-3d-point-into-2d-perspective-projection
  //   http://www.songho.ca/math/homogeneous/homogeneous.html
  //   https://docs.rs/nannou/0.13.0/nannou/math/struct.Matrix4.html
  //   https://docs.rs/nannou/0.13.0/nannou/prelude/trait.Transform.html
  let transformation_matrix = Matrix4::from_angle_x(nannou::math::Rad(PI / 6.0));
  let ripple_origin = pt2(model.right + 150.0, model.top - 200.0);

  // Draw all the vertical lines
  for i in 0..=model.n_lines {
    let x_frac = i as f32 / model.n_lines as f32;
    let x = lerp(model.left, model.right, x_frac);
    let points = (0..=model.n_steps)
      .map(|n| {
        let y_frac = n as f32 / model.n_steps as f32;
        let y = lerp(model.bottom, model.top, y_frac);
        next_point(x, y, ripple_origin, 0.80, model.seed, model.noise_scale)
      })
      .map(|(p, color)| (transformation_matrix.transform_point(p), color))
      .map(|(p, color)| (pt2(p.x, p.y), color));

    draw.polyline().points_colored(points);
  }

  // Draw all the horizontal lines
  // There's a lot of duplicate code here that could be abstracted, but I'm too tired
  for i in 0..=model.n_lines {
    let y_frac = i as f32 / model.n_lines as f32;
    let y = lerp(model.bottom, model.top, y_frac);
    let points = (0..=model.n_steps)
      .map(|n| {
        let x_frac = n as f32 / model.n_steps as f32;
        let x = lerp(model.left, model.right, x_frac);
        next_point(x, y, ripple_origin, 0.65, model.seed, model.noise_scale)
      })
      .map(|(p, color)| (transformation_matrix.transform_point(p), color))
      .map(|(p, color)| (pt2(p.x, p.y), color));

    draw.polyline().points_colored(points);
  }
}

fn next_point(
  x: f32,
  y: f32,
  ripple_origin: Point2,
  hue: f32,
  seed: f64,
  noise_scale: f64,
) -> (nannou::math::cgmath::Point3<f32>, Hsla) {
  let perlin = Perlin::new();
  let horiz_scale = 20.0;
  let hypot = ((x - ripple_origin.x) / horiz_scale).hypot((y - ripple_origin.y) / horiz_scale);
  let z_scale = horiz_scale * 4.0;

  // this is pretty boring, let's spice it up
  // let z = (hypot.cos() + 1.0) * z_scale;
  let z = (hypot.cos() + 1.0) / 2.0;
  // huh... really interesting effects using weird params to this noise fn
  let z = z
    * perlin.get([
      z as f64 / noise_scale,
      x as f64 / noise_scale,
      x as f64 / noise_scale,
      seed,
    ]) as f32;
  let z = z * z_scale;

  // trying to make the x,y shift with the z offset, as if it were a cloth being distorted in 3D space.
  // I think there are likely better ways to do this, because this isn't a perfect effect, but it's OK for now.
  let orientation = ((y - ripple_origin.y) / (x - ripple_origin.x)).atan().abs();
  let x = x + orientation.cos() * z;
  let y = y + orientation.sin() * z;
  (
    nannou::math::cgmath::Point3::new(x, y, z),
    hsla(
      hue + hypot / 500.0,
      0.4,
      map_range(z / z_scale, 0.0, 2.0, 0.3, 0.6),
      0.8,
    ),
  )
}

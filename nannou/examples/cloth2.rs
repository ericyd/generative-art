// Creating a topography-inspired surface from a noise field.
// This doesn't really resemble a cloth anymore, but the core
// algorithm is nearly identical to cloth.rs so I wanted to group them.
//
// Algorithm in a nutshell
// 1. start with n_lines evenly spaces points along to the left side of y axis (top to bottom)
// 2. for each point, add a small length to the line.
//    The z position of the line is determined by a noise field.
//    The x and y positions are slightly distorted based on the z value,
//    to simulate a cloth being folded.
// 3. Once the 3D lines are drawn, transform them to 2D points
// 4. Draw a polygon with same color as the background, that extends down beyond the line.
//    The top boundary of the polygon matches the topography line, and the lower bound
//    is just a simple closed polygon. This effectively "hides" any overlaps in the lines
//    which looks a lot cleaner and lends itself to the "closed surface" look.
// 5. Draw the actual line in a highlight color based on the topography.
//
// cargo run --release --example cloth2 -- --seed 68709
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::math::Matrix4;
use nannou::noise::{NoiseFn, Perlin, Turbulence};
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::{formatted_frame_path, Line2};

const BG_COLOR: Rgb<u8> = DARKSLATEGRAY;

fn main() {
  nannou::app(model).update(update).run();
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
  // maximum "height" of points
  z_scale: f32,
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
    top: args.get("top", win.top() + 200.0),
    right: args.get("right", win.right() - 100.0),
    bottom: args.get("bottom", win.bottom() + 100.0),
    left: args.get("left", win.left() + 100.0),
    n_lines: args.get("lines", 215),
    n_steps: args.get("steps", 300),
    seed: args.get("seed", random_range(1.0, 100000.0)),
    noise_scale: args.get("noise_scale", 600.0),
    z_scale: args.get("z-scale", 350.0),
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  let args = ArgParser::new();
  model.seed = args.get("seed", random_range(1.0, 100000.0));
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  draw.background().color(BG_COLOR);

  draw_lines(&draw, model);

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app.main_window().capture_frame(formatted_frame_path(
    app,
    &frame,
    format!("seed-{}", model.seed),
  ));
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
  let transformation_matrix = Matrix4::from_angle_x(nannou::math::Rad(PI / 4.0));
  let ripple_origin = pt2(model.right + 150.0, model.top - 200.0);

  // draw each line according to topographical definition
  for i in 0..=model.n_lines {
    let y = map_range(i, 0, model.n_lines, model.top, model.bottom);
    let points = (0..=model.n_steps)
      .map(|n| {
        let x = map_range(n, 0, model.n_steps, model.left, model.right);
        next_point(
          x,
          y,
          ripple_origin,
          model.seed,
          model.noise_scale,
          model.z_scale,
        )
      })
      .map(|(p, color)| (transformation_matrix.transform_point(p), color))
      .map(|(p, color)| (pt2(p.x, p.y), color));

    // build polygon that matches boundary, but extends down 200 points
    // to hide overlaps and create a solid appearance
    let polygon: Line2 = points.clone().map(|(pt, _color)| pt.clone()).collect();
    // probably a better way to do this;
    // this is just a hacky concatenation of first and last points that extend downward
    let first = polygon.first().unwrap();
    let last = polygon.last().unwrap();
    let polygon = [
      &[pt2(first.x, first.y - 200.0)],
      &polygon[..],
      &[pt2(last.x, last.y - 200.0)],
    ]
    .concat();
    draw
      .polygon()
      .color(BG_COLOR)
      .stroke_weight(0.0)
      .points(polygon);
    draw.polyline().points_colored(points);
  }
}

fn next_point(
  x: f32,
  y: f32,
  ripple_origin: Point2,
  seed: f64,
  noise_scale: f64,
  z_scale: f32,
) -> (nannou::math::cgmath::Point3<f32>, Hsla) {
  let source = Perlin::new();
  let noisefn = Turbulence::new(&source);

  let noise = noisefn.get([
    x as f64 / noise_scale,
    y as f64 / noise_scale,
    // z as f64 / noise_scale,
    seed,
  ]) as f32;
  let z = map_range(noise, -1.0, 1.0, 0.0, z_scale);

  // trying to make the x,y shift with the z offset, as if it were a cloth being distorted in 3D space.
  // I think there are likely better ways to do this, because this isn't a perfect effect, but it's OK for now.
  let orientation = ((y - ripple_origin.y) / (x - ripple_origin.x)).atan().abs();
  let x = x + orientation.cos() * z / 10.0;
  let y = y + orientation.sin() * z / 10.0;
  (
    nannou::math::cgmath::Point3::new(x, y, z),
    hsla(
      218.0 / 360.0,
      0.48,
      map_range(z, 0.0, 100.0, 0.45, 0.60),
      0.8,
    ),
  )
}

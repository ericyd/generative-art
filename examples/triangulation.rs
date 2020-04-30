// Well...
// wow.
// I am not good at Delauney.
// This took so much longer than I expected.
// So much for a simple drawing.
// I guess the lesson here is, don't draw while your brain is tired.
//
// Algorithm in a nutshell
// 1. Draw a bunch of concentric blobs
// 2. Flatmap over the blobs to create a point cloud
// 3. Triangulate the points with handy-dandy delaunator lib
// 4. Draw the trianges defined by delaunator
//
// cargo run --release --example triangulation
extern crate chrono;
extern crate delaunator;
extern crate nannou;

use nannou::color::*;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::blob::*;
use util::captured_frame_path;
use util::interp::{Interp, Interpolate};

use delaunator::{triangulate, Point};

fn main() {
  nannou::app(model).view(view).run();
}

struct Model {
  x_origin: f32,
  y_origin: f32,
  radius: f32,
  n_circles: i32,
  seed: f32,
  resolution: i32,
  noise_scale: f32,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  app.new_window().size(1024, 1024).build().unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));

  Model {
    x_origin: args.get("x-origin", -40.),
    y_origin: args.get("y-origin", 0.),
    radius: args.get("radius", 90.0),
    n_circles: args.get("circles", 30),
    seed: args.get("seed", 11.),
    resolution: args.get("resolution", 40),
    noise_scale: args.get("noise", 1.2),
  }
}

// the only thing this should do is draw the model.grid (or maybe model.next)
fn view(app: &App, model: &Model, frame: Frame) {
  let draw = app.draw();
  draw.background().color(WHITESMOKE);

  let points: Vec<Point> = (0..=model.n_circles)
    .flat_map(|n| {
      let frac = n as f32 / model.n_circles as f32;
      let noise = random_range(model.noise_scale * 0.98, model.noise_scale * 1.02);
      // need to look at nannou built-ins, I'm sure there are better options than this
      let radius = Interp::exp(model.radius / 10.0, model.radius, frac);

      let inner_points = Blob::new()
        .x_y(model.x_origin, model.y_origin)
        .radius(radius)
        .noise_scale(noise * (frac + 1.0))
        .seed(model.seed)
        .fuzziness(2.0)
        .rotate_rad(frac * PI)
        .resolution(model.resolution)
        .points();

      // removing the last point from here eliminates any duplicates that may be created from Blob::points()
      inner_points[0..inner_points.len() - 1].to_vec()
    })
    .map(|p| Point {
      x: p.x as f64,
      y: p.y as f64,
    })
    .collect();

  let delauney = triangulate(&points).expect("No triangulation exists.");

  // I'd be willing to bet there's a better way to iterate through the triangulation
  for (index, _) in delauney.triangles.iter().enumerate().step_by(3) {
    let tri_points = (index..=index + 2)
      .map(|i| delauney.triangles[i])
      .map(|p| points[p].clone())
      .map(|p| pt2(p.x as f32, p.y as f32));
    let frac = index as f32 / delauney.triangles.len() as f32;
    let hue = 0.6 + frac / 2.0;
    let lum = 0.6 + frac / 10.0;
    draw
      .polygon()
      .stroke(hsl(hue, 0.5, lum - 0.2))
      .color(hsl(hue, 0.5, lum))
      .points(tri_points);
  }

  // Write to the window frame and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame_threaded(captured_frame_path(app, &frame));
}

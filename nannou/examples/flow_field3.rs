// cargo run --release --example flow_field3 -- --nx 100 --ny 100 --padding 1.5 --weight 1.5 --overlap false --noise 165 --seed 8489
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::noise::{NoiseFn, Turbulence, Worley};
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::{formatted_frame_path, Line2};

fn main() {
  nannou::app(model).view(view).run();
}

struct Model {
  nx: i32,
  ny: i32,
  n_steps: i32,
  stroke_weight: f32,
  noise_scale: f64,
  seed: f64,
  // when false, overlaps are avoided in final result
  overlap: bool,
  // if overlap == false, how much space should exist between points
  padding: f32,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  app.new_window().size(1024, 1024).build().unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));

  Model {
    nx: args.get("nx", 50),
    ny: args.get("ny", 50),
    n_steps: args.get("steps", 200),
    stroke_weight: args.get("weight", 1.0),
    noise_scale: args.get("noise", 100.0),
    seed: args.get("seed", random_range(0.1, 10000.0)),
    overlap: args.get("overlap", true),
    padding: args.get("padding", 5.0),
  }
}

fn field<T: NoiseFn<[f64; 3]>>(model: &Model, x: f32, y: f32, noisefn: &T) -> f32 {
  let noise = noisefn.get([
    x as f64 / model.noise_scale,
    y as f64 / model.noise_scale,
    model.seed,
  ]);
  map_range(noise, -1.0, 1.0, 0.0, 2.0 * PI)
}

fn view(app: &App, model: &Model, frame: Frame) {
  let draw = app.draw();
  let win = app.window_rect();
  draw.background().color(SNOW);

  // Holds a record of all points, and allows avoiding overlap if model.overlap == false
  let mut existing_points: Vec<Point2> = Vec::new();
  // really interesting to play with different noise functions!
  let source = Worley::new();
  let noisefn = Turbulence::new(&source);

  for i in 0..=model.nx {
    println!("{} of {} rows", i, model.nx);
    for j in 0..=model.ny {
      let mut x = map_range(i, 0, model.nx, win.left() - 100.0, win.right() + 100.0)
        + random_range(-3.0, 3.0);
      let mut y = map_range(j, 0, model.ny, win.bottom() - 100.0, win.top() + 100.0)
        + random_range(-3.0, 3.0);

      // Generate points for the line that do not intersect other lines
      let points = (0..model.n_steps)
        .map(|_n| {
          let angle = field(model, x, y, &noisefn);
          x = x + angle.cos();
          y = y + angle.sin();
          let point = pt2(x, y);
          if !model.overlap
            && existing_points
              .iter()
              .any(|pt| pt.distance(point) < model.padding)
          {
            None
          } else {
            Some(point)
          }
        })
        // Skip any initial Nones
        .skip_while(|&o| o.is_none())
        // Take all the Somes until we hit a None.
        // We must call this before filter_map because otherwise non-contiguous
        // chunks of Some(point) might be concatenated and cause discontinuities that are drawn in.
        // According to docs, fuse should work the same way,
        // but it doesn't seem to work properly for this purpose
        // https://doc.rust-lang.org/std/iter/trait.Iterator.html#method.fuse
        .take_while(|&o| o.is_some())
        .filter_map(|o| o)
        .collect::<Line2>();
      // add the points from this line so future lines will avoid it
      existing_points.extend(points.iter());

      draw
        .polyline()
        .caps_round()
        .weight(model.stroke_weight)
        .color(hsl(
          0.0,
          0.0,
          map_range(points.len(), 0, model.n_steps as usize, 0.0, 1.1),
        ))
        .points(points);
    }
  }

  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame_threaded(formatted_frame_path(
      app,
      &frame,
      format!("seed-{}-noisescale-{}", model.seed, model.noise_scale),
    ));
}

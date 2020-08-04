// A classic flow field design, based on gravity.
// Amazing, exceptional, informative essay about flow field techniques here:
// https://tylerxhobbs.com/essays/2020/flow-fields
//
// Algorithm in a nutshell:
// 1. Start with points evenly spaced around a circle
// 2. Incrementally draw small lines on each point.
//    The direction of the small line is determined by a formula (`field` function)
// 3. Add the new point to an array so that future lines can avoid intersection
//
// The field in this drawing is just a noise field.
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::noise::{NoiseFn, OpenSimplex, Terrace};
use nannou::prelude::*;
use nannou::Draw;

mod util;
use util::args::ArgParser;
use util::{captured_frame_path, draw_paper_texture, Line2};

fn main() {
  nannou::app(model).view(view).run();
}

#[derive(Debug)]
struct Model {
  n_lines: i32,
  n_steps: usize,
  stroke_weight: f32,
  // when true, lines will not overlap
  overlap: bool,
  // minimum space allowed between non-overlapping points
  padding: f32,
  seed: f64,
  noise_scale: f64,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  app.new_window().size(1024, 1024).build().unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));

  Model {
    n_lines: args.get("n-lines", 200),
    n_steps: args.get("n-steps", 1000),
    stroke_weight: args.get("stroke-weight", 1.0),
    padding: args.get("padding", 2.0),
    overlap: args.get("overlap", true),
    seed: args.get("seed", random_range(1.0, 10000.0)),
    noise_scale: args.get("noise-scale", random_range(50.0, 400.0)),
  }
}

fn noise_field<T: NoiseFn<[f64; 3]>>(model: &Model, x: f32, y: f32, noisefn: &T) -> f32 {
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
  draw_paper_texture(&draw, &win, 7000, 0.05);

  draw_field(&draw, model, &win);

  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame_threaded(captured_frame_path(app, &frame));
}

fn draw_field(draw: &Draw, model: &Model, win: &Rect) {
  // Holds a record of all points, and allows avoiding overlap if model.overlap == false
  let mut existing_points: Vec<Point2> = Vec::new();
  let color = hsl(0.0, 0.0, 0.1);
  let source = OpenSimplex::new();
  let noisefn = Terrace::new(&source)
    .add_control_point(random_range(-10.0, 10.0))
    .add_control_point(random_range(-10.0, 10.0))
    .add_control_point(random_range(-10.0, 10.0))
    .add_control_point(random_range(-10.0, 10.0))
    .add_control_point(random_range(-10.0, 10.0));

  for i in 0..=model.n_lines {
    let init_angle = map_range(i, 0, model.n_lines, 0.0, 2.0 * PI);
    let mut x = init_angle.cos() * win.left().hypot(win.bottom()) * 0.15;
    let mut y = init_angle.sin() * win.left().hypot(win.bottom()) * 0.15;
    let init_x = x;

    // Generate points for the line that do not intersect other lines
    let points = (0..model.n_steps)
      .map(|_n| {
        // let angle = field(model, x, y);
        let angle = noise_field(model, x, y, &noisefn);
        // hmm, not quite sure why this is necessary
        if init_x > 0.0 {
          x -= angle.cos();
          y -= angle.sin();
        } else {
          x += angle.cos();
          y += angle.sin();
        };

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

    let n_points = points.len();
    for j in 0..n_points {
      if j + 1 > n_points - 1 {
        continue;
      }
      let start = points[j];
      let end = points[j + 1];
      let weight = map_range(j, 0, n_points - 1, 0.5, 3.5);
      // let weight = 1.0;
      draw
        .line()
        .start(start)
        .end(end)
        .weight(weight)
        .color(color);
    }
    // draw
    //   .polyline()
    //   .caps_round()
    //   .weight(model.stroke_weight)
    //   .color(color)
    //   .points(points);
  }
}

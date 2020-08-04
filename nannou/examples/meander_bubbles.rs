// cargo run --release --example meander_bubbles -- --detail 8 --spread 25 --loops 10 --x-start 0 --x-end 0
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::captured_frame_path;

fn main() {
  nannou::app(model).run();
}

struct Model {
  x_start: f32,
  x_end: f32,
  y_start: f32,
  y_end: f32,
  detail: i32,
  max_spread: f32,
  n_bubbles: i32,
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
    x_start: args.get("x-start", random_range(win.x.start, win.x.end)),
    x_end: args.get("x-end", random_range(win.x.start, win.x.end)),
    y_start: args.get("y-start", win.y.start),
    y_end: args.get("y-end", win.y.end),
    // "detail" is the number of recursions we will use in our meandering line.
    // It needn't go much above 10 - it's exponential growth
    detail: args.get("detail", 11),
    max_spread: args.get("spread", 25.),
    n_bubbles: args.get("n-bubbles", 25),
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  // named colors https://docs.rs/nannou/0.13.1/nannou/color/named/index.html
  draw.background().color(IVORY);

  let line = meander(model);
  spread_line(line, &draw, model);

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

// Finite subdivision rule algorithm to create a "fractal" line.
// Each subdivision diplaces the point by a random amount, perpendicular
// to the orientation of the line.
//
// The algorithm uses in-place mutation of an array -- my suspicion
// is that all the inserts are pretty inefficient. I would like to investigate
// using a linked list for this, or possibly another data structure
fn meander(model: &Model) -> Vec<Point2> {
  let start = pt2(model.x_start, model.y_start);
  let end = pt2(model.x_end, model.y_end);
  let mut points: Vec<Point2> = vec![start, end];

  for _recursion in 0..model.detail {
    let temp_points = points.clone();
    let iter_max = temp_points.len() - 1;

    for i in 0..iter_max {
      let one = temp_points[i];
      let two = temp_points[i + 1];
      let x_mid = (two.x + one.x) / 2.0;
      let y_mid = (two.y + one.y) / 2.0;
      let distance = one.distance(two);
      let orientation = ((two.y - one.y) / (two.x - one.x)).atan();
      let perpendicular = orientation + PI / 2.;
      let offset = random_range(distance / -2., distance / 2.);

      let new = pt2(
        x_mid + perpendicular.cos() * offset,
        y_mid + perpendicular.sin() * offset, // may be interesting to have random offset on both x and y?
      );
      points.insert(i * 2 + 1, new);
    }
  }

  points
}

fn spread_line(line: Vec<Point2>, draw: &Draw, model: &Model) {
  for _bubble in 0..model.n_bubbles {
    draw_line(
      line
        .iter()
        .map(|pt| {
          pt2(
            pt.x + random_range(-model.max_spread, model.max_spread),
            pt.y + random_range(-model.max_spread, model.max_spread),
          )
        })
        .collect(),
      draw,
    );
  }
}

fn draw_line(line: Vec<Point2>, draw: &Draw) {
  for point in line {
    draw
      .ellipse()
      .color(IVORY)
      .stroke(hsla(0.0, 0.0, 0.0, 0.6))
      .w_h(10., 10.)
      .x_y(point.x, point.y);
  }
}

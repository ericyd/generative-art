// a random walker implementation,
// with each new step based on the angles of a 3D prism.
//
// cargo run --release --example walker -- --loops 10
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::draw_paper_texture_color;
use util::formatted_frame_path;
use util::grid;
use util::PrismaticWalker;

fn main() {
  nannou::app(model).update(update).run();
}

struct Model {
  n_lines: usize,
  velocity: f32,
  stroke_weight: f32,
  padding: f32,
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

  Model {
    n_lines: args.get("n-lines", 20),
    velocity: args.get("velocity", 10.),
    stroke_weight: args.get("stroke-weight", 2.),
    padding: args.get("padding", 9.),
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  let args = ArgParser::new();
  model.n_lines = args.get("n-lines", random_range(25, 45));
  model.velocity = args.get("velocity", random_range(3.1, 10.));
  model.stroke_weight = args.get("stroke-weight", random_range(1., model.velocity / 3.));
  model.padding = args.get("padding", random_range(model.velocity - 1., model.velocity));
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let win = app.window_rect();
  let draw = app.draw();
  draw.background().color(WHITE);
  draw_paper_texture_color(&draw, &win, 5000, hsla(0.15, 0.8, 0.3, 0.05));

  let angle = PI / 9.;
  let mut existing_points = vec![];

  for (i, j) in grid(model.n_lines, model.n_lines) {
    let scale = 0.8;

    // build from center outwards, to prevent biasing towards a corner.
    // There is probably a better way to do this, but this was the first solution I thought of
    let x = if i % 2 == 0 {
      map_range(i, 0, model.n_lines - 1, 0., win.right() * scale)
    } else {
      map_range(i, 0, model.n_lines - 1, 0., win.left() * scale)
    };

    let y = if j % 2 == 0 {
      map_range(j, 0, model.n_lines - 1, 0., win.top() * scale)
    } else {
      map_range(j, 0, model.n_lines - 1, 0., win.bottom() * scale)
    };

    let start = pt2(x, y);
    let bounds = Rect::from_w_h(win.w() * scale, win.h() * scale);

    let points = PrismaticWalker::new(start, angle)
      .velocity(model.velocity)
      .walk_no_overlap(4000, model.padding, &existing_points, &bounds);

    existing_points.extend(points.iter());

    draw
      .polyline()
      .stroke_weight(model.stroke_weight)
      .points(points);
  }

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  println!("Loop # {}", frame.nth() + 1);
  app.main_window().capture_frame(formatted_frame_path(
    app,
    &frame,
    format!(
      "n_lines-{}-velocity-{}-stroke_weight-{}-padding-{}",
      model.n_lines, model.velocity, model.stroke_weight, model.padding
    ),
  ));
}

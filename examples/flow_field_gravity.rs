// Fixed the gravity field, and extracted to a module (util/gravity)
//
// The idea here is the same as any other flow field
// (see other flow_field examples for more in-depth description of method),
// But here we're calculating the gravitational forces on a point mass.
// The gravity forces can be positive or negative (impossible!),
// can vary in mass and placement, and can vary in spatial position.
// Much of the generation of the actual gravity field is done in the util/gravity module.
//
// It turns out, gravity fields with 2 bodies look the best!
//
// Model { n_lines: 40, n_steps: 300, stroke_weight: 1.0, system: GravitySystem { g: 15.0, bodies: [GravitationalBody { mass: 191.89732, x: 184.47424, y: -542.5523 }, GravitationalBody { mass: -442.96082, x: 645.9241, y: 419.40985 }], n_bodies: 2 }, overlap: false, padding: 10.0, dot_radius: 5, palette: "eric3" }
// Model { n_lines: 40, n_steps: 300, stroke_weight: 1.0, system: GravitySystem { g: 15.0, bodies: [GravitationalBody { mass: 160.33011, x: -121.541626, y: 271.2848 }, GravitationalBody { mass: -327.7808, x: 366.6604, y: 77.8905 }], n_bodies: 2 }, overlap: false, padding: 10.0, dot_radius: 5, palette: "eric3" }
//
// cargo run --release --example flow_field_gravity -- --loops 10 --overlap false --n-bodies 2
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::prelude::*;
use nannou::Draw;

mod util;
use util::args::ArgParser;
use util::color::*;
use util::draw_paper_texture;
use util::{capture_model, captured_frame_path, point_cloud, Line2};
use util::{GravitationalBody, GravitySystem};

fn main() {
  nannou::app(model).view(view).update(update).run();
}

#[derive(Debug)]
struct Model {
  // how many lines in both the x and y axis.
  // Total lines will by n_lines^2
  n_lines: usize,
  // Total positional steps calculated for each line
  n_steps: i32,
  stroke_weight: f32,
  system: GravitySystem,
  // when true, lines cannot overlap.
  // makes rendering much much slower
  overlap: bool,
  // minimum space between points when avoiding overlaps
  padding: f32,
  // size of the dots
  dot_radius: usize,
  palette: String,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  app.new_window().size(1024, 1024).build().unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));
  let win = app.window_rect();

  /* Generate gravitational system */
  let n_bodies = args.get("n-bodies", random_range(2, 9));
  // let system = GravitySystem::generate(n_bodies).g(args.get("g", 1000.0));

  let bodies: Vec<GravitationalBody> = (0..n_bodies)
    .map(|_| GravitationalBody::generate_from_rect(150.0..450.0, true, &win, false))
    .collect();
  let system = GravitySystem::new(args.get("g", 15.0), bodies);

  Model {
    n_lines: args.get("n-lines", 40),
    n_steps: args.get("n-steps", 300),
    stroke_weight: args.get("stroke-weight", 1.0),
    system,
    overlap: args.get("overlap", true),
    padding: args.get("padding", 10.0),
    dot_radius: args.get("dot-radius", 5),
    palette: args.get_string("palette", "eric3"),
  }
}

fn update(app: &App, model: &mut Model, _update: Update) {
  let args = ArgParser::new();
  let win = app.window_rect();
  let n_bodies = args.get("n-bodies", random_range(2, 9));
  // model.system = GravitySystem::generate(n_bodies).g(args.get("g", 1000.0));

  let bodies: Vec<GravitationalBody> = (0..n_bodies)
    .map(|_| GravitationalBody::generate_from_rect(150.0..450.0, true, &win, false))
    .collect();
  model.system = GravitySystem::new(args.get("g", 15.0), bodies);
}

fn field(model: &Model, x: f32, y: f32) -> (f32, f32) {
  model.system.force(x, y)
}

fn view(app: &App, model: &Model, frame: Frame) {
  let draw = app.draw();
  let win = app.window_rect();

  draw.background().color(FLORALWHITE);
  draw_paper_texture(&draw, &win, 5000, 0.04);

  draw_field(&draw, model, &win);

  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame_threaded(captured_frame_path(app, &frame));
  capture_model(app, &frame, model);
}

fn draw_field(draw: &Draw, model: &Model, win: &Rect) {
  // Holds a record of all points, and allows avoiding overlap if model.overlap == false
  let mut existing_points: Vec<Point2> = Vec::new();
  let palette = get_palette(&model.palette);

  // Place points in regular grid
  for pt in point_cloud(
    model.n_lines,
    model.n_lines,
    win.left() * 1.5,
    win.right() * 1.5,
    win.bottom() * 1.5,
    win.top() * 1.5,
  ) {
    let mut x = pt.x + random_range(-10.0, 10.0);
    let mut y = pt.y + random_range(-10.0, 10.0);
    let (h, s, l) = random_color(palette).into_components();
    let h = h.to_positive_degrees() / 360.0;
    let color = hsl(
      random_range(h * 0.95, h * 1.05),
      random_range(s * 0.95, s * 1.05),
      random_range(l * 0.95, l * 1.05),
    );

    // Generate points for the line that do not intersect other lines
    let points = (0..model.n_steps)
      .map(|_n| {
        let (f_x, f_y) = field(model, x, y);
        x += f_x;
        y += f_y;
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
      .take_while(|&o| o.is_some())
      .filter_map(|o| o)
      .step_by(model.dot_radius * 2)
      .collect::<Line2>();
    // add the points from this line so future lines will avoid it
    existing_points.extend(points.iter());

    for point in points {
      draw
        .ellipse()
        .x_y(point.x, point.y)
        .radius(model.dot_radius as f32)
        .color(color);
    }
  }
}

// Extremely similar to flow_field_gravity2, but with different arrangement
// of gravitational bodies.
// A small cluster of bodies is placed close to the center, and additional bodies
// are placed around the exterior of the image.
//
// cargo run --release --example flow_field_gravity3 -- --loops 10
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::prelude::*;
use nannou::Draw;

mod util;
use util::args::ArgParser;
use util::color::*;
use util::{capture_model, captured_frame_path, Line2};
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
  n_steps: usize,
  stroke_weight: f32,
  system: GravitySystem,
  // when true, lines cannot overlap.
  // makes rendering much much slower
  overlap: bool,
  // minimum space between points when avoiding overlaps
  padding: f32,
  // size of the dots
  palette: String,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  app.new_window().size(1024, 1024).build().unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));
  let win = app.window_rect();

  // the model defined here doesn't matter because `update` will run before `view`
  Model {
    n_lines: 100,
    n_steps: 100,
    stroke_weight: 1.0,
    system: generate_system(&args, &win),
    overlap: args.get("overlap", true),
    padding: args.get("padding", 1.0),
    palette: args.get("palette", "random".to_string()),
  }
}

fn update(app: &App, model: &mut Model, _update: Update) {
  let args = ArgParser::new();
  let win = app.window_rect();

  model.system = generate_system(&args, &win);
  model.n_lines = args.get("n-lines", random_range(250, 450));
  model.n_steps = args.get("n-steps", random_range(300, 500));
  model.stroke_weight = args.get("stroke-weight", random_range(0.5, 1.25));
}

// Generate gravitational system
fn generate_system(args: &ArgParser, win: &Rect) -> GravitySystem {
  let n_bodies = args.get("n-bodies", random_range(4, 20));
  let win_scale = 0.25;
  let mut bodies: Vec<GravitationalBody> = (0..n_bodies)
    .map(|_| {
      GravitationalBody::generate_with(
        150.0..300.0,
        true,
        win.left() * win_scale..win.right() * win_scale,
        win.bottom() * win_scale..win.top() * win_scale,
      )
    })
    .collect();

  bodies.extend(
    (0..n_bodies).map(|_| GravitationalBody::generate_from_rect(300.0..550.0, true, &win, true)),
  );
  // let bodies = vec![GravitationalBody { mass: -269.64178, x: 529.4929, y: 448.29968 }, GravitationalBody { mass: -361.54446, x: 59.1969, y: 194.87689 }, GravitationalBody { mass: 430.64758, x: -529.8672, y: -81.48578 }, GravitationalBody { mass: -322.75632, x: -669.6961, y: 153.97766 }, GravitationalBody { mass: 444.74286, x: -58.524292, y: 182.8664 }, GravitationalBody { mass: -279.46735, x: 122.475464, y: -122.651855 }];
  // let bodies = vec![GravitationalBody { mass: -192.06592, x: -587.23535, y: -277.60736 }, GravitationalBody { mass: 233.25151, x: -3.833496, y: -590.09814 }, GravitationalBody { mass: 188.82547, x: -131.2204, y: -218.35068 }, GravitationalBody { mass: 160.60474, x: -63.902466, y: 694.10474 }, GravitationalBody { mass: -207.61653, x: -87.04462, y: -193.02698 }, GravitationalBody { mass: 305.77753, x: 211.37134, y: -138.52759 }, GravitationalBody { mass: -352.32764, x: -124.30194, y: -54.64032 }, GravitationalBody { mass: -223.24216, x: 333.72974, y: 707.2042 }, GravitationalBody { mass: -388.52127, x: -223.32922, y: -713.9999 }, GravitationalBody { mass: 253.91422, x: 54.804077, y: -632.81726 }];
  GravitySystem::new(args.get("g", 1.0), bodies)
}

fn field(model: &Model, x: f32, y: f32) -> (f32, f32) {
  model.system.force(x, y)
}

fn view(app: &App, model: &Model, frame: Frame) {
  let draw = app.draw();

  draw.background().color(FLORALWHITE);

  draw_field(&draw, model);

  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame_threaded(captured_frame_path(app, &frame));
  capture_model(app, &frame, model);
}

fn draw_field(draw: &Draw, model: &Model) {
  // Holds a record of all points, and allows avoiding overlap if model.overlap == false
  let mut existing_points: Vec<Point2> = Vec::new();
  let palette = get_palette(&model.palette);

  for body in &model.system.bodies {
    // draw lots of lines eminating from the body
    for n in 0..model.n_lines {
      let angle = map_range(n, 0, model.n_lines, 0.0, 2.0 * PI);
      let mut x = body.x + angle.cos() * 0.1;
      let mut y = body.y + angle.sin() * 0.1;
      let (h, s, l) = random_color(palette).into_components();
      let h = h.to_positive_degrees() / 360.0;
      let color = hsl(
        random_range(h * 0.95, h * 1.05 + 0.01), // add 0.01 to avoid panics when hue is 0.0
        random_range(s * 0.95, s * 1.05 + 0.01),
        random_range(l * 0.95, l * 1.05 + 0.01),
      );

      // Generate points for the line that do not intersect other lines
      let points = (0..model.n_steps)
        .map(|_n| {
          let (f_x, f_y) = field(model, x, y);
          x += f_x * 4.0; // multiplying the force dramatically improves the "reach" of the lines while conserving memory
          y += f_y * 4.0; // also forces are normalized so max force is 1.0
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
        // Skip any initial Nones, then take all the Somes until we hit another None.
        // We must call this before filter_map so that non-contiguous chunks of Some(point) are not concatenated
        .skip_while(|&o| o.is_none())
        .take_while(|&o| o.is_some())
        .filter_map(|o| o)
        .collect::<Line2>();

      // add the points from this line so future lines will avoid it
      existing_points.extend(points.iter());

      draw
        .polyline()
        .color(color)
        .stroke_weight(model.stroke_weight)
        .points(points);
    }
  }
}

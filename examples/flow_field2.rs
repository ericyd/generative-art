// A classic flow field design, based on gravity.
// Amazing, exceptional, informative essay about flow field techniques here:
// https://tylerxhobbs.com/essays/2020/flow-fields
//
// Algorithm in a nutshell:
// 1. Start with a grid of approximately-evenly spaced points
// 2. Incrementally draw small lines on each point.
//    The direction of the small line is determined by a formula (`field` function)
// 3. Add the new point to an array so that future lines can avoid intersection
//
// The field in this drawing is based on the simplified "three body problem".
// In human terms, it is a visualization of the gravity field a mass-less object
// would experience in different spatial positions.
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::prelude::*;
use nannou::Draw;

mod util;
use util::args::ArgParser;
use util::captured_frame_path;
use util::circle::Circle;
use util::color::*;

fn main() {
  nannou::app(model).view(view).run();
}

// gravitational bodies (b) have a mass, x coord and y coord
struct GravitationalBody {
  mass: f32,
  x: f32,
  y: f32,
}

struct Model {
  nx: i32,
  ny: i32,
  n_steps: i32,
  stroke_weight: f32,
  bodies: Vec<GravitationalBody>,
  // "g" sort of represents the gravitational constant, but...
  g: f32,
  palette: String,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  app.new_window().size(1024, 1024).build().unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));

  /* custom gravitational bodies
  let bodies = vec![
    GravitationalBody {
      mass: 225.4,
      x: 14.8,
      y: 44.0,
    },
    GravitationalBody {
      mass: -196.5,
      x: 215.0,
      y: 74.6,
    },
    GravitationalBody {
      mass: 251.5,
      x: -88.9,
      y: 60.5,
    },
    GravitationalBody {
      mass: -154.8,
      x: 122.7,
      y: 293.1,
    },
    GravitationalBody {
      mass: 173.2,
      x: -219.4,
      y: -222.7,
    },
    GravitationalBody {
      mass: 296.8,
      x: 166.3,
      y: -150.7,
    },
  ];
  */

  /* Random gravitational bodies */
  let n_bodies: i32 = args.get("bodies", random_range(4, 9));
  let bodies: Vec<GravitationalBody> = (0..n_bodies)
    .map(|_| {
      let mass = if random_f32() < 0.5 {
        random_range(-125.0, -300.0)
      } else {
        random_range(125.0, 300.0)
      };
      let x = random_range(-300.0, 300.0);
      let y = random_range(-300.0, 300.0);

      println!(
        "GravitationalBody {{ mass: {}, x: {}, y: {} }},",
        mass, x, y
      );

      GravitationalBody { mass, x, y }
    })
    .collect();

  Model {
    g: args.get("g", -10.0),
    nx: args.get("nx", 80),
    ny: args.get("ny", 80),
    n_steps: args.get("steps", 100),
    stroke_weight: args.get("weight", 3.0),
    bodies,
    palette: args.get_string("palette", "random"),
  }
}

// Currently this just returns an angle,
// meaning the resulting vector field will all be unit vectors.
// I'd love to build this out to accomodate for differing velocities
// of the objects moving through the field, and the resulting
// acceleration on the object.
// from the "Restricted three body problem"
// https://en.wikipedia.org/wiki/Three-body_problem
fn field(model: &Model, x: f32, y: f32) -> f32 {
  let g = -(model.g * 10.0.powi(4));

  let radius = |x_i: f32, y_i: f32| (x - x_i).hypot(y - y_i);

  let x_force = |body: &GravitationalBody| -> f32 {
    g * body.mass * (x - body.x) / radius(body.x, body.y).powi(3)
  };

  let y_force = |body: &GravitationalBody| -> f32 {
    g * body.mass * (y - body.y) / radius(body.x, body.y).powi(3)
  };

  let g_x = model.bodies.iter().fold(0.0, |force, b| force + x_force(b));
  let g_y = model.bodies.iter().fold(0.0, |force, b| force + y_force(b));

  // x, y corresponds to the force vector acting on the point.
  // All we want right now is the angle of that vector, so we
  // just calculate the orientation from the point
  ((y - g_y) / (x - g_x)).atan()
}

fn view(app: &App, model: &Model, frame: Frame) {
  let draw = app.draw();
  let win = app.window_rect();

  draw.background().color(SNOW);
  // draw.background().color(hsl(0., 0., 0.05));
  // draw.background().color(rgb_from_hex("#3E1C4A"));

  draw_field(&draw, model, &win);

  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame_threaded(captured_frame_path(app, &frame));
}

fn draw_field(draw: &Draw, model: &Model, win: &Rect) {
  let palette = get_palette(&model.palette);
  let mut circles: Vec<Circle> = Vec::new();

  for i in 0..=model.nx {
    for j in 0..=model.ny {
      let mut x =
        map_range(i, 0, model.nx, win.left() * 1.3, win.right() * 1.3) + random_range(-3.0, 3.0);
      let mut y =
        map_range(j, 0, model.ny, win.bottom() * 1.3, win.top() * 1.3) + random_range(-3.0, 3.0);

      let color = random_color(palette);

      // we pluck points from the line based on radius size,
      // so this ensures that each line has the same number of points regardless of size
      let n_points = model.n_steps * model.stroke_weight as i32 * 2;
      let mut initial_points = (0..n_points)
        .map(|_n| {
          let angle = field(model, x, y);
          x = x + angle.cos();
          y = y + angle.sin();
          pt2(x, y)
        })
        // this ensures that adjacent dots don't overlap
        .step_by(model.stroke_weight as usize * 2)
        .peekable();
      let mut final_points = Vec::new();

      let mut has_drawn = false;
      while initial_points.peek() != None {
        let p = initial_points.next().unwrap();
        let circle = Circle::new(p.x, p.y, model.stroke_weight * 0.9);
        let has_intersection = circles.iter().any(|c| c.has_intersection(circle));

        // if the line hasn't even placed a single point,
        // keep moving forward until we find a point that works.
        // If there's already at least one point in the line, just cut it off
        if has_intersection && !has_drawn {
          continue;
        } else if has_intersection && has_drawn {
          break;
        }

        has_drawn = true;
        final_points.push(p);
        circles.push(circle);
      }

      draw
        .polyline()
        .caps_round()
        .weight(model.stroke_weight)
        .color(color)
        .points(final_points);
    }
  }
}

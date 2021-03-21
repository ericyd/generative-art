// cargo run --release --example flow_field_dot -- --palette eric3
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
  bodies: Vec<GravitationalBody>,
  // "g" sort of represents the gravitational constant... ish
  g: f32,
  dot_radius: usize,
  palette: String,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  app.new_window().size(1024, 1024).build().unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));

  // custom gravitational bodies
  let bodies = vec![
    GravitationalBody {
      mass: -209.7,
      x: -844.0,
      y: -723.5,
    },
    GravitationalBody {
      mass: 216.5,
      x: -873.6,
      y: 777.5,
    },
    GravitationalBody {
      mass: 179.8,
      x: -414.8,
      y: 929.9,
    },
    GravitationalBody {
      mass: -171.2,
      x: 830.3,
      y: -834.3,
    },
    GravitationalBody {
      mass: 215.4,
      x: 741.3,
      y: 837.97,
    },
  ];

  // randomized gravitational bodies
  // let n_bodies: i32 = args.get("bodies", random_range(3, 6));
  // let bodies: Vec<GravitationalBody> = (0..n_bodies)
  //   .map(|_| {
  //     let mass = if random_f32() < 0.5 {
  //       random_range(-125.0, -225.0)
  //     } else {
  //       random_range(125.0, 225.0)
  //     };
  //     let x = if random_f32() < 0.5 {
  //       random_range(-700.0, -900.0)
  //     } else {
  //       random_range(700.0, 900.0)
  //     };
  //     let y = if random_f32() < 0.5 {
  //       random_range(-700.0, -900.0)
  //     } else {
  //       random_range(700.0, 900.0)
  //     };
  //     println!("mass {}, x {}, y {}", mass, x, y);
  //     GravitationalBody {
  //       mass,
  //       x,
  //       y,
  //     }
  //   })
  //   .collect();

  Model {
    g: args.get("g", 1.0),
    nx: args.get("nx", 100),
    ny: args.get("ny", 100),
    n_steps: args.get("steps", 400),
    bodies,
    dot_radius: args.get("radius", 3),
    palette: args.get_string("palette", "eric3"),
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
  // just calculate the orientation from the point.
  // This is more of the "classic" gravity force
  // ((y - g_y) / (x - g_x)).atan()

  // this is pretty much just a circle, but still cool
  // x.hypot(y)

  // This is, I don't know, maybe the isolines of the gravity field?
  g_x.hypot(g_y)
}

fn view(app: &App, model: &Model, frame: Frame) {
  let draw = app.draw();
  let win = app.window_rect();

  draw.background().color(WHITE);

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
      let n_points = model.n_steps * model.dot_radius as i32 * 2;
      let mut points = (0..n_points)
        .map(|_n| {
          let angle = field(model, x, y);
          x = x + angle.cos();
          y = y + angle.sin();
          pt2(x, y)
        })
        // this ensures that adjacent dots don't overlap
        .step_by(model.dot_radius * 2)
        .peekable();

      let mut has_drawn = false;
      while points.peek() != None {
        let p = points.next().unwrap();
        let circle = Circle::new(p.x, p.y, model.dot_radius as f32 * 0.9);
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
        draw
          .ellipse()
          .radius(model.dot_radius as f32)
          .x_y(p.x, p.y)
          .color(color);
        circles.push(circle);
      }
    }
  }
}

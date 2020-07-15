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
use util::captured_frame_path;
use util::draw_paper_texture_color;
use util::grid;
use util::Line2;

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
    n_lines: args.get("n-lines", 100),
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
  model.padding = args.get("padding", model.velocity - 1.);
}

struct Walker {
  start: Point2,
  base_angle: f32,
  velocity: f32,
}

impl Walker {
  fn new(start: Point2, base_angle: f32) -> Self {
    Walker {
      start,
      base_angle,
      velocity: 5.0,
    }
  }

  fn velocity(mut self, velocity: f32) -> Self {
    self.velocity = velocity;
    self
  }

  fn angle(&self, angle: f32) -> f32 {
    // encourage straight lines
    if random_f32() < 0.8 {
      angle
    } else {
      // kind of a wacky switch/case construct here
      match random_f32() {
        _a if _a < 1. / 6. => PI * 3. / 2.,
        _a if (1. / 6.) <= _a && _a < (2. / 6.) => PI / 2.,
        _a if (2. / 6.) <= _a && _a < (3. / 6.) => self.base_angle,
        _a if (3. / 6.) <= _a && _a < (4. / 6.) => PI - self.base_angle,
        _a if (4. / 6.) <= _a && _a < (5. / 6.) => PI + self.base_angle,
        _ => PI * 2. - self.base_angle,
      }
    }
  }

  // not currently used but could be useful in future
  fn _walk(&self, n: usize) -> Line2 {
    let mut x = self.start.x;
    let mut y = self.start.y;
    let mut angle = self.angle(self.base_angle);
    (0..n)
      .map(move |_n| {
        angle = self.angle(angle);
        x += angle.cos() * self.velocity;
        y += angle.sin() * self.velocity;
        pt2(x, y)
      })
      .collect()
  }

  fn walk_no_overlap(
    &self,
    n: usize,
    padding: f32,
    pre_existing_points: &Vec<Point2>,
    bounds: &Rect,
  ) -> Line2 {
    let mut x = self.start.x;
    let mut y = self.start.y;
    let mut angle = self.angle(self.base_angle);
    let mut existing_points = [&[self.start], &pre_existing_points[..]].concat();
    (0..n)
      .map(|_n| {
        angle = self.angle(angle);
        let new_x = x + angle.cos() * self.velocity;
        let new_y = y + angle.sin() * self.velocity;
        let point = pt2(new_x, new_y);

        if existing_points
          .iter()
          .any(|pt| pt.distance(point) < padding)
          || !bounds.contains(point)
        {
          None
        } else {
          x = new_x;
          y = new_y;
          existing_points.push(point);
          Some(point)
        }
      })
      // pull out the Somes
      .filter_map(|o| o)
      .collect()
  }
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

    let points = Walker::new(start, angle)
      .velocity(model.velocity)
      .walk_no_overlap(4000, model.velocity - 1., &existing_points, &bounds);

    existing_points.extend(points.iter());

    draw
      .polyline()
      .stroke_weight(model.stroke_weight)
      .points(points);
  }

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

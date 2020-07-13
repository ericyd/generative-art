// Drawing prisms in the most simple, brute-force way I could imagine!
// Define a vertex, then the three dimensions for the prism,
// Then, draw the quadrilateral polygons that would be seen as the top, left, and right
// based on a set angle (which is related to the perspective, but extremely simplified)
//
// cargo run --release --example prism
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::{captured_frame_path, Line2};

fn main() {
  nannou::app(model).update(update).run();
}

struct Model {
  n_prisms: usize,
  color: bool,
}

struct Prism {
  vertex: Point2,
  width: f32,
  height: f32,
  depth: f32,
  angle: f32, // radians - relates to perspective
}

impl Prism {
  fn new(vertex: Point2, width: f32, height: f32, depth: f32) -> Self {
    Prism {
      vertex,
      width,
      height,
      depth,
      angle: PI * 1. / 7.,
    }
  }

  // all sides draw points in clockwise order starting with vertex
  fn top(&self) -> Line2 {
    let one = self.vertex;
    let two = pt2(
      self.vertex.x + self.depth * (PI - self.angle).cos(),
      self.vertex.y + self.depth * (PI - self.angle).sin(),
    );
    let three = pt2(
      two.x + self.width * self.angle.cos(),
      two.y + self.width * self.angle.sin(),
    );
    let four = pt2(
      self.vertex.x + self.width * self.angle.cos(),
      self.vertex.y + self.width * self.angle.sin(),
    );
    vec![one, two, three, four]
  }

  fn left(&self) -> Line2 {
    let one = self.vertex;
    let two = pt2(
      self.vertex.x + self.depth * (PI - self.angle).cos(),
      self.vertex.y + self.depth * (PI - self.angle).sin(),
    );
    let three = pt2(two.x, two.y - self.height);
    let four = pt2(self.vertex.x, self.vertex.y - self.height);
    vec![one, two, three, four]
  }

  fn right(&self) -> Line2 {
    let one = self.vertex;
    let two = pt2(
      self.vertex.x + self.width * self.angle.cos(),
      self.vertex.y + self.width * self.angle.sin(),
    );
    let three = pt2(two.x, two.y - self.height);
    let four = pt2(self.vertex.x, self.vertex.y - self.height);
    vec![one, two, three, four]
  }

  fn draw(&self, draw: &Draw, model: &Model) {
    let stroke_weight = 2.;
    let stroke_color = BLACK;
    let color1 = if model.color { SALMON } else { WHITE };
    let color2 = if model.color {
      rgb(252. / 255., 181. / 255., 100. / 255.)
    } else {
      rgb(1., 1., 1.)
    };

    draw
      .polygon()
      .caps_round()
      .join_round()
      .stroke_color(stroke_color)
      .stroke_weight(stroke_weight)
      .color(WHITE)
      .points(self.left());
    draw
      .polygon()
      .caps_round()
      .join_round()
      .stroke_color(stroke_color)
      .stroke_weight(stroke_weight)
      .color(color1)
      .points(self.right());
    draw
      .polygon()
      .caps_round()
      .join_round()
      .stroke_color(stroke_color)
      .stroke_weight(stroke_weight)
      .color(color2)
      .points(self.top());
  }
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
    n_prisms: args.get("n-prisms", 200),
    color: args.get("color", true),
  }
}

fn update(_app: &App, _model: &mut Model, _update: Update) {}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let win = app.window_rect();
  let draw = app.draw();
  draw.background().color(WHITE);

  // In future iterations, it would be cool to "sort" these from back to front,
  // to improve the ordering of the image. But, not very straightforward since order
  // could depend on vertex, w, h, and d.
  //
  // Also could consider matching params, e.g. if width is small, make height also small but depth large.
  // This might result in some interesting shapes
  for _i in 0..model.n_prisms {
    Prism::new(
      pt2(win.x.lerp(random_f32()), win.y.lerp(random_f32())),
      random_range(10., 200.),
      random_range(10., 200.),
      random_range(10., 200.),
    )
    .draw(&draw, model);
  }

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

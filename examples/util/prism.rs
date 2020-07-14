// A super-simple implementation of a 3D prism
// made by simply drawing the polygons that are
// shown when looking with perspective.
// The polygons are all quadrilaterals defined by the vertices
// which are labeled below
//
//  B▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔C
// ▕ ╲               ╲
// ▕  ╲               ╲
// ▕   ╲               ╲
// ▕    ╲     top       ╲
// ▕     ╲               ╲
// G      A ▁▁▁▁▁▁▁▁▁▁▁▁▁▁D
//  ╲ left▕               ▕
//   ╲    ▕               ▕
//    ╲   ▕               ▕
//     ╲  ▕     right     ▕
//      ╲ ▕               ▕
//       ╲▕               ▕
//        ╲F▁▁▁▁▁▁▁▁▁▁▁▁▁▁E

use super::Line2;
use nannou::prelude::*;

pub struct Prism {
  vertex: Point2,
  width: f32,
  height: f32,
  depth: f32,
  angle: f32, // in radians
  left_color: Rgba,
  top_color: Rgba,
  right_color: Rgba,
}

impl Prism {
  pub fn new(vertex: Point2) -> Self {
    Prism {
      vertex,
      width: 1.,
      height: 1.,
      depth: 1.,
      angle: PI / 7.,                                               // arbitrary
      left_color: rgba(1., 1., 1., 1.),                             // white
      top_color: rgba(1., 0.549, 0.412, 1.),                        // salmon
      right_color: rgba(252. / 255., 181. / 255., 100. / 255., 1.), // orangy
    }
  }

  fn a(&self) -> Point2 {
    self.vertex
  }

  fn b(&self) -> Point2 {
    pt2(
      self.vertex.x + self.depth * (PI - self.angle).cos(),
      self.vertex.y + self.depth * (PI - self.angle).sin(),
    )
  }

  fn c(&self) -> Point2 {
    pt2(
      self.b().x + self.width * self.angle.cos(),
      self.b().y + self.width * self.angle.sin(),
    )
  }

  // titled d_ instead of `d` to avoid name conflict with public `d` function
  fn d_(&self) -> Point2 {
    pt2(
      self.vertex.x + self.width * self.angle.cos(),
      self.vertex.y + self.width * self.angle.sin(),
    )
  }

  fn e(&self) -> Point2 {
    pt2(self.d_().x, self.d_().y - self.height)
  }

  fn f(&self) -> Point2 {
    pt2(self.vertex.x, self.vertex.y - self.height)
  }

  fn g(&self) -> Point2 {
    pt2(self.b().x, self.b().y - self.height)
  }

  // all sides draw points in clockwise order starting with vertex
  fn top(&self) -> Line2 {
    vec![self.vertex, self.b(), self.c(), self.d_()]
  }

  fn left(&self) -> Line2 {
    vec![self.vertex, self.f(), self.g(), self.b()]
  }

  fn right(&self) -> Line2 {
    vec![self.vertex, self.d_(), self.e(), self.f()]
  }

  pub fn w(mut self, width: f32) -> Self {
    self.width = width;
    self
  }

  pub fn h(mut self, height: f32) -> Self {
    self.height = height;
    self
  }

  pub fn d(mut self, depth: f32) -> Self {
    self.depth = depth;
    self
  }

  pub fn top_color(mut self, color: Rgba) -> Self {
    self.top_color = color;
    self
  }

  pub fn right_color(mut self, color: Rgba) -> Self {
    self.right_color = color;
    self
  }

  pub fn left_color(mut self, color: Rgba) -> Self {
    self.left_color = color;
    self
  }

  pub fn angle(mut self, radians: f32) -> Self {
    self.angle = radians;
    self
  }

  pub fn draw(&self, draw: &Draw) {
    let stroke_weight = 2.;
    let stroke_color = BLACK;

    draw
      .polygon()
      .caps_round()
      .join_round()
      .stroke_color(stroke_color)
      .stroke_weight(stroke_weight)
      .color(self.left_color)
      .points(self.left());
    draw
      .polygon()
      .caps_round()
      .join_round()
      .stroke_color(stroke_color)
      .stroke_weight(stroke_weight)
      .color(self.right_color)
      .points(self.right());
    draw
      .polygon()
      .caps_round()
      .join_round()
      .stroke_color(stroke_color)
      .stroke_weight(stroke_weight)
      .color(self.top_color)
      .points(self.top());
  }
}

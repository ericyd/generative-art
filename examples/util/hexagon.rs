use nannou::prelude::*;
use nannou::geom::point::Point2;
use std::fmt::{self, Display, Formatter};

#[derive(Debug, Copy, Clone)]
pub struct Hexagon {
  pub x: f32,
  pub y: f32,
  pub radius: f32,
}

impl Hexagon {
  pub fn new(x: f32, y: f32, radius: f32) -> Hexagon {
    Hexagon { x, y, radius }
  }

  fn has_intersection(&self, other: Hexagon) -> bool {
    // should be able to use this method but I'll be damned if Rust makes any sense
    // use nannou::draw::mesh::vertex::IntoVertex;
    // use nannou::geom::polygon::Polygon;
    // use nannou::geom::vertex::Vertex2d;
    // use nannou::mesh::vertex::WithTexCoords;
    // let points = self.points();
    // let polygon = Polygon::new(points.iter());
    // other.points().iter().any(|p| {
    //   // let vertex = p.into_vertex();
    //   match polygon.contains(p) {
    //     // match polygon.contains(&vertex) {
    //   Some(_) => true,
    //   None => false
    // }})
    let vec1 = Point2::new(self.x, self.y);
    let vec2 = Point2::new(other.x, other.y);
    vec1.distance(vec2) < (self.radius + other.radius) * (PI / 6.).cos()
  }

  pub fn points(&self) -> Vec<Point2> {
    (1..=6)
      .map(|n| {
        let factor = n as f32 / 6.;
        let theta = factor * 2. * PI;
        let x = theta.cos() * self.radius + self.x;
        let y = theta.sin() * self.radius + self.y;
        pt2(x, y)
      })
      .collect()
  }
}

impl Display for Hexagon {
  // `f` is a buffer, this method must write the formatted string into it
  fn fmt(&self, f: &mut Formatter) -> fmt::Result {
    // `write!` is like `format!`, but it will write the formatted string
    // into a buffer (the first argument)
    write!(
      f,
      "Hexagon<x: {}, y: {}, radius: {}>",
      self.x, self.y, self.radius
    )
  }
}

pub fn honeycomb_hex(
  radius: f32,
  num_layers: i32,
  padding: f32,
  hole_in_center: bool,
) -> Vec<Hexagon> {
  let num_hexes = 6 * (1..=num_layers).sum::<i32>();
  let mut nodes = vec![Hexagon::new(0.0, 0.0, radius)];
  let mut last_angle = PI / 6.0;
  for idx in 0..num_hexes {
    // try to "turn" PI/3 radians.
    // If there is an intersection, then just continue straight
    let hex = honeycomb_hexagon(nodes[idx as usize], last_angle + (PI / 3.0), padding);
    if nodes.iter().any(|node| node.has_intersection(hex)) {
      nodes.push(honeycomb_hexagon(nodes[idx as usize], last_angle, padding));
    } else {
      nodes.push(hex);
      last_angle = if !hole_in_center && idx == 0 {
        last_angle + (PI * 2.0 / 3.0)
      } else {
        last_angle + (PI / 3.0)
      };
    }
  }
  nodes
}

fn honeycomb_hexagon(other: Hexagon, angle: f32, distance: f32) -> Hexagon {
  let x = angle.cos() * ((other.radius * 2.) * (PI / 6.).cos() + distance) + other.x;
  let y = angle.sin() * ((other.radius * 2.) * (PI / 6.).cos() + distance) + other.y;
  let radius = other.radius;
  Hexagon::new(x, y, radius)
}

use nannou::geom::vector::Vector2;
use nannou::prelude::*;
use std::fmt::{self, Display, Formatter};

#[derive(Debug, Copy, Clone)]
pub struct Circle {
  pub x: f32,
  pub y: f32,
  pub radius: f32,
}

impl Circle {
  pub fn new(x: f32, y: f32, radius: f32) -> Circle {
    Circle { x, y, radius }
  }

  pub fn new_64(x: f64, y: f64, radius: f64) -> Circle {
    Circle {
      x: x as f32,
      y: y as f32,
      radius: radius as f32,
    }
  }

  pub fn has_intersection(&self, other: Circle) -> bool {
    self.has_padded_intersection(other, 0.0)
  }

  pub fn has_padded_intersection(&self, other: Circle, padding: f32) -> bool {
    let vec1 = Vector2::new(self.x, self.y);
    let vec2 = Vector2::new(other.x, other.y);
    vec1.distance(vec2) < self.radius + other.radius + padding
  }

  pub fn contains(&self, point: Point2) -> bool {
    self.contains_padded(point, 0.0)
  }

  pub fn contains_padded(&self, point: Point2, padding: f32) -> bool {
    pt2(self.x, self.y).distance(point) < self.radius + padding
  }
}

impl Display for Circle {
  // `f` is a buffer, this method must write the formatted string into it
  fn fmt(&self, f: &mut Formatter) -> fmt::Result {
    // `write!` is like `format!`, but it will write the formatted string
    // into a buffer (the first argument)
    write!(
      f,
      "Circle<x: {}, y: {}, radius: {}>",
      self.x, self.y, self.radius
    )
  }
}

pub fn honeycomb_circles(
  radius: f32,
  num_layers: i32,
  padding: f32,
  hole_in_center: bool,
) -> Vec<Circle> {
  let num_circles = 6 * (1..=num_layers).sum::<i32>();
  let mut nodes = vec![Circle::new(0.0, 0.0, radius)];
  let mut last_angle = 0.0;
  for idx in 0..num_circles {
    // try to "turn" PI/3 radians.
    // If there is an intersection, then just continue straight
    let circle = honeycomb_circle(nodes[idx as usize], last_angle + (PI / 3.0), padding);
    if nodes.iter().any(|node| node.has_intersection(circle)) {
      nodes.push(honeycomb_circle(nodes[idx as usize], last_angle, padding));
    } else {
      nodes.push(circle);
      last_angle = if !hole_in_center && idx == 0 {
        last_angle + (PI * 2.0 / 3.0)
      } else {
        last_angle + (PI / 3.0)
      };
    }
  }
  nodes
}

fn honeycomb_circle(other: Circle, angle: f32, distance: f32) -> Circle {
  let x = angle.cos() * (other.radius * 2. + distance) + other.x;
  let y = angle.sin() * (other.radius * 2. + distance) + other.y;
  let radius = other.radius;
  Circle::new(x, y, radius)
}

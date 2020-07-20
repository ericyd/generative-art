// Random walker implementations
use super::Line2;
use nannou::prelude::*;

// a walker that follows angular paths that mimic a 3D prism
pub struct PrismaticWalker {
  start: Point2,
  base_angle: f32,
  velocity: f32,
}

impl PrismaticWalker {
  pub fn new(start: Point2, base_angle: f32) -> Self {
    Self {
      start,
      base_angle,
      velocity: 5.0,
    }
  }

  pub fn velocity(mut self, velocity: f32) -> Self {
    self.velocity = velocity;
    self
  }

  pub fn angle(&self, angle: f32) -> f32 {
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
  pub fn walk(&self, n: usize) -> Line2 {
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

  pub fn walk_no_overlap(
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

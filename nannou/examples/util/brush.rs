use super::interp::{lerp, nextf};
use nannou::prelude::*;
use nannou::Draw;
use std::fmt::{self, Display, Formatter};

#[derive(Debug, Copy, Clone)]
pub struct Brush {
  width: f32,
  hsla: [f32; 4],
}

impl Brush {
  pub fn new() -> Self {
    Brush {
      width: 1.,
      hsla: [1., 1., 1., 1.],
    }
  }

  pub fn width(mut self, width: f32) -> Self {
    self.width = width;
    self
  }

  pub fn hsla(mut self, h: f32, s: f32, l: f32, a: f32) -> Self {
    self.hsla = [h, s, l, a];
    self
  }

  pub fn stroke(&self, start: Point2, end: Point2, draw: &Draw) {
    let n_lines = (self.width * 10.) as i32;
    // let original_distance = start.distance(end);
    for n in 0..n_lines {
      let frac = n as f32 / n_lines as f32;
      let (lum, alpha) = self.adjust_lum_alpha();
      let spread = lerp(-self.width, self.width, frac);
      let (start, end) = self.spread(start, end, spread);
      let (start, end) = self.stagger(start, end, frac);
      draw
        .line()
        .start(start)
        .end(end)
        .color(hsla(self.hsla[0], self.hsla[1], lum, alpha))
        .weight(0.5);
    }
  }

  pub fn path(&self, points: Vec<Point2>, draw: &Draw) {
    let n_lines = (self.width * 10.) as i32;
    for n in 0..n_lines {
      let frac = n as f32 / n_lines as f32;
      let (lum, alpha) = self.adjust_lum_alpha();
      let spread = lerp(-self.width, self.width, frac);

      // spread the incoming points based on the frac
      let new_points: Vec<Point2> = points
        .iter()
        .enumerate()
        .flat_map(|(i, pt)| {
          if i == points.len() - 1 || i % 2 == 0 {
            return vec![];
          }
          let start = pt;
          let end = points[i as usize + 1];
          let (start, end) = self.spread(*start, end, spread);
          vec![start, end]
        })
        .collect();

      // instead of "staggering" the start and end, we simply chop the path since it's already a multi-point path.
      // "edge_weight" applies the shortening more to the edges than the middle
      let edge_weight = (0.5 - frac).abs() * 2.;

      // ok, but I think the method below works better
      // let chop_from_start =
      //   (random_range(0., new_points.len() as f32 / 10.) * edge_weight).floor() as usize;
      // let chop_from_end =
      //   (random_range(0., new_points.len() as f32 / 2.) * edge_weight).floor() as usize;

      let chop_from_start =
        (random_range(edge_weight * 2., new_points.len() as f32 / 10.)).floor() as usize;
      let chop_from_end = (random_range(edge_weight * 3., new_points.len() as f32 / 2.)
        * edge_weight)
        .floor() as usize;

      let new_points = new_points[chop_from_start..(new_points.len() - 1 - chop_from_end)].to_vec();

      draw
        .polyline()
        .color(hsla(self.hsla[0], self.hsla[1], lum, alpha))
        .weight(0.5)
        .points(new_points);
    }
  }

  // give the stroke a subtle arc by giving it half a sin curve.
  // Turns out this isn't super easy!
  fn slight_curve(&self, start: Point2, end: Point2, draw: &Draw) {
    let (lum, alpha) = self.adjust_lum_alpha();
    let x_range = end.x - start.x;
    let y_range = end.y - start.y;
    let angle = y_range.atan2(x_range);
    let segments = 20;
    let dx = x_range / segments as f32;
    let dy = y_range / segments as f32;
    let segment_length = start.distance(end) / segments as f32;
    let points: Vec<Point2> = (0..segments)
      .map(|n| {
        let pos = n as f32 / segments as f32;
        let x = angle.cos() * segment_length + dx * n as f32 + start.x;
        let y = angle.sin() * segment_length + dy * n as f32 + start.y;
        // let x = x + (pos * (PI + angle)).cos() * 20.;// + (angle + PI).cos() * 20.;
        let y = y + (pos * (PI + angle)).sin() * 10.; // + (angle + PI).sin() * 20.;
        pt2(x, y)
      })
      .collect();

    draw
      .polyline()
      .color(hsla(self.hsla[0], self.hsla[1], lum, alpha))
      .weight(0.5)
      .points(points);
  }

  fn spread(&self, start: Point2, end: Point2, spread: f32) -> (Point2, Point2) {
    // spread points at a right angle from the original line
    let orientation = (end.y - start.y).atan2(end.x - start.x);
    let angle = orientation + PI / 2.;
    self.spread_by_angle(start, end, spread, angle)
  }

  fn spread_by_angle(
    &self,
    start: Point2,
    end: Point2,
    spread: f32,
    orientation: f32,
  ) -> (Point2, Point2) {
    let start_x = start.x + orientation.cos() * spread;
    let start_y = start.y + orientation.sin() * spread;
    let end_x = end.x + orientation.cos() * spread;
    let end_y = end.y + orientation.sin() * spread;
    (pt2(start_x, start_y), pt2(end_x, end_y))
  }

  // stagger_frac will be a value between 0 and 1.
  // When it is near the middle, offset should be highest
  fn stagger(&self, start: Point2, end: Point2, stagger_frac: f32) -> (Point2, Point2) {
    let middle_weight = 1. - (0.5 - stagger_frac).abs();
    let distance = start.distance(end);
    let orientation = (end.y - start.y).atan2(end.x - start.x);

    let start_offset = nextf(-0.1, 0.1) * middle_weight;
    let start_x = start.x + orientation.cos() * distance * start_offset;
    let start_y = start.y + orientation.sin() * distance * start_offset;

    let end_offset = nextf(-0.5, 0.2) * middle_weight;
    let end_x = end.x + orientation.cos() * distance * end_offset;
    let end_y = end.y + orientation.sin() * distance * end_offset;
    (pt2(start_x, start_y), pt2(end_x, end_y))
  }

  fn adjust_lum_alpha(&self) -> (f32, f32) {
    let lum = if random_f32() < 0.05 {
      self.hsla[2] * 0.5
    } else if random_f32() < 0.5 {
      self.hsla[2] * 1.5
    } else {
      self.hsla[2]
    };
    let alpha = nextf(0.7, self.hsla[3]);
    (lum, alpha)
  }
}

impl Display for Brush {
  // `f` is a buffer, this method must write the formatted string into it
  fn fmt(&self, f: &mut Formatter) -> fmt::Result {
    // `write!` is like `format!`, but it will write the formatted string
    // into a buffer (the first argument)
    write!(f, "Brush<width: {}>", self.width)
  }
}

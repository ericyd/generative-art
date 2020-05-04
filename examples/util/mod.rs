// Something about using a module in examples - it works better to ignore dead code warnings
#![allow(dead_code)]
extern crate chrono;
extern crate nannou;

use chrono::offset::Local;
use nannou::prelude::*;

pub mod args;
pub mod blob;
pub mod brush;
pub mod circle;
pub mod color;
pub mod hexagon;
pub mod interp;

pub type Line2 = Vec<Point2>;

pub fn captured_frame_path_multi(app: &App, frame: &Frame, letter: char) -> std::path::PathBuf {
  // Create a path that we want to save this frame to.
  app
    .project_path()
    .expect("failed to locate `project_path`")
    .join("assets")
    .join(app.exe_name().unwrap())
    // Name each file after the number of the frame.
    .join(format!(
      "{}_{:03}{}",
      Local::now().format("%Y-%m-%dT%H-%M-%S"),
      frame.nth(),
      letter
    ))
    // instagram only works with jpeg :shrug:
    .with_extension("jpeg")
}

pub fn captured_frame_path(app: &App, frame: &Frame) -> std::path::PathBuf {
  captured_frame_path_multi(app, frame, '_')
}

// simple default smoothing function
pub fn smooth(line: &Line2) -> Line2 {
  smooth_by(4, line)
}

// basic line-smoothing algorithm
// TODO: investigate other smoothing algorithms, not sure this is very efficient/good
pub fn smooth_by(smoothness: usize, line: &Line2) -> Line2 {
  let length = line.len();
  line
    .clone()
    .iter()
    .enumerate()
    .map(|(i, _pt)| {
      let min = if i < smoothness { 0 } else { i - smoothness };
      let max = if i + smoothness > length - 1 {
        length - 1
      } else {
        i + smoothness
      };
      // if i > length - 4 || i < smoothness {
      //   return pt.clone();
      // }
      let range = min..max;
      // let magnitude: f32 = range.magnitude().into();
      let magnitude = range.clone().count() as f32;
      let x = &line[range.clone()].iter().map(|p| p.x).sum::<f32>() / magnitude;
      let y = &line[range].iter().map(|p| p.y).sum::<f32>() / magnitude;
      pt2(x, y)
    })
    .collect()
}

// creates a new line with length equal to
//   old_line.len() * 2.powi(depth)
// and shape/curve that is equivalent to the old line
pub fn oversample(old_line: &Line2, depth: i32) -> Line2 {
  meander(old_line, depth, 0.0)
}

// Finite subdivision algorithm to generate fractal line from a starting line.
// The resulting line with have length equal to
//   old_line.len() * 2.powi(depth)
// divergence relates to the distance from the midpoint that the subdivided point will be offset.
pub fn meander(old_line: &Line2, depth: i32, divergence: f32) -> Line2 {
  let mut line = old_line.clone();
  for _recursion in 0..depth {
    let temp_line = line.clone();
    let iter_max = temp_line.len() - 1;

    for i in 0..iter_max {
      let one = temp_line[i];
      let two = temp_line[i + 1];
      let x_mid = (two.x + one.x) / 2.0;
      let y_mid = (two.y + one.y) / 2.0;
      let distance = one.distance(two);
      let orientation = ((two.y - one.y) / (two.x - one.x)).atan();
      let perpendicular = orientation + PI / 2.;
      // must use a conditional here because random_range doesn't like it when start == end
      let offset = if divergence == 0.0 {
        0.0
      } else {
        random_range(distance * -divergence, distance * divergence)
      };

      let new = pt2(
        x_mid + perpendicular.cos() * offset,
        y_mid + perpendicular.sin() * offset, // may be interesting to have random offset on both x and y?
      );
      line.insert(i * 2 + 1, new);
    }
  }

  line
}

// SO FTW https://stackoverflow.com/a/2259502
pub fn rotate(point: Point2, origin: Point2, radians: f32) -> Point2 {
  let sin = radians.sin();
  let cos = radians.cos();

  // translate point back to origin:
  let x = point.x - origin.x;
  let y = point.y - origin.y;

  // rotate point
  let xnew = x * cos - y * sin;
  let ynew = x * sin + y * cos;

  // translate point back:
  let x = xnew + origin.x;
  let y = ynew + origin.y;
  pt2(x, y)
}

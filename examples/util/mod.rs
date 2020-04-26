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

// TODO: would be cool to make an "oversample_smooth_by" function
// that oversamples a line and then smooths it.
// Or, find an easier way to interpolate splines over sparse lines :D

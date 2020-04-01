extern crate chrono;
extern crate nannou;

use chrono::offset::Local;
use nannou::prelude::*;

pub mod blob;
pub mod circle;
pub mod interp;

pub fn captured_frame_path(app: &App, frame: &Frame) -> std::path::PathBuf {
  // Create a path that we want to save this frame to.
  app
    .project_path()
    .expect("failed to locate `project_path`")
    .join("assets")
    .join(app.exe_name().unwrap())
    // Name each file after the number of the frame.
    .join(format!(
      "{}_{:03}",
      Local::now().format("%Y-%m-%dT%H-%M-%S"),
      frame.nth()
    ))
    // instagram only works with jpeg :shrug:
    .with_extension("jpeg")
}

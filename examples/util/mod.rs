extern crate nannou;
extern crate chrono;

use nannou::prelude::*;
use chrono::format::format;
use chrono::offset::Local;

pub mod interp;

pub fn captured_frame_path(app: &App, frame: &Frame) -> std::path::PathBuf {
  // Create a path that we want to save this frame to.
  app
    .project_path()
    .expect("failed to locate `project_path`")
    // Capture all frames to a directory called `/<path_to_nannou>/nannou/simple_capture`.
    .join("assets")
    // Name each file after the number of the frame.
    .join(format!("{}_{}_{:03}", app.exe_name().unwrap(), Local::now().format("%Y-%m-%dT%H-%M-%S"), frame.nth()))
    // The extension will be PNG. We also support tiff, bmp, gif, jpeg, webp and some others.
    .with_extension("png")
}

extern crate nannou;
extern crate chrono;

use nannou::color::*;
use nannou::noise::{NoiseFn, Perlin};
use nannou::prelude::*;

mod util;
use util::interp::{Interp, Interpolate};
use util::captured_frame_path;

fn main() {
  nannou::sketch(view).size(1024, 768).run();
}

fn view(app: &App, frame: Frame) {
  let win = app.window_rect();
  let pi2 = PI * 2.0;
  app.set_loop_mode(LoopMode::NTimes {
    // two frames are necessary for capture_frame to work properly
    number_of_updates: 2,
  });

  // number of vertices in each polyline
  let num_steps = 50;
  // distance between each vertex in polyline
  let default_length = 1.0;
  // higher is less noisy ("longer wavelength")
  let noise_scale = 25.0;
  // number of points in our "grid" of starting points
  let resolution = 75;
  // noise generator
  let perlin = Perlin::new();

  // Prepare to draw.
  let draw = app.draw();

  // Clear the background to black.
  draw.background().color(BLACK);

  for i in 0..resolution {
    let radius_factor = i as f64 / (resolution as f64 - 1.0);
    let start_radius = Interp::lin(0.0, 800.0, radius_factor);

    let points_in_circle = Interp::exp(resolution as f64 * 2.0, resolution as f64 * 4.0, radius_factor).round() as i32;
    for j in 0..points_in_circle {
      let factor = j as f32 / points_in_circle as f32;
  
      // slightly randomize starting position
      let randomize_start = { || (random_f64() * 7.0) - 3.5 };
      let mut current_x = start_radius * (factor as f64 * pi2 as f64).cos() + randomize_start();
      let mut current_y = start_radius * (factor as f64 * pi2 as f64).sin() + randomize_start();

      let mut points = Vec::new();
      let radius = (current_x.powi(2) + current_y.powi(2)).sqrt();
      let color = Hsl::new(
        RgbHue::from_radians(radius as f32 / win.w() * pi2),
        0.7,
        0.5,
      );
      // let line_weight = 4.0 - (radius as f32 / win.w() * 6.0);  // option to make center lines heavier
      let line_weight = 1.0;

      points.push((pt2(current_x as f32, current_y as f32), color));

      for _n in 0..num_steps {
        let radius = (current_x.powi(2) + current_y.powi(2)).sqrt();

        // theta is the angle from the center to the point
        // Since win.y() is a range from negative to positive, theta must be inverted when y is negative
        let theta = match current_y < 0.0 {
          true => (current_x / radius).acos() * -1.0,
          false => (current_x / radius).acos(),
        };

        // noisy weighted gradient pointing away from center
        let noise_factor = 0.75;
        let theta_factor = 1.0;
        let angle = perlin.get([current_x / noise_scale, current_y / noise_scale]) * noise_factor
          + theta * theta_factor;

        let new_x = angle.cos() * default_length + current_x;
        let new_y = angle.sin() * default_length + current_y;

        let color = Hsl::new(
          RgbHue::from_radians(radius as f32 / win.w() * pi2),
          0.7,
          0.5,
        );
        points.push((pt2(new_x as f32, new_y as f32), color));
        current_x = new_x;
        current_y = new_y;
      }

      draw
        .polyline()
        .start_cap_round()
        .weight(line_weight)
        .colored_points(points);
    }
  }

  // Write to the window frame.
  draw.to_frame(app, &frame).unwrap();

  // credit: https://github.com/nannou-org/nannou/blob/6dd78a5a7c966d46f25a4f56aeedfc3f4e54c7f5/examples/simple_capture.rs
  let file_path = captured_frame_path(app, &frame);
  app.main_window().capture_frame_threaded(file_path);
}

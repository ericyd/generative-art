// https://www.instagram.com/p/B-ieQE8HD_O/
// cargo run --release --example flow_fields_1

extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::prelude::*;

mod util;

use util::captured_frame_path_multi;
use util::interp::{Interp, Interpolate};

fn main() {
  nannou::app(model).update(update).view(view).run();
}

struct Model {
  a: WindowId,
  b: WindowId,
  c: WindowId,
  d: WindowId,
}

fn model(app: &App) -> Model {
  let a = app
    .new_window()
    .size(1024, 1024)
    .title("Window A")
    .build()
    .unwrap();
  let b = app
    .new_window()
    .size(1024, 1024)
    .title("Window B")
    .build()
    .unwrap();
  let c = app
    .new_window()
    .size(1024, 1024)
    .title("Window C")
    .build()
    .unwrap();
  let d = app
    .new_window()
    .size(1024, 1024)
    .title("Window D")
    .build()
    .unwrap();
  Model { a, b, c, d }
}

fn update(_app: &App, _model: &mut Model, _update: Update) {}

// There are so many options for interesting fields, the below are just a starting point!
//    m * (x + y) / (x.powi(2) + y.powi(2) + n)
//    m * (x - y) / (x.powi(2) + y.powi(2) + n)
//    where 500 < m < 1000 and 2000 < n < 10000
// pushing m and n outside of those ranges can result in some trippy designs,
// but be aware that the gradients may blow up around the center of the image,
// as the radius gets very small.
//
// there are plenty of other interesting options out there too!
//    100000.0 * (x - y) / (x.powi(2) + y.powi(2) + 10000.0)
//    100000.0 * (x - y) / (x + y + 1000.0).powi(2)
//    1000.0 * (x + y) / ((x - y).powi(2) + 2200.0)

fn generic_field_1(m: f32, n: f32, x: f32, y: f32) -> f32 {
  m * (x + y) / (x.powi(2) + y.powi(2) + n)
}

fn field1(x: f32, y: f32) -> f32 {
  generic_field_1(500., 8000., x, y)
}

fn field2(x: f32, y: f32) -> f32 {
  generic_field_1(10000., 10000., x, -y)
}

fn field3(x: f32, y: f32) -> f32 {
  500.0 * x.hypot(y) / (x.powi(2) + y.powi(2) + 1000.0)
}

fn field4(x: f32, y: f32) -> f32 {
  800.0 * (x - y).abs() / (x.powi(2) + y.powi(2) + 9000.0)
}

fn view(app: &App, model: &Model, frame: Frame) {
  app.set_loop_mode(LoopMode::NTimes {
    // two frames are necessary for capture_frame to work properly
    number_of_updates: 2,
  });

  match frame.window_id() {
    id if id == model.a => magnetic_field(id, app, frame, field1, 'a', [0.5, 0.7], [0.01, 0.2]),
    id if id == model.b => magnetic_field(id, app, frame, field2, 'b', [0.6, 0.75], [0.1, 0.29]),
    id if id == model.c => magnetic_field(id, app, frame, field3, 'c', [0.44, 0.62], [0.55, 0.78]),
    id if id == model.d => magnetic_field(id, app, frame, field4, 'd', [0.12, 0.23], [0.34, 0.45]),
    _ => (),
  }
}

// char_id is only useful for frame capturing, no other utility
fn magnetic_field(
  id: WindowId,
  app: &App,
  frame: Frame,
  field: fn(f32, f32) -> f32,
  char_id: char,
  color_range_1: [f32; 2],
  color_range_2: [f32; 2],
) {
  let win = match app.window(id) {
    Some(window) => window.rect(),
    None => return,
  };

  // Prepare to draw.
  let draw = app.draw();

  let pi2 = PI * 2.0;
  // number of vertices in each polyline
  let num_steps = 500;
  // distance between each vertex in polyline
  let default_length = 1.0;
  // number of points in our "grid" of starting points
  let resolution = 105;

  // Clear the background to black.
  draw.background().color(BLACK);

  let num_circles = 40;
  for i in 0..num_circles {
    let radius_factor = i as f32 / num_circles as f32;
    let start_radius = Interp::exp(10.0, 800.0, radius_factor);

    for j in 0..resolution {
      let factor = j as f32 / resolution as f32;

      // slightly randomize starting position
      let randomize_start = { || (random_f32() * 7.0) - 3.5 };
      let mut current_x = start_radius * (factor * pi2).cos() + randomize_start();
      let mut current_y = start_radius * (factor * pi2).sin() + randomize_start();

      let mut points = Vec::new();
      let radius = (current_x.powi(2) + current_y.powi(2)).sqrt();

      let factor = if current_y < -1.0 * current_x {
        Interp::lin(color_range_1[0], color_range_1[1], radius / win.w())
      } else {
        Interp::lin(color_range_2[0], color_range_2[1], radius / win.w())
      };

      let hue = RgbHue::from_radians(factor * pi2);
      let color = Alpha::<Hsl<_, _>, f32>::new(hue, 0.5, 0.38, 0.3);
      let line_weight = 1.0;
      points.push((pt2(current_x, current_y), color));

      for _n in 0..num_steps {
        let angle = field(current_x, current_y);

        let new_x = angle.cos() * default_length + current_x;
        let new_y = angle.sin() * default_length + current_y;

        points.push((pt2(new_x, new_y), color));
        current_x = new_x;
        current_y = new_y;
      }

      draw
        .polyline()
        .start_cap_round()
        .weight(line_weight)
        .points_colored(points);
    }
  }

  // Write to the window frame.
  draw.to_frame(app, &frame).unwrap();

  // credit: https://github.com/nannou-org/nannou/blob/6dd78a5a7c966d46f25a4f56aeedfc3f4e54c7f5/examples/simple_capture.rs
  let file_path = captured_frame_path_multi(app, &frame, char_id, String::from(""));
  app.window(id).unwrap().capture_frame_threaded(file_path);
}

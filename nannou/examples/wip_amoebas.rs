// A sketch inspired by the pattern created by a solution of baking soda that dried on my counter.
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::blob::*;
use util::color::*;
use util::{captured_frame_path, Line2};

fn main() {
  nannou::app(model).run();
}

struct Model {
  palette: String,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();

  app
    .new_window()
    .title(app.exe_name().unwrap())
    .view(view)
    .size(1024, 1024)
    .build()
    .unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get_usize("loops", 1)));

  Model {
    palette: args.get_string("palette", "random"),
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  let win = app.window_rect();
  draw.background().color(hsl(0.0, 0.0, 0.01));
  let palette = get_palette(&model.palette);

  let blobs1 = blobs(pt2(100.0, -100.0), 200);
  let blobs2 = blobs(pt2(-300.0, 300.0), 75);
  let blobs3 = blobs(pt2(-400.0, -350.0), 50);
  draw_outer_ring(&draw, blobs1, random_color(palette));
  draw_outer_ring(&draw, blobs2, random_color(palette));
  draw_outer_ring(&draw, blobs3, random_color(palette));

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

fn blobs(origin: Point2, max_radius: i32) -> Vec<Line2> {
  let seed = random_f32();
  let rotation = random_range(0.0, 2.0 * PI);
  (5..max_radius)
    .step_by(2)
    .map(|r| {
      Blob::new()
        .x_y(origin.x, origin.y)
        .w_h(r as f32, r as f32)
        .noise_scale(1.0)
        .seed(seed)
        .resolution(max_radius * 4)
        .rotate_rad(rotation)
        .points()
    })
    .collect()
}

fn draw_outer_ring(draw: &Draw, blobs: Vec<Line2>, color: Hsl) {
  let (h, s, l) = color.into_components();
  let h = h.to_positive_degrees() / 360.0;
  for (i, blob) in blobs.iter().enumerate() {
    // outer ring has much larger points
    let radius = if i == blobs.len() - 1 {
      random_range(2.0, 4.0)
    } else {
      random_range(0.5, 1.5)
    };
    // outer ring has substantially higher chance of drawing point
    let chance = if i == blobs.len() - 1 {
      0.5
    } else {
      1.0 - map_range(i, 0, blobs.len() - 1, 0.95, 0.8)
    };
    for point in blob {
      if random_f32() < chance {
        draw
          .ellipse()
          .x_y(point.x, point.y)
          .radius(radius)
          .color(hsl(h, s, random_range(l * 0.9, l * 1.1)));
        // .color(hsla(0.0, 0.0, random_range(0.7, 1.0), 1.0));
      }
    }
  }
}

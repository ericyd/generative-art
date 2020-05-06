// This is essentially a copy of meander_smoke2 with a couple differences:
// 1. The blobs are distorted in slightly different ways
// 2. The meander line is seeded with a "straight" line cuttiing diagnally through the frame.
// 3. There is "panelling" added to the final image
//
// meander_smoke2.rs has more details about the algorithm used
//   cargo run --release --example meander_smoke3
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::blob::*;
use util::interp::*;
use util::{captured_frame_path, oversample, smooth};

type Line2 = Vec<Point2>;

fn main() {
  nannou::app(model).run();
}

struct Model {
  // where the "base line" starts
  x_start: f32,
  y_start: f32,
  // where the "base line" ends
  x_end: f32,
  y_end: f32,

  // the noise scale of the blobs
  noise: f32,
  // the number of blobs applied to the base line
  n_blobs: i32,
  // the speed at which blobs rotate
  rotation: f32,
  // the speed at which blob deformation changes
  seed_scale: f32,
  // a pct by which the radius changes over the length of the base line
  // Should be in (1,10)
  radius: f32,
  // the number of points on each blob: translates to density of flow lines
  resolution: i32,
  // the number of recursions in the fractal line generation
  detail: i32,
  // the maximum deviation of each fractal subdivision
  meander_strength: f32,
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
  let win = app.window_rect();

  Model {
    x_start: args.get("x-start", win.left()),
    x_end: args.get("x-end", win.right()),
    y_start: args.get("y-start", win.bottom()),
    y_end: args.get("y-end", win.top()),
    noise: args.get("noise", 2.0),
    n_blobs: args.get("blobs", 1000),
    rotation: args.get("rotation", 5.0),
    seed_scale: args.get("seed", 5.),
    radius: args.get("radius", 5.),
    resolution: args.get("resolution", 260),
    detail: args.get("detail", 9),
    meander_strength: args.get("strength", 5.0),
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  let win = app.window_rect();
  draw_left_bg(&draw, &win);
  draw_center_bg(&draw, &win);
  draw_right_bg(&draw, &win);

  let base_line = generate_primary_line(model);
  // sharp
  // let blobs = apply_blobs_to_line(oversample(7, &base_line), model);
  // smooth
  let blobs = apply_blobs_to_line(oversample(7, &smooth(&base_line)), model);
  let colored_lines = map_blobs_to_lines(blobs, model);
  draw_smoke(colored_lines, &draw);

  draw_frame(&draw, &win);

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

fn generate_primary_line(model: &Model) -> Line2 {
  let start = pt2(model.x_start, model.y_start);
  let mid = pt2(
    (model.x_end + model.x_start) / 2.0,
    (model.y_end + model.y_start) / 2.0,
  );
  let end = pt2(model.x_end, model.y_end);
  let start_line = vec![start, mid, end];
  meander(&start_line, model.detail, model.meander_strength / 10.)
}

fn apply_blobs_to_line(line: Line2, model: &Model) -> Vec<Line2> {
  let length = line.len();

  let step = if length / model.n_blobs as usize > 0 {
    length / model.n_blobs as usize
  } else {
    println!(
      "Warning! Not enough steps in the line for the requested number of blobs.
    Defaulting to 1 blob per line segment
    {} line segements
    {} requested blobs",
      length, model.n_blobs
    );
    1
  };

  line
    .iter()
    .enumerate()
    .step_by(step)
    .map(|(i, point)| {
      let pct = i as f32 / length as f32;
      let diameter = ((pct * PI * 6.).sin() * 0.65 + 1.) * model.radius * (2. - pct);
      // let diameter = (pct + 0.85).powf(pct + model.radius);
      Blob::new()
        .x_y(point.x, point.y)
        .w_h(diameter, diameter)
        .resolution(model.resolution)
        .seed(pct * model.seed_scale)
        .noise_scale(model.noise)
        .rotate_rad(((pct * 4.0 * PI).sin() + 1.0) / 2.0 * model.rotation)
        .points()
    })
    .collect()
}

fn map_blobs_to_lines(blobs: Vec<Line2>, model: &Model) -> Vec<Vec<(Point2, LinSrgba)>> {
  let blob_count = blobs.len() as f32;
  (0..model.resolution)
    .map(|n| {
      blobs
        .iter()
        .enumerate()
        .map(|(i, blob)| {
          let pct = n as f32 / model.resolution as f32;
          let centerness = 1.0 - (0.5 - pct).abs() * 2.0;
          let alpha = lerp(0.001, 0.1, centerness);
          // maybe weird to instantiate LinSrgba this way, but hsla is so much easier to work with :shrug:
          let orange = LinSrgba::from(hsla(23. / 360., 0.96, 0.48, alpha));
          let pink = LinSrgba::from(hsla(216. / 360., 0.96, 0.82, i as f32 / blob_count * alpha));
          (blob[n as usize], orange.overlay(pink))
        })
        .collect()
    })
    .collect()
}

fn draw_left_bg(draw: &Draw, win: &Rect) {
  let n = 200;
  for i in 0..=n {
    let color = rgb(
      55. / 255.,
      35. / 255.,
      72. / 255. + 0.05 * map_range(i, 0, n, 0.0, 1.0),
    );
    let w_over_3 = win.w() / 3.0;
    draw
      .rect()
      .x_y(
        win.left() + w_over_3 * 0.5,
        win.y.lerp(map_range(i, 0, n, 0.0, 1.0)),
      )
      .w_h(w_over_3, win.h() / n as f32)
      .color(color);
  }
}

fn draw_center_bg(draw: &Draw, win: &Rect) {
  let n = 200;
  for i in 0..=n {
    // hsl isn't mapping right for some reason...
    // let color = rgb(
    //   23. / 255.,
    //   38. / 255.,
    //   79. / 255. + 0.05 * map_range(i, 0, n, 0.0, 1.0),
    // );
    let color = rgb(
      55. / 255.,
      21. / 255.,
      43. / 255. + 0.05 * map_range(i, 0, n, 0.0, 1.0),
    );
    let w_over_3 = win.w() / 3.0;
    draw
      .rect()
      .x_y(
        win.left() + w_over_3 * 1.5,
        win.y.lerp(map_range(i, 0, n, 0.0, 1.0)),
      )
      .w_h(w_over_3, win.h() / n as f32)
      .color(color);
  }
}

fn draw_right_bg(draw: &Draw, win: &Rect) {
  let n = 200;
  for i in 0..=n {
    // let color = hsl(276. / 360., 0.35, 0.09 + 0.05 * map_range(i, 0, n, 0.0, 1.0));
    let color = rgb(
      99. / 255.,
      29. / 255.,
      51. / 255. + 0.05 * map_range(i, 0, n, 0.0, 1.0),
    );
    let w_over_3 = win.w() / 3.0;
    draw
      .rect()
      .x_y(
        win.left() + w_over_3 * 2.5,
        win.y.lerp(map_range(i, 0, n, 0.0, 1.0)),
      )
      .w_h(w_over_3, win.h() / n as f32)
      .color(color);
  }
}

fn draw_frame(draw: &Draw, win: &Rect) {
  let w_over_3 = win.w() / 3.0;
  let border = 30.0;

  // left frame
  draw
    .rect()
    .x_y(win.left() + w_over_3 * 0.5 + border / 2.0, 0.0)
    .w_h(w_over_3, win.h())
    .color(hsla(0., 0., 0., 0.))
    .stroke(hsl(0., 0., 0.))
    .stroke_weight(border);

  // right frame
  draw
    .rect()
    .x_y(win.left() + w_over_3 * 2.5 - border / 2.0, 0.0)
    .w_h(w_over_3, win.h())
    .color(hsla(0., 0., 0., 0.))
    .stroke(hsl(0., 0., 0.))
    .stroke_weight(border);

  // outer frame
  draw
    .rect()
    .x_y(0.0, 0.0)
    .w_h(win.w(), win.h())
    .color(hsla(0., 0., 0., 0.))
    .stroke(hsl(0., 0., 0.))
    .stroke_weight(border * 2.0);
}

fn draw_smoke(lines: Vec<Vec<(Point2, LinSrgba)>>, draw: &Draw) {
  let length = lines[0].len();
  for (i, line) in lines.iter().enumerate() {
    let pct = i as f32 / length as f32;
    let centerness = 1.0 - (0.5 - pct).abs() * 2.0;
    let weight = lerp(0.05, 4., centerness);

    draw.polyline().weight(weight).colored_points(line.clone());
  }
}

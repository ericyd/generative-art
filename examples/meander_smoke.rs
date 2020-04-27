// Shout out to DOGAMI and the Willamette River!
// https://www.oregongeology.org/pubs/ll/p-poster-willamette.htm
// cargo run --release --example meander_smoke -- --resolution 300 --rotation 35 --seed 15 --meander 4 --radius 25 --noise 0.96 --blobs 1000 --detail 11
// cargo run --release --example meander_smoke -- --resolution 300 --rotation 5 --seed 55 --meander 5 --radius 25 --noise 1.36 --blobs 1000 --detail 11
// cargo run --release --example meander_smoke -- --resolution 300 --rotation 5 --seed 15 --meander 5 --radius 25 --noise 1.36 --blobs 1000 --detail 11
// cargo run --release --example meander_smoke -- --resolution 300 --rotation 15 --seed 15 --meander 2 --radius 25 --noise 1.36 --blobs 1000 --detail 11
// cargo run --release --example meander_smoke -- --resolution 200 --rotation 15 --seed 15 --meander 2 --radius 35 --noise 1.36 --blobs 500 --detail 11
// cargo run --release --example meander_smoke -- --resolution 250 --rotation 19 --seed 35 --meander 5 --radius 15 --noise 1.46 --blobs 300
// cargo run --release --example meander_smoke -- --resolution 150 --rotation 19 --seed 45 --meander 5 --radius 15 --noise 1.76 --blobs 400
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::blob::*;
use util::interp::*;
use util::{captured_frame_path, smooth};

type Line2 = Vec<Point2>;

fn main() {
  nannou::app(model).run();
}

struct Model {
  x_start: f32,
  x_end: f32,
  y_start: f32,
  y_end: f32,
  detail: i32,
  noise: f32,
  n_blobs: i32,
  meander_strength: f32,
  rotation: f32,
  seed_factor: f32,
  radius: f32,
  resolution: i32,
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
    x_start: args.get("x-start", 0.0),
    x_end: args.get("x-end", 0.0),
    y_start: args.get("y-start", win.y.start - 100.),
    y_end: args.get("y-end", win.y.end + 100.),
    // "detail" is the number of recursions we will use in our meandering line.
    // It needn't go much above 10 - it's exponential growth
    detail: args.get("detail", 10),
    noise: args.get("noise", 1.0),
    n_blobs: args.get("blobs", 300),
    // this value gets divided by 10 when determining placement of subdivision points
    meander_strength: args.get("meander", 3.),
    // "speed" at which the blob rotates
    rotation: args.get("rotation", 0.0),
    // "speed" at which the blob morphs
    seed_factor: args.get("seed", 3.),
    radius: args.get("radius", 35.),
    resolution: args.get("resolution", 360),
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  // draw.background().color(B);
  draw_bg(&draw, &app.window_rect());

  let base_line = generate_primary_line(model);
  let blobs = apply_blobs_to_line(smooth(&base_line), model);
  let river_lines = map_blobs_to_lines(blobs, model);

  draw_river(river_lines, &draw);

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

fn generate_primary_line(model: &Model) -> Line2 {
  let start = pt2(model.x_start, model.y_start);
  let end = pt2(model.x_end, model.y_end);
  let start_line = vec![start, end];
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
      let factor = i as f32 / length as f32;
      Blob::new()
        .x_y(point.x, point.y)
        .w_h(model.radius * 2., model.radius * 2.)
        .seed(factor * model.seed_factor)
        .noise_scale(model.noise)
        .rotate_rad(factor * model.rotation)
        .resolution(model.resolution)
        .points()
    })
    .collect()
}

fn map_blobs_to_lines(blobs: Vec<Line2>, model: &Model) -> Vec<Line2> {
  (0..model.resolution)
    .map(|n| blobs.iter().map(|blob| blob[n as usize]).collect())
    .collect()
}

fn draw_bg(draw: &Draw, win: &Rect) {
  for i in 0..100 {
    let factor = i as f32 / 99.0;
    let dark_blue = hsl(229. / 360., 0.87, 0.01 + 0.1 * factor);
    draw
      .rect()
      .x_y(0.0, win.y.lerp(factor))
      .w_h(win.x.magnitude(), win.y.magnitude() / 99.0)
      .color(dark_blue);
  }
}

fn draw_river(river_lines: Vec<Line2>, draw: &Draw) {
  let length = river_lines[0].len();
  for (i, line) in river_lines.iter().enumerate() {
    let factor = i as f32 / length as f32;
    let centerness = 1.0 - (0.5 - factor).abs() * 2.0;
    let alpha = lerp(0.001, 0.1, centerness);
    let color = hsla(0.0, 0.0, 1.0, alpha);
    let weight = lerp(0.05, 4., centerness);

    draw
      .polyline()
      .color(color)
      // .weight(centerness * 4.)
      .weight(weight)
      .points(line.clone());
  }
}

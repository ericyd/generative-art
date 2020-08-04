// cargo run --release --example meander_blobs -- --blobs 400 --noise 0.8 --x-start -500 --x-end 700 --y-start -200 --y-end 200 --detail 10 --meander 3 --rotation 0 --seed 15
// cargo run --release --example meander_blobs -- --blobs 750 --noise 01 --x-start -700 --x-end 700 --y-start -200 --y-end 400 --detail 11 --meander 3 --rotation 0 --seed 15 --radius 80
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::blob::*;
use util::captured_frame_path;

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
    x_start: args.get("x-start", win.x.start - 100.),
    x_end: args.get("x-end", win.x.end + 100.),
    y_start: args.get("y-start", 0.),
    y_end: args.get("y-end", 0.),
    // "detail" is the number of recursions we will use in our meandering line.
    // It needn't go much above 10 - it's exponential growth
    detail: args.get("detail", 10),
    noise: args.get("noise", 1.0),
    n_blobs: args.get("blobs", 300),
    // this value gets divided by 10 when determining placement of subdivision points
    meander_strength: args.get("meander", 5.),
    // "speed" at which the blob rotates
    rotation: args.get("rotation", 0.5),
    // "speed" at which the blob morphs
    seed_factor: args.get("seed", 3.),
    radius: args.get("radius", 35.),
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  // named colors https://docs.rs/nannou/0.13.1/nannou/color/named/index.html
  draw.background().color(IVORY);

  let line = meander(model);
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
  let orange = hsla(27. / 360., 0.96, 0.68, 1.0);
  let blue = hsla(207.5 / 360., 0.96, 0.63, 1.0);
  let gradient = Gradient::new(vec![orange, blue]);

  for (i, point) in line.iter().enumerate().step_by(step) {
    let factor = i as f32 / length as f32;
    let blob = Blob::new()
      .x_y(point.x, point.y)
      .w_h(model.radius * 2., model.radius * 2.)
      .seed(factor * model.seed_factor)
      .noise_scale(model.noise)
      .rotate_rad(factor * model.rotation)
      .points();

    let color = gradient.get(factor);
    draw_blob(blob, color, &draw);
  }

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

// Finite subdivision rule algorithm to create a "fractal" line.
// Each subdivision diplaces the point by a random amount, perpendicular
// to the orientation of the line.
//
// The algorithm uses in-place mutation of an array -- my suspicion
// is that all the inserts are pretty inefficient. I would like to investigate
// using a linked list for this, or possibly another data structure
fn meander(model: &Model) -> Vec<Point2> {
  let start = pt2(model.x_start, model.y_start);
  let end = pt2(model.x_end, model.y_end);
  let mut points: Vec<Point2> = vec![start, end];

  for _recursion in 0..model.detail {
    let temp_points = points.clone();
    let iter_max = temp_points.len() - 1;

    for i in 0..iter_max {
      let one = temp_points[i];
      let two = temp_points[i + 1];
      let x_mid = (two.x + one.x) / 2.0;
      let y_mid = (two.y + one.y) / 2.0;
      let distance = one.distance(two);
      let orientation = ((two.y - one.y) / (two.x - one.x)).atan();
      let perpendicular = orientation + PI / 2.;
      let offset = random_range(
        distance * (-model.meander_strength / 10.),
        distance * (model.meander_strength / 10.),
      );

      let new = pt2(
        x_mid + perpendicular.cos() * offset,
        y_mid + perpendicular.sin() * offset, // may be interesting to have random offset on both x and y?
      );
      points.insert(i * 2 + 1, new);
    }
  }

  points
}

fn draw_blob(blob: Vec<Point2>, color: Hsla, draw: &Draw) {
  // I don't really know if this is a good way of simulating sepia tones,
  // but leaving it here as a good example of blending colors
  let sepia = LinSrgba::new(240. / 255., 209. / 255., 122. / 255., 0.7);
  let (h, s, l) = Hsl::from(color).into_components();
  draw
    .polygon()
    .color(LinSrgba::from(color).overlay(sepia))
    // honestly cannot decide which I prefer, but the bold colors are pretty compelling
    .stroke(hsla(h.to_positive_degrees() / 360., s * 0.5, l * 0.2, 1.0))
    // .stroke(hsla(0.0, 0.0, 0.2, 1.0))
    .stroke_weight(1.5)
    .points(blob);
}

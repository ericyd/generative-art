// This is extremely similar to meander_smoke example,
// except this uses a variable radius to enhance the "smoke" effect.
// In real life, streams of smoke generally start in laminar flow (small radius)
// and change to turbulent flow (large radius) as they move through space.
// Another key difference: the base line here is not a fractal, it is a sine curve.
//
// Could be an interesting project to revisit meander_smoke example with variable radius too.
//
// Algorithm in a nutshell:
//  1. Create a "base line" which is just a stretched sine curve
//  2. Apply "blobs" along the line. Blobs are circles distorted with Perlin noise
//     The blobs are distorted and rotated by varying degrees based on input params.
//  3. Draw lines along the blobs to form lengthwise lines, "parallel" to the base line
//     (note: they are definitely not parallel)
//  4. Voila you are complete.
//
// For quick iteration/experimentation, this sketch accepts command line args
// to adjust parameters without recompiling. A full example looks something like this.
//   cargo run --release --example meander_smoke2 -- --x_start -1000 --x_end 1000 --noise 4 --n_blobs 200 --rotation 3 --seed_factor 4 --radius 6 --resolution 1000
//   cargo run --release --example meander_smoke2 -- --seed 15 --noise 3.0  --radius 3 --resolution 1000 --rotation 15
//   cargo run --release --example meander_smoke2 -- --seed 5 --radius 6 --resolution 1000
// It's a poor man's arg parser so you'll have to read the code to understand what each param means.
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::blob::*;
use util::captured_frame_path;
use util::interp::*;

type Line2 = Vec<Point2>;

fn main() {
  nannou::app(model).run();
}

struct Model {
  // where the "base line" starts
  x_start: f32,
  // where the "base line" ends
  x_end: f32,
  // the noise scale of the blobs
  noise: f32,
  // the number of blobs applied to the base line
  n_blobs: i32,
  // the speed at which blobs rotate
  rotation: f32,
  // the speed at which blob deformation changes
  seed_factor: f32,
  // a factor by which the radius changes over the length of the base line
  // Should be in (1,10)
  radius: f32,
  // the number of points on each blob: translates to density of flow lines
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
    x_start: args.get("x-start", win.left()),
    x_end: args.get("x-end", win.right()),
    noise: args.get("noise", 2.0),
    n_blobs: args.get("blobs", 500),
    rotation: args.get("rotation", 5.0),
    seed_factor: args.get("seed", 5.),
    radius: args.get("radius", 5.),
    resolution: args.get("resolution", 720),
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  // draw.background().color(WHITESMOKE);
  draw_bg(&draw, &app.window_rect());

  let base_line = generate_primary_line(model);
  let blobs = apply_blobs_to_line(base_line, model);
  let colored_lines = map_blobs_to_lines(blobs, model);
  draw_smoke(colored_lines, &draw);

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

fn generate_primary_line(model: &Model) -> Line2 {
  (0..1000)
    .map(|n| {
      let x = lerp(model.x_start, model.x_end, n as f32 / 1000.0);
      let y = (x * 4.0 / (model.x_end - model.x_start) + 100.0).sin() * 100.0 - 100.0;
      pt2(x, y)
    })
    .collect()
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
      // let diameter = ((factor * PI * 6.).sin() * 0.65 + 1.) * model.radius * (2. - factor);
      let diameter = (factor + 0.85).powf(factor + model.radius);
      Blob::new()
        .x_y(point.x, point.y)
        .w_h(diameter, diameter)
        .resolution(model.resolution)
        .seed(factor * model.seed_factor)
        .noise_scale(model.noise)
        .rotate_rad(factor * model.rotation)
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
          let factor = n as f32 / model.resolution as f32;
          let centerness = 1.0 - (0.5 - factor).abs() * 2.0;
          let alpha = lerp(0.001, 0.1, centerness);
          // maybe weird to instantiate LinSrgba this way, but hsla is so much easier to work with :shrug:
          let orange = LinSrgba::from(hsla(23. / 360., 0.96, 0.48, alpha));
          let pink = LinSrgba::from(hsla(256. / 360., 0.96, 0.56, i as f32 / blob_count * alpha));
          (blob[n as usize], orange.overlay(pink))
        })
        .collect()
    })
    .collect()
}

fn draw_bg(draw: &Draw, win: &Rect) {
  for i in 0..100 {
    let factor = i as f32 / 99.0;
    let dark_blue = hsl(276. / 360., 0.15, 0.01 + 0.05 * factor);
    draw
      .rect()
      .x_y(0.0, win.y.lerp(factor))
      .w_h(win.x.magnitude(), win.y.magnitude() / 99.0)
      .color(dark_blue);
  }
}

fn draw_smoke(lines: Vec<Vec<(Point2, LinSrgba)>>, draw: &Draw) {
  let length = lines[0].len();
  for (i, line) in lines.iter().enumerate() {
    let factor = i as f32 / length as f32;
    let centerness = 1.0 - (0.5 - factor).abs() * 2.0;
    let weight = lerp(0.05, 4., centerness);

    draw.polyline().weight(weight).points_colored(line.clone());
  }
}

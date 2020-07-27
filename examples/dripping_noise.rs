// The only really interesting thing about this sketch is the object avoidance
// algorithm on line 216 which uses an "anti gravity" concept.
// cargo run --release --example dripping_noise
// cargo run --release --example dripping_noise -- --nx 4 --ny 8 --line-length 600 --seed 530307.3930699833 --noise-scale 90.0 --stroke-weight 1.0 --padding 2.0 --resolution 100 --magic-number 8
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::noise::{NoiseFn, OpenSimplex};
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::blob::Blob;
use util::capture_model;
use util::captured_frame_path;
use util::grid;
use util::Line2;

fn main() {
  nannou::app(model).update(update).run();
}

#[derive(Debug)]
struct Model {
  // dimensions of blob grid
  nx: usize,
  ny: usize,
  // length of flow lines
  line_length: usize,
  // noise seed
  seed: f64,
  // noise "scale" - higher is longer wavelength
  noise_scale: f64,
  // line stroke weight
  stroke_weight: f32,
  // padding for object avoidance
  padding: f32,
  // number of "outer points" per blob
  resolution: i32,
  // blob to highlight
  magic_number: usize,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();

  app
    .new_window()
    .title(app.exe_name().unwrap())
    .view(view)
    .size(700, 1024)
    .build()
    .unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));

  Model {
    nx: args.get("nx", 4),
    ny: args.get("ny", 8),
    line_length: args.get("line-length", 400),
    noise_scale: args.get("noise-scale", random_range(30.0, 200.0)),
    seed: args.get("seed", random_range(1.0, 10.0.powi(7))),
    padding: args.get("padding", 2.0),
    stroke_weight: args.get("stroke-weight", 1.0),
    resolution: args.get("resolution", 100),
    magic_number: args.get("magic-number", 8),
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  let args = ArgParser::new();
  model.nx = args.get("nx", 4);
  model.ny = args.get("ny", 8);
  model.line_length = args.get("line-length", 400);
  model.noise_scale = args.get("noise-scale", random_range(30.0, 200.0));
  model.seed = args.get("seed", random_range(1.0, 10.0.powi(7)));
  model.padding = args.get("padding", 2.0);
  model.stroke_weight = args.get("stroke-weight", 1.0);
  model.resolution = args.get("resolution", 100);
  model.stroke_weight = args.get("stroke-weight", 1.0);
  model.magic_number = args.get("magic-number", model.ny % model.nx + model.nx);
}

fn view(app: &App, model: &Model, frame: Frame) {
  let win = app.window_rect();

  // Prepare to draw.
  let draw = app.draw();
  draw.background().color(WHITE);

  let blobs = gen_blobs(
    model,
    &Rect::from_corners(
      pt2(win.left() * 0.65, win.bottom() * 0.2),
      pt2(win.right() * 0.65, win.top() * 0.8),
    ),
  );

  for (i, blob) in blobs.iter().enumerate() {
    println!("drawing blob {} of {}", i + 1, model.nx * model.ny);
    let others: Vec<Point2> = blobs
      .iter()
      .enumerate()
      .filter(|(j, _b)| *j != i)
      .flat_map(|(_i, b)| b.points())
      .collect();
    for outer_point in blob.points() {
      let line = gen_line(
        &outer_point,
        &others,
        model.line_length,
        model.noise_scale,
        model.padding,
        model.seed,
      );
      draw_line(
        &draw,
        line,
        model.noise_scale,
        model.seed,
        model.stroke_weight,
        i == model.magic_number,
      );
    }
  }

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
  capture_model(app, &frame, model);
}

fn gen_blobs(model: &Model, bounds: &Rect) -> Vec<Blob> {
  let Model { nx, ny, .. } = *model;
  let noise = OpenSimplex::new();
  let w_h = 20.;
  let index_noise = |i, j| noise.get([i as f64, j as f64, model.seed]);
  let x_offset = |i, j| {
    map_range(
      index_noise(i, j),
      -1.,
      1.,
      -bounds.w() / nx as f32,
      bounds.w() / nx as f32,
    )
  };
  let y_offset = |i, j| {
    map_range(
      index_noise(i, j),
      -1.,
      1.,
      -bounds.h() / ny as f32,
      bounds.h() / ny as f32,
    )
  };
  grid(nx, ny)
    .map(|(i, j)| {
      let x = map_range(i, 0, nx - 1, bounds.left(), bounds.right()) + x_offset(i, j);
      let y = map_range(j, 0, ny - 1, bounds.bottom(), bounds.top()) + y_offset(i, j);
      Blob::new()
        .x_y(x, y)
        .w_h(w_h, w_h)
        .noise_scale(0.8)
        .seed(random_range(1., 10000.))
        .resolution(model.resolution)
    })
    .collect()
}

fn gen_line(
  point: &Point2,
  others: &Vec<Point2>,
  length: usize,
  noise_scale: f64,
  padding: f32,
  seed: f64,
) -> Line2 {
  let mut x = point.x;
  let mut y = point.y;
  let noise = OpenSimplex::new();
  (0..length)
    .map(|_i| {
      let new_point = next_point(x, y, noise, noise_scale, seed, padding, others);
      x = new_point.x;
      y = new_point.y;
      new_point
    })
    .collect()
}

fn next_point(
  x: f32,
  y: f32,
  noise: impl NoiseFn<[f64; 3]>,
  noise_scale: f64,
  seed: f64,
  padding: f32,
  others: &Vec<Point2>,
) -> Point2 {
  let val = map_range(
    noise.get([x as f64 / noise_scale, y as f64 / noise_scale, seed]),
    -1.,
    1.,
    PI / 2., // range from [π/2,2π + π/2) so that the "average" direction is down (noise usually averages to 0)
    PI * 5. / 2.,
  );
  let mut new_x = x + val.cos();
  let mut new_y = y + val.sin();
  let new_point = pt2(new_x, new_y);

  // object avoidance using "anti-gravity"
  // https://www.ibm.com/developerworks/java/library/j-antigrav/
  for point in others {
    let distance = point.distance(new_point);
    // This really breaks down with values lower than 2 in the pow function.
    // Not sure why, I'd like to shape this more though
    let force = padding / distance.powi(2);
    let diff = new_point - *point;
    let angle = diff.y.atan2(diff.x);
    new_x += angle.cos() * force;
    new_y += angle.sin() * force;
  }

  pt2(new_x, new_y)
}

fn draw_line(
  draw: &Draw,
  line: Line2,
  noise_scale: f64,
  seed: f64,
  stroke_weight: f32,
  highlight: bool,
) {
  let hard_way = false;
  if hard_way {
    // hard way -> adjust lightness and alpha for each line segment
    let noise = OpenSimplex::new();
    for (i, point) in line.iter().enumerate() {
      if i == 0 {
        continue;
      }
      let i_noise = noise.get([i as f64 / noise_scale, seed]);
      let lightness = map_range(i_noise, -1., 1., 0.0, 0.2);
      let alpha = map_range(i_noise, -1., 1., 0.8, 1.0);
      draw
        .polyline()
        .caps_round()
        .color(hsla(0., 0., lightness, alpha))
        .stroke_weight(stroke_weight)
        .points(vec![line[i - 1], *point]);
    }
  } else {
    // easy way -> just draw a black line
    let color = if highlight {
      hsla(2. / 360., 0.91, 0.38, 0.6)
    } else {
      hsla(0.0, 0.0, 0.0, 0.5)
    };
    draw
      .polyline()
      .caps_round()
      .color(color)
      .stroke_weight(stroke_weight)
      .points(line);
  }
}

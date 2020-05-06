// oh, right, this is basically just voronoi
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::{captured_frame_path, oversample, smooth_by, Line2};

const HEIGHT: u32 = 1024;
const WIDTH: u32 = 1024;

// ExpandoBlob is an arbitrary closed shape whose points expand until they hit another point.
// The `id` field is important to avoid checking for collisions with itself
#[derive(Debug, Clone)]
struct ExpandoBlob {
  id: u64,
  points: Vec<Point2>,
  origin: Point2,
}

struct Model {
  loops: usize,
  animation_rate: u64,
  animate: bool,
  blobs: Vec<ExpandoBlob>,
  resolution: i32,
  padding: f32,
}

fn main() {
  nannou::app(model).update(update).run();
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  let loops = args.get_usize("loops", 200);

  app
    .new_window()
    .title(app.exe_name().unwrap())
    .view(view)
    .size(WIDTH, HEIGHT)
    .build()
    .unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(loops));
  let n_blobs = args.get("blobs", random_range(15, 30));
  let resolution = args.get("resolution", 300);
  let padding = args.get("padding", 5.0);
  let win = app.window_rect();

  Model {
    loops,
    resolution,
    animate: args.get("animate", true),
    animation_rate: args.get("rate", 10),
    blobs: build_blobs(n_blobs, resolution, padding, &win),
    padding,
  }
}

fn build_blobs(n_blobs: usize, resolution: i32, padding: f32, win: &Rect) -> Vec<ExpandoBlob> {
  // Mutable vectors are the easiest way to build this,
  // since we want to modify the inputs (points) based on the existing elements in the vector
  let radius = 5.0;
  let mut points: Vec<Point2> = Vec::with_capacity(n_blobs);
  while points.len() < n_blobs {
    // arrange the point randomly
    let mut x = random_range(win.left(), win.right());
    let mut y = random_range(win.bottom(), win.top());

    // if the point intersects another point when it is initiated, try moving it so it does not intersect
    while points
      .iter()
      .any(|pt| pt.distance(pt2(x, y)) < radius * 2.0 + padding)
    {
      x = random_range(win.left(), win.right());
      y = random_range(win.bottom(), win.top());
    }

    points.push(pt2(x, y));
  }

  // Once we have our base points, create the ExpandoBlobs
  points
    .iter()
    .map(|pt| ExpandoBlob {
      id: random_range(100000, 10000000),
      points: (0..resolution)
        .map(|n| {
          let angle = map_range(n, 0, resolution - 1, 0.0, 2.0 * PI);
          pt2(angle.cos() * radius + pt.x, angle.sin() * radius + pt.y)
        })
        .collect(),
      origin: *pt,
    })
    .collect()
}

// each update, expand the points in the blobs until they hit another point
fn update(_app: &App, model: &mut Model, _update: Update) {
  // let Model { blobs, resolution, .. } = model;

  model.blobs = model
    .blobs
    .iter()
    .map(|blob| ExpandoBlob {
      id: blob.id,
      origin: blob.origin,
      points: blob
        .points
        .clone()
        .iter()
        .enumerate()
        .map(|(i, pt)| {
          // find new point by expanding outwards by 1
          let angle = map_range(i, 0, model.resolution as usize - 1, 0.0, 2.0 * PI);
          let new_pt = pt2(pt.x + angle.cos(), pt.y + angle.sin());
          // check intersections with all points in all other blobs.
          // Ignore current blob, so that closely-spaced adjacent points don't cause failure
          // if intersection, do not move, else move
          if model
            .blobs
            .clone()
            .iter()
            .filter(|b| b.id != blob.id)
            .flat_map(|b| b.points.clone())
            .any(|p| p.distance(new_pt) < model.padding)
          {
            pt.clone()
          } else {
            new_pt
          }
        })
        .collect(),
    })
    .collect();
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  draw.background().color(hsl(0., 0., 0.04));

  // draw every `rate` frames if animating
  println!("{}", frame.nth()); // kind of nice to know things are running smoothly if it isn't animating
  if model.animate && frame.nth() > 0 && frame.nth() % model.animation_rate == 0 {
    draw_blobs(&draw, model);
    draw.to_frame(app, &frame).unwrap();
    // app
    //   .main_window()
    //   .capture_frame(captured_frame_path(app, &frame));
  }

  // draw final frame
  if frame.nth() == model.loops as u64 - 1 {
    draw_blobs(&draw, model);
    draw.to_frame(app, &frame).unwrap();
    app
      .main_window()
      .capture_frame(captured_frame_path(app, &frame));
  }
}

/// draw the points cluster
fn draw_blobs(draw: &Draw, model: &Model) {
  let n_blobs = model.blobs.len();
  for (i, blob) in model.blobs.iter().enumerate() {
    let hue = map_range(i, 0, n_blobs, 260.0, 285.0) / 360.;
    let lum = map_range(i, 0, n_blobs, 0.20, 0.39);
    // hmm, might be a bug in oversample, this isn't working right at all
    // let points = smooth_by(20, &oversample(3, &blob.points));
    let points = smooth_by(20, &blob.points);
    for n in 0..20 {
      let line = shrink_points(&points, blob.origin, n, model.resolution);
      draw
        .polygon()
        .color(hsl(hue, 0.5, lum))
        .stroke(hsl(hue, 0.5, map_range(n, 0, 9, lum * 1.2, lum * 1.5)))
        .points(line);
    }
  }
}

fn shrink_points(points: &Line2, origin: Point2, n: i32, resolution: i32) -> Line2 {
  let start_angle = ((points[0].y - origin.y) / (points[0].x - origin.x)).atan();
  let new_points: Vec<Point2> = points
    .iter()
    .enumerate()
    .flat_map(|(i, pt)| {
      if i == points.len() - 1 || i % 2 == 0 {
        return vec![];
      }
      // get data for adjustment
      let start = pt;
      let end = points[i as usize + 1];
      let distance = start.distance(origin);
      let orientation = start_angle + map_range(i, 0, resolution as usize - 1, 0.0, 2.0 * PI);
      let spread = if n as f32 * 5.0 > distance {
        distance
      } else {
        n as f32 * 5.0
      };

      // adjust start and end
      let start_x = start.x - orientation.cos() * spread;
      let start_y = start.y - orientation.sin() * spread;
      let end_x = end.x - orientation.cos() * spread;
      let end_y = end.y - orientation.sin() * spread;

      vec![pt2(start_x, start_y), pt2(end_x, end_y)]
    })
    .collect();

  new_points
}

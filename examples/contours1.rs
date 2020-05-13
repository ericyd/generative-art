// An implementation of Bruce Hill's "Meandering Triangles" contour algorithm.
// For my purposes, it looks great and is very fast!
// https://blog.bruce-hill.com/meandering-triangles
//
// cargo run --release --example contours1
//
// I don't know why but I've always loved the look of contour plots.
// There is something about them that just looks magical to me.
// In this implementation, the "surface" is a noise function,
// and the seed will regenerate on every loop, which means you can quickly see different
// surfaces mapped just by setting a higher loop count, e.g.
//
// cargo run --release --example contours1 -- --loops 5
//
// Algorithm in a nutshell (note: please see link above for thorough explanation!)
// 1. Create a grid of nx by ny points, scaled between the window bounds.
// 2. Triangulate the points with the Delaunator library
//    Note: there might be a faster way since our grid is regular,
//    but Delaunator is certainly the easiest way since I've used it before
// 3. Map the triangulated 2D points to 3D using a noise function for the elevation.
// 4. Draw contours by iterating through each triangle of 3D points:
//  4a. For each vertex in the triangle, test if it is above or below the target contour level ("threshold")
//  4b. If all points are above or below, skip that triangle
//  4c. If any points cross the threshold, draw a line between the two edges that cross
//  4d. Concatenate all the "crossing points" and you should have yourself a contour plot!
// Note that I am leaving out a portion of the algorithm where you connect your contour
// points into full lines. For my purpose it was unecessary so I omitted it.
//
// Note: this example is kind of noisy (logging), I added a bunch of print statements
// to debug a performance bottleneck, and then didn't feel like removing them.
extern crate chrono;
extern crate delaunator;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::noise::{NoiseFn, Perlin, Turbulence};
use nannou::prelude::*;

use delaunator::{triangulate, Point};

mod util;
use util::args::ArgParser;
use util::{formatted_frame_path, point_cloud, Line2, PointCloud};

fn main() {
  nannou::app(model).update(update).run();
}

struct Model {
  // how many lines to draw
  nx: usize,
  // resolution of the lines
  ny: usize,
  // noise seed
  seed: f64,
  // controls "frequency" of noise "waves"
  noise_scale: f64,
  // maximum "height" of points
  z_scale: f32,
  // Number of contour thresholds to draw
  n_contours: usize,
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
    nx: args.get("nx", 250),
    ny: args.get("ny", 250),
    seed: args.get("seed", random_range(1.0, 100000.0)),
    noise_scale: args.get("noise_scale", 600.0),
    z_scale: args.get("z-scale", 350.0),
    n_contours: args.get("contours", 30),
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  let args = ArgParser::new();
  model.seed = args.get("seed", random_range(1.0, 100000.0));
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  draw.background().color(Rgb::<u8>::new(247, 206, 94));
  let win = app.window_rect();

  println!(
    "Creating point cloud for {} x {} = {} points ...",
    model.nx,
    model.ny,
    model.nx * model.ny
  );
  let grid = point_cloud(
    model.nx,
    model.ny,
    win.left(),
    win.right(),
    win.bottom(),
    win.top(),
  );

  let triangles = calc_triangles(model, grid);

  for n in 0..model.n_contours {
    let threshold = map_range(
      n,
      0,
      model.n_contours - 1,
      model.z_scale * 0.1,
      model.z_scale * 0.9,
    );
    println!("drawing contour for {} threshold", threshold);
    let contour = calc_contour(threshold, &triangles);
    draw_contours(&draw, model, &win, contour);
  }

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app.main_window().capture_frame(formatted_frame_path(
    app,
    &frame,
    format!("seed-{}", model.seed),
  ));
}

struct Triangle3D {
  vertices: Vec<Point3>,
}

impl Triangle3D {
  fn contour_line(&self, threshold: f32) -> Option<Line2> {
    // If all points are above or all are below, then there are no intersections
    let below: Vec<Point3> = self
      .vertices
      .iter()
      .filter(|v| v.z < threshold)
      .cloned()
      .collect();
    let above: Vec<Point3> = self
      .vertices
      .iter()
      .filter(|v| v.z >= threshold)
      .cloned()
      .collect();

    // no intersections
    if above.is_empty() || below.is_empty() {
      return None;
    }

    // We have a contour line, let's find it
    let minority = if above.len() < below.len() {
      above.clone()
    } else {
      below.clone()
    };
    let majority = if above.len() > below.len() {
      above.clone()
    } else {
      below.clone()
    };

    let mut contour_points = vec![];
    let crossed_edges = vec![(minority[0], majority[0]), (minority[0], majority[1])];
    for (vertex1, vertex2) in crossed_edges {
      // the percentage of the distance along the edge at which the point crosses
      let how_far = (threshold - vertex2.z) / (vertex1.z - vertex2.z);
      let crossing_point = pt2(
        how_far * vertex1.x + (1.0 - how_far) * vertex2.x,
        how_far * vertex1.y + (1.0 - how_far) * vertex2.y,
      );
      contour_points.push(crossing_point);
    }
    Some(contour_points)
  }
}

// Returning a closure for the elevation function rather than
// calculating it on the fly every time makes this approx. 1 billion times faster,
// presumably because it doesn't have to instantiate the noisefn every time
fn elevation(seed: f64, noise_scale: f64, z_scale: f32) -> impl Fn(f64, f64) -> f32 {
  let source = Perlin::new();
  let noisefn = Turbulence::new(source);

  move |x, y| {
    let noise = noisefn.get([x / noise_scale, y / noise_scale, seed]) as f32;
    map_range(noise, -1.0, 1.0, 0.0, z_scale)
  }
}

fn calc_triangles(model: &Model, grid: PointCloud) -> Vec<Triangle3D> {
  println!("Triangulating ...");
  let points: Vec<Point> = grid
    .iter()
    .map(|p| Point {
      x: p.x as f64,
      y: p.y as f64,
    })
    .collect();

  let delauney = triangulate(&points).expect("No triangulation exists.");

  // delauney.triangles is flat array,
  // where every 3 values represents the indices of the points of a triangle
  println!("Mapping to Triangle3D ...");
  let elevation_fn = elevation(model.seed, model.noise_scale, model.z_scale);
  (0..delauney.triangles.len() / 3)
    .map(|n| {
      let vertices: Vec<Point3> = (3 * n..=3 * n + 2) // indices of delauney.triangles
        .map(|i| delauney.triangles[i]) // indices of points
        .map(|p| points[p].clone()) // the actual points
        .map(|p| {
          // 3D point
          pt3(p.x as f32, p.y as f32, elevation_fn(p.x, p.y))
        })
        .collect();
      Triangle3D { vertices }
    })
    .collect()
}

fn calc_contour(threshold: f32, triangles: &Vec<Triangle3D>) -> Vec<Line2> {
  triangles
    .iter()
    .map(|t| t.contour_line(threshold))
    .filter_map(|o| o)
    .collect()
}

fn draw_contours(draw: &Draw, _model: &Model, _win: &Rect, contours: Vec<Line2>) {
  let color = Rgb::<u8>::new(7, 86, 143);
  for line in contours {
    // each line is just 2 points, even though that isn't necessarily obvious
    draw.line().color(color).start(line[0]).end(line[1]);
  }
}

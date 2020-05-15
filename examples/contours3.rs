// Identical to contours2 but this version connects the contiguous lines.
// This allows us to make polygons out of our contours!
// Turns out this is harder than it seems. See `connect_contour_segments` method
// for more details.
//
// This is an implementation of Bruce Hill's "Meandering Triangles" contour algorithm:
// https://blog.bruce-hill.com/meandering-triangles
//
// To make closed polygons, run with --closed true, e.g.
// cargo run --release --example contours3 -- --closed true
// To have normal contours, just run --closed false (or omit because this is the default)
// cargo run --release --example contours3 -- --closed false
// note that polygons takes WAY longer because the "connecting segments" part
// is by far the most intensive part of the algorithm, and it gets skipped if we aren't drawing polygons.
extern crate chrono;
extern crate delaunator;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::noise::{Fbm, MultiFractal, NoiseFn, Turbulence};
use nannou::prelude::*;

use delaunator::{triangulate, Point};

mod util;
use util::args::ArgParser;
use util::{capture_model, captured_frame_path, point_cloud, Line2, PointCloud};

fn main() {
  nannou::app(model).update(update).run();
}

#[derive(Debug)]
struct Model {
  // how grid points on the x axis
  nx: usize,
  // how grid points on the y axis
  ny: usize,
  // noise seed
  seed: f64,
  // controls "frequency" of noise "waves"
  noise_scale: f64,
  // maximum "height" of points
  z_scale: f32,
  // Number of contour thresholds to draw
  n_contours: usize,
  // the min/max percent of the z_scale at which to draw contours.
  // Both values should be in the [0.0,1.0] range
  min_contour: f32,
  max_contour: f32,
  // These all relate to the "MultiFractal" trait in the noise module
  octaves: usize,
  frequency: f64,
  lacunarity: f64,
  persistence: f64,
  // when true, draw polygons. Else draw lines
  closed: bool,
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
    nx: args.get("nx", 50),
    ny: args.get("ny", 50),
    seed: args.get("seed", random_range(1.0, 100000.0)),
    noise_scale: args.get("noise_scale", 600.0),
    z_scale: args.get("z-scale", 350.0),
    n_contours: args.get("contours", 70),
    min_contour: args.get("min-contour", 0.01),
    max_contour: args.get("max-contour", 0.99),
    // These are the defaults from the source for Fbm struct
    // pub const DEFAULT_OCTAVE_COUNT: usize = 6;
    // pub const DEFAULT_FREQUENCY: f64 = 1.0;
    // pub const DEFAULT_LACUNARITY: f64 = std::f64::consts::PI * 2.0 / 3.0;
    // pub const DEFAULT_PERSISTENCE: f64 = 0.5;
    // pub const MAX_OCTAVES: usize = 32;
    //
    // Notes to self
    // low persistence w/ low lacunarity = longer "wavelength"
    octaves: args.get("octaves", 2),
    frequency: args.get("frequency", 1.45),
    lacunarity: args.get("lacunarity", std::f64::consts::PI),
    persistence: args.get("persistence", 0.78),
    closed: args.get("closed", false),
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  let args = ArgParser::new();
  model.seed = args.get("seed", random_range(1.0, 100000.0));
  model.octaves = args.get("octaves", random_range(2, 4));
  model.frequency = args.get("frequency", random_range(0.2, 1.5));
  model.lacunarity = args.get("lacunarity", random_range(1.0, 4.0));
  model.persistence = args.get("persistence", random_range(0.2, 1.2));
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  draw.background().color(WHITESMOKE);
  let win = app.window_rect();

  println!(
    "Creating point cloud for {} x {} = {} points ...",
    model.nx,
    model.ny,
    model.nx * model.ny
  );
  // define a window extent larger than the actual window,
  // to increase the odds that contours will result in closed polygons
  let grid = point_cloud(
    model.nx,
    model.ny,
    win.left() * 2.0,
    win.right() * 2.0,
    win.bottom() * 2.0,
    win.top() * 2.0,
  );

  let triangles = calc_triangles(model, grid);

  // draw from highest to lowest contour
  for n in 0..model.n_contours {
    let threshold = map_range(
      n,
      0,
      model.n_contours - 1,
      model.z_scale * model.max_contour,
      model.z_scale * model.min_contour,
    );
    println!("drawing contour for {} threshold", threshold);
    let contour_segments = calc_contour(threshold, &triangles);
    // if we don't care about polygons, we can same the computer power of calculating them
    if model.closed {
      let contour = connect_contour_segments(contour_segments);
      draw_contour(&draw, model, n, contour);
    } else {
      draw_contour(&draw, model, n, contour_segments);
    }
  }

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
  capture_model(app, &frame, model);
}

/// A simple container for a Triangle that exists in 3 dimensions.
/// `vertices` should only have 3 points - perhaps will make that explicit in the future.
struct Triangle3D {
  vertices: Vec<Point3>,
}

impl Triangle3D {
  /// Returns a optional line segment that traces the contour path across the triangle
  /// for the given threshold (elevation).
  /// If no line exists across this triangle for the given threshold,
  /// return None.
  /// If a line exists, return Some(line)
  /// where `line` is two points, each sitting on one edge of the triangle.
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

/// Returns a closure that calculates the elevation for a given (x,y) coordinate.
/// The elevation is based on a noise function which is modulated by the
/// model.seed and model.noise_scale parameters.
/// The height of the resulting "topography" ranges from 0.0 to model.z_scale
fn elevation_fn(model: &Model) -> impl Fn(f64, f64) -> f32 {
  // need to extract these values so they can be moved into the closure
  let Model {
    noise_scale,
    z_scale,
    seed,
    ..
  } = *model;
  // Fractal brownian motion looks real cool here! And is super configurable
  let source = Fbm::new()
    .set_octaves(model.octaves)
    .set_frequency(model.frequency)
    .set_lacunarity(model.lacunarity)
    .set_persistence(model.persistence);
  let noisefn = Turbulence::new(source);

  move |x, y| {
    let noise = noisefn.get([x / noise_scale, y / noise_scale, seed]) as f32;
    map_range(noise, -1.0, 1.0, 0.0, z_scale)
  }
}

/// Calculates the Delauney triangulation for the given PointCloud,
/// then maps the 2D points to 3D using an elevation function for the z coodinate.
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
  let elevation = elevation_fn(model);
  (0..delauney.triangles.len() / 3)
    .map(|n| {
      let vertices: Vec<Point3> = (3 * n..=3 * n + 2) // indices of delauney.triangles
        .map(|i| delauney.triangles[i]) // indices of points
        .map(|p| points[p].clone()) // the actual points
        .map(|p| {
          // 3D point
          pt3(p.x as f32, p.y as f32, elevation(p.x, p.y))
        })
        .collect();
      Triangle3D { vertices }
    })
    .collect()
}

/// Returns a set of lines that describe a single contour line for our set of triangles.
/// The contour is _not_ contiguous, it is a disjointed set of short lines that bisect individual triangles.
fn calc_contour(threshold: f32, triangles: &Vec<Triangle3D>) -> Vec<Line2> {
  triangles
    .iter()
    .map(|t| t.contour_line(threshold))
    .filter_map(|o| o)
    .collect()
}

fn close_enough(p1: &Point2, p2: &Point2) -> bool {
  let threshold = 0.1;
  p1.distance(*p2) < threshold
}

/// Kind of a tricky thing, building contiguous lines from a collection of segments!
/// This algorithm takes a mutable collection of segments, and iteratively builds
/// contiguous contour lines by
/// 1. popping a random segment from the stack
/// 2. finding all segments that could connect to the "head" of the line
/// 3. finding all segments that could connect to the "end" of the line
/// 4. concatenating the relevant points to the line
/// 5. breaking once no head or tail points are found.
fn connect_contour_segments(mut segments: Vec<Line2>) -> Vec<Line2> {
  let mut contour_lines = vec![];

  'outer: loop {
    // grab random segment
    match segments.pop() {
      None => break 'outer,
      Some(mut line) => {
        'inner: loop {
          // find segments that join at the head of the line.
          // if we find a point that matches, we must take the other point from the segment, so
          // we continue to build a line moving forwards with no duplicate points.
          let cloned_segments = segments.clone();
          let mut head_segments: Vec<(usize, &Line2)> = cloned_segments
            .iter()
            .enumerate()
            .filter(|(_i, seg)| {
              close_enough(line.last().unwrap(), &seg[0])
                || close_enough(line.last().unwrap(), &seg[1])
            })
            .collect();
          let no_head_segments = head_segments.is_empty();
          match head_segments.pop() {
            None => (),
            Some((index, new_seg)) => {
              if close_enough(&new_seg[0], line.last().unwrap()) {
                line.push(new_seg[1]);
              } else {
                line.push(new_seg[0]);
              }
              segments.remove(index);
            }
          }

          // find segments that join at the tail of the line
          // if we find a point that matches, we must take the other point from the segment
          // and insert it at the beginning of the line
          let cloned_segments = segments.clone();
          let mut tail_segments: Vec<(usize, &Line2)> = cloned_segments
            .iter()
            .enumerate()
            .filter(|(_i, seg)| close_enough(&line[0], &seg[0]) || close_enough(&line[0], &seg[1]))
            .collect();
          let no_tail_segments = tail_segments.is_empty();
          match tail_segments.pop() {
            None => (),
            Some((index, new_seg)) => {
              if close_enough(&new_seg[0], &line[0]) {
                line.insert(0, new_seg[1]);
              } else {
                line.insert(0, new_seg[0]);
              }
              segments.remove(index);
            }
          }

          // when no matching segments were found, push the line and start again!
          // Otherwise, iterate
          if no_head_segments && no_tail_segments {
            contour_lines.push(line);
            break 'inner;
          }
        }
      }
    }
  }

  contour_lines
}

// Draw all the lines for the contour.
// In contours1 and contours2, the `contour` vector contained a ton of 2-point segments.
// In this implementation, it contains full "connected" contours. However, any given
// contour can still have multiple non-contiguous segments, even if they are all closed.
// Hence, we still have a Vec<Line2>, but each Line2 has a very different meaning.
// In retrospect, I probably should have given the contour_segments a specific data type
// in contour1 and contour2, but y'know you live and learn
fn draw_contour(draw: &Draw, model: &Model, n: usize, contour: Vec<Line2>) {
  let hue = map_range(n, 0, model.n_contours - 1, 180.0, 254.0) / 360.0;
  let color = hsla(hue, 0.55, 0.52, 0.2);
  let stroke = hsla(hue, 0.45, 0.32, 1.0);
  for line in contour {
    // if the line is not closed (think a contour along a valley through a map),
    // then we need to adjust the polygon so that it has additional points and
    // appears to be a closed shape.
    if model.closed {
      draw
        .polygon()
        .color(color)
        .stroke_weight(1.0)
        .stroke(stroke)
        .points(line);
    } else {
      draw.polyline().color(stroke).weight(1.0).points(line);
    }
  }
}

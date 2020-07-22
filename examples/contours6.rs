// New idea: instead of trying to join polygons,
// why not just color the elevation map and then draw contours directly?
//
// This is an implementation of Bruce Hill's "Meandering Triangles" contour algorithm:
// https://blog.bruce-hill.com/meandering-triangles
//
// cargo run --release --example contours6
extern crate chrono;
extern crate delaunator;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::noise::{Fbm, MultiFractal, NoiseFn};
use nannou::prelude::*;

use std::collections::VecDeque;

use delaunator::{triangulate, Point};

mod util;
use util::args::ArgParser;
use util::{capture_model, captured_frame_path, point_cloud, PointCloud};

type Deque2 = VecDeque<Point2>;

fn main() {
  nannou::app(model).update(update).run();
}

#[derive(Debug)]
struct Model {
  // how grid points on each the x axis and y axis
  grid: usize,
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
    grid: args.get("grid", 100),
    seed: args.get("seed", random_range(1.0, 100000.0)),
    noise_scale: args.get("noise_scale", 800.0),
    z_scale: args.get("z-scale", 350.0),
    n_contours: args.get("n-contours", 70),
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
  model.octaves = args.get("octaves", 8);
  model.frequency = args.get("frequency", random_range(1.2, 1.8));
  model.lacunarity = args.get("lacunarity", random_range(2.0, 2.6));
  model.persistence = args.get("persistence", random_range(0.28, 0.38));
  model.n_contours = args.get("n-contours", random_range(150, 250));
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  draw.background().color(WHITE);
  let win = app.window_rect();

  println!(
    "Creating point cloud for {} x {} = {} points ...",
    model.grid,
    model.grid,
    model.grid * model.grid
  );
  // define a window extent larger than the actual window,
  // to increase the odds that contours will result in closed polygons
  let grid = point_cloud(
    model.grid,
    model.grid,
    win.left() * 1.25,
    win.right() * 1.25,
    win.bottom() * 1.25,
    win.top() * 1.25,
  );

  let triangles = calc_triangles(model, grid);

  draw_elevation_map(&draw, model, &win);

  // draw from highest to lowest contour
  for n in 0..model.n_contours {
    let threshold = map_range(
      n,
      0,
      model.n_contours - 1,
      model.z_scale * model.max_contour,
      model.z_scale * model.min_contour,
    );
    println!(
      "drawing contour {} of {} ({} threshold)",
      n + 1,
      model.n_contours,
      threshold
    );
    let contour_segments = calc_contour(threshold, &triangles);
    // if we don't care about polygons, we can save the computer power of calculating them
    if model.closed {
      draw_contour_polygons(&draw, model, n, contour_segments, &win);
    } else {
      draw_contour_lines(&draw, model, n, contour_segments);
    }
  }

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
  capture_model(app, &frame, model);
}

fn draw_elevation_map(draw: &Draw, model: &Model, win: &Rect) {
  println!("drawing elevation map...");
  let elevation_map = elevation_fn(model);

  for _x in 0..=win.w() as i32 {
    let x = map_range(_x, 0, win.w() as i32, win.left(), win.right());
    for _y in 0..=win.h() as i32 {
      let y = map_range(_y, 0, win.h() as i32, win.bottom(), win.top());

      let elevation = elevation_map(x.into(), y.into());
      let hue = if elevation < model.z_scale / 2. {
        0.07
      } else {
        0.59
      };
      let color = hsla(
        hue,
        0.5,
        map_range(elevation, 0., model.z_scale, 0.3, 0.8),
        1.,
      );
      draw.rect().color(color).x_y(x, y).w_h(1.0, 1.0);
    }
  }
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
  fn contour_line(&self, threshold: f32) -> Option<Deque2> {
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

    let mut contour_points = VecDeque::with_capacity(2);
    let crossed_edges = vec![(minority[0], majority[0]), (minority[0], majority[1])];
    for (vertex1, vertex2) in crossed_edges {
      // the percentage of the distance along the edge at which the point crosses
      let how_far = (threshold - vertex2.z) / (vertex1.z - vertex2.z);
      let crossing_point = pt2(
        how_far * vertex1.x + (1.0 - how_far) * vertex2.x,
        how_far * vertex1.y + (1.0 - how_far) * vertex2.y,
      );
      contour_points.push_back(crossing_point);
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
  let noisefn = Fbm::new()
    .set_octaves(model.octaves)
    .set_frequency(model.frequency)
    .set_lacunarity(model.lacunarity)
    .set_persistence(model.persistence);

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
fn calc_contour(threshold: f32, triangles: &Vec<Triangle3D>) -> Vec<Deque2> {
  triangles
    .iter()
    .map(|t| t.contour_line(threshold))
    .filter_map(|o| o)
    .collect()
}

fn is_near(p1: &Point2, p2: &Point2) -> bool {
  p1.distance(*p2) < 1.
}

fn is_somewhat_near(p1: &Point2, p2: &Point2) -> bool {
  p1.distance(*p2) < 20.0
}

/// Kind of a tricky thing, building contiguous lines from a collection of segments!
/// This algorithm takes a mutable collection of segments, and iteratively builds
/// contiguous contour lines by
/// 1. popping a random segment from the stack
/// 2. finding all segments that could connect to the "head" of the line
/// 3. finding all segments that could connect to the "end" of the line
/// 4. concatenating the relevant points to the line
/// 5. breaking once no head or tail points are found.
///
/// The result is a collection of lines, each at the same threshold.
/// The reason it is a collection of lines is that each threshold could
/// theoretically exist as a closed line in multiple places on the topography,
/// e.g. in localized peaks and valleys.
///
/// I don't believe this is a perfect script,
/// it sometimes results in unclosed lines that should be closed
fn connect_contour_segments(mut segments: Vec<Deque2>) -> Vec<Deque2> {
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
          let mut head_segments: Vec<(usize, &Deque2)> = cloned_segments
            .iter()
            .enumerate()
            .filter(|(_i, seg)| {
              is_near(line.back().unwrap(), &seg[0]) || is_near(line.back().unwrap(), &seg[1])
            })
            .collect();
          let no_head_segments = head_segments.is_empty();
          match head_segments.pop() {
            None => (),
            Some((index, new_seg)) => {
              if is_near(&new_seg[0], line.back().unwrap()) {
                line.push_back(new_seg[0]);
                line.push_back(new_seg[1]);
              } else {
                line.push_back(new_seg[1]);
                line.push_back(new_seg[0]);
              }
              segments.remove(index);
            }
          }

          // find segments that join at the tail of the line
          // if we find a point that matches, we must take the other point from the segment
          // and insert it at the beginning of the line
          let cloned_segments = segments.clone();
          let mut tail_segments: Vec<(usize, &Deque2)> = cloned_segments
            .iter()
            .enumerate()
            .filter(|(_i, seg)| is_near(&line[0], &seg[0]) || is_near(&line[0], &seg[1]))
            .collect();
          let no_tail_segments = tail_segments.is_empty();
          match tail_segments.pop() {
            None => (),
            Some((index, new_seg)) => {
              if is_near(&new_seg[0], &line[0]) {
                line.push_front(new_seg[0]);
                line.push_front(new_seg[1]);
              } else {
                line.push_front(new_seg[1]);
                line.push_front(new_seg[0]);
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

fn has_terminal_on_sides(front: &Point2, back: &Point2, win: &Rect) -> bool {
  (front.x < win.left() && back.x > win.right()) || (back.x < win.left() && front.x > win.right())
}

fn should_go_left(front: &Point2, back: &Point2) -> bool {
  (front.x + back.x) / 2. < 0.
}

fn should_go_down(front: &Point2, back: &Point2) -> bool {
  (front.y + back.y) / 2. < 0.
}

// if the polygon doesn't connect, that should indicate that the terminals are off screen.
// We can make sure the connections for the polygon don't slice through the image
// by adding fake terminals that are substantially out of view.
// If terminals are on opposite sides of map, draw up/down
// else, draw left or right
fn get_extended_terminals(front: &Point2, back: &Point2, win: &Rect) -> (Point2, Point2) {
  if has_terminal_on_sides(front, back, win) {
    if should_go_down(front, back) {
      (
        pt2(front.x, front.y - win.h()),
        pt2(back.x, back.y - win.h()),
      )
    } else {
      (
        pt2(front.x, front.y + win.h()),
        pt2(back.x, back.y + win.h()),
      )
    }
  } else if should_go_left(front, back) {
    (
      pt2(front.x - win.w(), front.y),
      pt2(back.x - win.w(), back.y),
    )
  } else {
    (
      pt2(front.x + win.w(), front.y),
      pt2(back.x + win.w(), back.y),
    )
  }
}

fn terminals_are_in_view(front: &Point2, back: &Point2, win: &Rect) -> bool {
  win.left() < front.x
    && front.x < win.right()
    && win.left() < back.x
    && back.x < win.right()
    && win.bottom() < front.y
    && front.y < win.top()
    && win.bottom() < back.y
    && back.y < win.top()
}

fn terminals_are_out_of_view(front: &Point2, back: &Point2, win: &Rect) -> bool {
  (win.left() > front.x || front.x > win.right() || win.bottom() > front.y || front.y > win.top())
    && (win.left() > back.x || back.x > win.right() || win.bottom() > back.y || back.y > win.top())
}

fn should_show(front: &Point2, back: &Point2, win: &Rect) -> bool {
  terminals_are_in_view(front, back, win) || terminals_are_out_of_view(front, back, win)
}

// Draw all the lines for the contour.
// In contours1 and contours2, the `contour` vector contained a ton of 2-point segments.
// In this implementation, it contains full "connected" contours. However, any given
// contour can still have multiple non-contiguous segments, even if they are all closed.
// Hence, we still have a Vec<Line2>, but each Line2 has a very different meaning.
// In retrospect, I probably should have given the contour_segments a specific data type
// in contour1 and contour2, but y'know you live and learn
fn draw_contour_polygons(
  draw: &Draw,
  model: &Model,
  n: usize,
  contour_segments: Vec<Deque2>,
  win: &Rect,
) {
  let contour = connect_contour_segments(contour_segments);
  let hue = if n < model.n_contours / 2 { 0.07 } else { 0.59 };
  let color = hsla(
    hue,
    0.5,
    map_range(n, 0, model.n_contours - 1, 0.3, 0.8),
    0.1,
  );
  let stroke = hsla(hue, 0.4, 0.05, 1.);
  for mut line in contour.clone() {
    if !line.is_empty() {
      let front = line.front().unwrap().clone();
      let back = line.back().unwrap().clone();
      // if the line is not closed, then we will try our best to make it appear closed!
      if !is_somewhat_near(&front, &back) && terminals_are_out_of_view(&front, &back, win) {
        let (start, end) = get_extended_terminals(&front, &back, win);
        line.push_front(start);
        line.push_back(end);
      }
      if should_show(&front, &back, win) {
        draw
          .polygon()
          .color(color)
          .stroke_weight(1.0)
          .stroke(stroke)
          .points(line.clone());
      }
    }
  }

  for line in contour {
    // draw the stroke will full opacity
    draw.polyline().color(stroke).weight(2.0).points(line);
  }
}

fn draw_contour_lines(draw: &Draw, model: &Model, n: usize, contour: Vec<Deque2>) {
  let hue = if n < model.n_contours / 2 { 0.07 } else { 0.59 };
  let stroke = hsla(
    hue,
    0.5,
    map_range(n, 0, model.n_contours - 1, 0.05, 0.3),
    1.,
  );
  for line in contour {
    draw.polyline().color(stroke).weight(2.0).points(line);
  }
}

use nannou::noise::{Fbm, MultiFractal, NoiseFn};
use nannou::prelude::*;

use std::collections::VecDeque;

use delaunator::{triangulate, Point};

use super::PointCloud;

pub type Deque2 = VecDeque<Point2>;

/// A simple container for a Triangle that exists in 3 dimensions.
/// `vertices` should only have 3 points - perhaps will make that explicit in the future.
pub struct Triangle3D {
  vertices: Vec<Point3>,
}

impl Triangle3D {
  /// Returns a optional line segment that traces the contour path across the triangle
  /// for the given threshold (elevation).
  /// If no line exists across this triangle for the given threshold,
  /// return None.
  /// If a line exists, return Some(line)
  /// where `line` is two points, each sitting on one edge of the triangle.
  pub fn contour_line(&self, threshold: f32) -> Option<Deque2> {
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

/// Calculates the Delauney triangulation for the given PointCloud,
/// then maps the 2D points to 3D using an elevation function for the z coodinate.
pub fn calc_triangles(fbm_opts: &FbmOptions, grid: PointCloud) -> Vec<Triangle3D> {
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
  let elevation = fbm_elevation_fn(fbm_opts);
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
pub fn calc_contour(threshold: f32, triangles: &Vec<Triangle3D>) -> Vec<Deque2> {
  triangles
    .iter()
    .map(|t| t.contour_line(threshold))
    .filter_map(|o| o)
    .collect()
}

pub fn is_near(p1: &Point2, p2: &Point2) -> bool {
  p1.distance(*p2) < 1.
}

pub fn is_somewhat_near(p1: &Point2, p2: &Point2) -> bool {
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
pub fn connect_contour_segments(mut segments: Vec<Deque2>) -> Vec<Deque2> {
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

#[derive(Debug)]
pub struct FbmOptions {
  // influences "frequency" of noise "waves"
  pub noise_scale: f64,
  // maximum "height" of points
  pub z_scale: f32,
  // noise seed
  pub seed: f64,
  // These all relate to the "MultiFractal" trait in the noise module
  // These are the defaults from the source for Fbm struct
  //  pub const DEFAULT_OCTAVE_COUNT: usize = 6;
  //  pub const DEFAULT_FREQUENCY: f64 = 1.0;
  //  pub const DEFAULT_LACUNARITY: f64 = std::f64::consts::PI * 2.0 / 3.0;
  //  pub const DEFAULT_PERSISTENCE: f64 = 0.5;
  //  pub const MAX_OCTAVES: usize = 32;
  //
  // Notes to self
  // low persistence w/ low lacunarity = longer "wavelength"
  pub octaves: usize,
  pub frequency: f64,
  pub lacunarity: f64,
  pub persistence: f64,
}

impl FbmOptions {
  pub fn default() -> Self {
    Self {
      noise_scale: 800.0,
      z_scale: 1.0,
      seed: random_range(1.0, 10000.0),
      octaves: 8,
      frequency: random_range(1.2, 1.8),
      lacunarity: random_range(2.0, 2.6),
      persistence: random_range(0.28, 0.38),
    }
  }
}

/// Returns a closure that calculates the elevation for a given (x,y) coordinate.
/// The elevation is based on a noise function which is modulated by the
/// model.seed and model.noise_scale parameters.
/// The height of the resulting "topography" ranges from 0.0 to model.z_scale
pub fn fbm_elevation_fn(opts: &FbmOptions) -> impl Fn(f64, f64) -> f32 {
  // need to extract these values so they can be moved into the closure
  let FbmOptions {
    noise_scale,
    z_scale,
    seed,
    ..
  } = *opts;
  // Fractal brownian motion looks real cool here! And is super configurable
  let noisefn = Fbm::new()
    .set_octaves(opts.octaves)
    .set_frequency(opts.frequency)
    .set_lacunarity(opts.lacunarity)
    .set_persistence(opts.persistence);

  move |x, y| {
    let noise = noisefn.get([x / noise_scale, y / noise_scale, seed]) as f32;
    map_range(noise, -1.0, 1.0, 0.0, z_scale)
  }
}

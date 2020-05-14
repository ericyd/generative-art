use super::PointCloud;
use nannou::prelude::*;

/// Grid is an iterator that returns points in the half-open range
/// i in [0,nx)
/// j in [0,ny)
/// That is, there will always be `nx` i elements and `ny` j elements,
/// but the values will range from 0 to nx-1 for the i values,
/// and from 0 to ny-1 for the j values.
/// (i,j) tuples are returned in column-major order.
/// refs:
///   https://en.wikipedia.org/wiki/Interval_(mathematics)#Including_or_excluding_endpoints
///   https://en.wikipedia.org/wiki/Row-_and_column-major_order
#[derive(Copy, Clone)]
pub struct Grid {
  nx: usize,
  ny: usize,
  curr: usize,
  max: usize,
}

impl Grid {
  pub fn new(nx: usize, ny: usize) -> Self {
    Grid {
      nx,
      ny,
      curr: 0,
      max: nx * ny - 1,
    }
  }
}

impl Iterator for Grid {
  type Item = (usize, usize);

  // Get the next (i,j) tuple, or None if it is maxed
  fn next(&mut self) -> Option<(usize, usize)> {
    if self.curr > self.max {
      return None;
    }
    // Current implementation is column-major order.
    // For row-major order, just a simple adjustment:
    // let x = self.curr % self.nx;
    // let y = (self.curr - x) / self.nx;
    let y = self.curr % self.ny;
    let x = (self.curr - y) / self.ny;
    self.curr += 1;
    Some((x, y))
  }
}

pub fn grid(nx: usize, ny: usize) -> Grid {
  Grid::new(nx, ny)
}

pub fn point_cloud(
  nx: usize,
  ny: usize,
  x_min: f32,
  x_max: f32,
  y_min: f32,
  y_max: f32,
) -> PointCloud {
  Grid::new(nx, ny)
    .map(|(i, j)| {
      pt2(
        map_range(i, 0, nx - 1, x_min, x_max),
        map_range(j, 0, ny - 1, y_min, y_max),
      )
    })
    .collect()
}

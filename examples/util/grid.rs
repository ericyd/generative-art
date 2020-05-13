use super::PointCloud;
use nannou::prelude::*;

#[derive(Copy, Clone)]
struct PointIndex {
  x: usize,
  y: usize,
}

/// Grid is an iterator that returns points in the half-open range
/// x = [0,nx)
/// y = [0,ny)
/// https://stackoverflow.com/a/4396303
/// That is, there will always be `nx` x elements and `ny` y elements,
/// but the values will range from 0 to nx-1 for the x values,
/// and from 0 to ny-1 for the y values.
#[derive(Copy, Clone)]
pub struct Grid {
  nx: usize,
  ny: usize,
  curr: PointIndex,
}

impl Grid {
  pub fn new(nx: usize, ny: usize) -> Self {
    Grid {
      nx,
      ny,
      curr: PointIndex { x: 0, y: 0 },
    }
  }
}

impl Iterator for Grid {
  type Item = (usize, usize);

  // When the `Iterator` is finished, `None` is returned.
  // Otherwise, the next value is wrapped in `Some` and returned.
  fn next(&mut self) -> Option<(usize, usize)> {
    if self.ny - 1 > self.curr.y {
      // without this super ugly nested conditional,
      // the first y element is always skipped.
      // I need to figure out a better way to determine the `next` element...
      if self.curr.x == 0 && self.curr.y == 0 {
        let val = Some((self.curr.x, self.curr.y));
        self.curr.y += 1;
        return val;
      } else {
        self.curr.y += 1;
        return Some((self.curr.x, self.curr.y));
      }
    }
    if self.nx - 1 > self.curr.x {
      self.curr.y = 0;
      self.curr.x += 1;
      return Some((self.curr.x, self.curr.y));
    }
    None
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

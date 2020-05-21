// This is the same as diffusion1 but using a ratio of a:b as the value, instead of separate values for a and b.
// not really working right now but I think it _should_...
// it would be more memory efficient, not sure if it would actually speed things up much

// http://www.karlsims.com/rd.htmldid

extern crate chrono;
extern crate nannou;
// extern crate ndarray;

// use ndarray::{arr3, Array, Array1, Array2, ShapeBuilder};

use nannou::color::*;
use nannou::noise::{Curve, NoiseFn, Perlin, Worley};
use nannou::prelude::*;

mod util;
use util::blob::Blob;
use util::interp::{lerp, nextf, Interp, Interpolate};

fn main() {
  nannou::sketch(view).size(1024, 768).run();
}

fn constrain(val: f32) -> f32 {
  if val < 0. {
    0.
  } else if val > 1.0 {
    1.0
  } else {
    val
  }
}

fn grid_index(i: usize, j: usize, nx: usize) -> usize {
  j * nx + i
}

fn laplace(i: usize, j: usize, grid: &Vec<f32>, a_b: f32, nx: usize, ny: usize) -> f32 {
  if i == 0 || i == nx - 1 || j == 0 || j == ny - 1 {
    if a_b == 0. {
      grid[grid_index(i, j, nx)]
    } else {
      1. - grid[grid_index(i, j, nx)]
    }
  } else {
    if a_b == 0. {
      [
        (grid[grid_index(i - 1, j - 1, nx)]) * 0.05, // bottom left corner
        (grid[grid_index(i - 1, j + 1, nx)]) * 0.05, // bottom right corner
        (grid[grid_index(i + 1, j - 1, nx)]) * 0.05, // top left corner
        (grid[grid_index(i + 1, j + 1, nx)]) * 0.05, // top right corner
        (grid[grid_index(i - 1, j, nx)]) * 0.2,      // left side
        (grid[grid_index(i + 1, j, nx)]) * 0.2,      // right side
        (grid[grid_index(i, j + 1, nx)]) * 0.2,      // top side
        (grid[grid_index(i, j - 1, nx)]) * 0.2,      // bottom side
        (grid[grid_index(i, j, nx)]) * -1.,          // center
      ]
      .iter()
      .sum()
    } else {
      [
        (1. - grid[grid_index(i - 1, j - 1, nx)]) * 0.05, // bottom left corner
        (1. - grid[grid_index(i - 1, j + 1, nx)]) * 0.05, // bottom right corner
        (1. - grid[grid_index(i + 1, j - 1, nx)]) * 0.05, // top left corner
        (1. - grid[grid_index(i + 1, j + 1, nx)]) * 0.05, // top right corner
        (1. - grid[grid_index(i - 1, j, nx)]) * 0.2,      // left side
        (1. - grid[grid_index(i + 1, j, nx)]) * 0.2,      // right side
        (1. - grid[grid_index(i, j + 1, nx)]) * 0.2,      // top side
        (1. - grid[grid_index(i, j - 1, nx)]) * 0.2,      // bottom side
        (1. - grid[grid_index(i, j, nx)]) * -1.,          // center
      ]
      .iter()
      .sum()
    }
  }
}

fn view(app: &App, frame: Frame) {
  let win = app.window_rect();
  let pi2 = PI * 2.0;
  app.set_loop_mode(LoopMode::NTimes {
    // two frames are necessary for capture_frame to work properly
    number_of_updates: 1,
  });

  // Prepare to draw.
  let draw = app.draw();

  // set background color
  // let bg = Alpha::<Hsl<_, _>, f32>::new(RgbHue::from_degrees(36.0), 0.59, 0.90, 1.0);
  draw.background().color(WHITE);

  //set up the geometry of the problem
  let xmin = win.x.start;
  let xmax = win.x.end;
  let ymin = win.y.start;
  let ymax = win.y.end;
  let nx = 100;
  let ny = 100;
  let iterations = 2000;
  let pixel_width = (xmax - xmin) / nx as f32;
  let pixel_height = (ymax - ymin) / ny as f32;

  // diffusivity (D) of quantity "A" and "B"
  let d_a = 1.0;
  let d_b = 0.5;
  // the "feed" or "source" rate of quantity "A"
  let f = 0.055;
  // the "kill" or "sink" rate of quantity "B"
  let k = 0.062;
  // "delta t" - time differential between steps
  let dt = 1.0;

  // initial values for quantity "a" and "b"
  // let mut a = 0.0;
  // let mut b = 1.0;

  // grid holds our quantity concentrations in the form [a, b]
  let mut grid = Vec::with_capacity(nx * ny);
  let mut next = Vec::with_capacity(nx * ny);
  for g in 0..(nx * ny) {
    // initialize with full "A" quantity
    grid.push(1.0);
    next.push(1.0);
  }

  // drop a "circular" blob of "B" quantity in the middle
  // "A" is 1.0, "B" is 0.0
  // for i in nx / 2 - 10..nx / 2 + 10 {
  //   for j in ny / 2 - 10..ny / 2 + 10 {
  //     if ((i - nx / 2) as f32).hypot((j - ny / 2) as f32) < 20. {
  //       grid[grid_index(i, j, nx)] = 0.0;
  //     }
  //   }
  // }

  for i in nx / 2 - 2..nx / 2 + 2 {
    for j in ny / 2 - 2..ny / 2 + 2 {
      grid[grid_index(i, j, nx)] = 0.0;
    }
  }

  for it in 0..iterations {
    for i in 0..nx {
      for j in 0..ny {
        let b = 1. - grid[grid_index(i, j, nx)];
        let a = 1. - b; //grid[grid_index(i, j, nx)];

        // apply convolution
        let grad_a = laplace(i, j, &grid, 0., nx, ny);
        let grad_b = laplace(i, j, &grid, 1., nx, ny);

        let source = f * (1.0 - a);
        let sink = (k + f) * b;
        let reaction = a * b.powi(2);

        let diffusion_a = d_a * grad_a;
        let diffusion_b = d_b * grad_b;

        let a_prime = a + (diffusion_a - reaction + source) * dt;
        let b_prime = b + (diffusion_b + reaction - sink) * dt;

        // println!("init_a: {}, init_b: {}, a: {} b: {}, constrained a: {}, constrained b: {}", a, b, a_prime, b_prime, constrain(a_prime), constrain(b_prime));

        let a_prime = constrain(a_prime);
        let b_prime = constrain(b_prime);

        next[grid_index(i, j, nx)] = constrain(a_prime + b_prime);
      }
    }

    // swap next and grid
    grid = (*next).to_vec();
  }

  for i in 0..nx {
    for j in 0..ny {
      let b = 1. - next[grid_index(i, j, nx)];
      let a = next[grid_index(i, j, nx)];

      println!("a: {} b: {}", a, b);

      let x = win.x.lerp(i as f32 / nx as f32);
      let y = win.y.lerp(j as f32 / ny as f32);

      // let color = if a_prime > b_prime { color_a } else { color_b };
      let hue = constrain(a - b);
      // let hue = a;

      // let hue = constrain(b - a);
      // let color = nannou::color::hsl(hue, 0.5, 0.5);
      let color = nannou::color::hsl(0., 0.0, hue);
      // let color = nannou::color::rgb(a_prime * hue_a * 255., 0., b_prime * hue_b * 255.);

      draw
        .rect()
        .color(color)
        .x_y(x, y)
        .w_h(pixel_width, pixel_height);
    }
  }

  // Write to the window frame.
  draw.to_frame(app, &frame).unwrap();

  // save image
  // app.main_window().capture_frame_threaded(util::captured_frame_path(app, &frame));

  // if frame.nth() > 0 {
  // }
  // std::process::exit(0)
}

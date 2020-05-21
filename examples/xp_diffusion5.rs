// this is the same as diffusion1 but uses a "2d" array instead of a 1d array for the grid

// http://www.karlsims.com/rd.html

extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::noise::{Curve, NoiseFn, Perlin, Worley};
use nannou::prelude::*;
use std::f64::consts::E;

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

fn field1(x: f32, y: f32, t: f64) -> f32 {
  // 7.5 * E.powf(-0.5 * t) as f32 * (2. * x).sin() * (2. * y).sin()
  // println!("{}, {}, {}", E.powf(-0.5 * t) as f32, (2. * x * PI).sin(), (2. * y * PI).sin());
  // E.powf(0.5 * t) as f32 * (2. * x * PI).sin().abs() * (2. * y * PI).sin().abs()

  (E as f32).powf(-16. / t as f32 * ((x - 0.5).powi(2) + (y - 0.5).powi(2))) as f32
}

fn laplace(i: usize, j: usize, grid: &Vec<Vec<[f32; 2]>>, a_b: usize, nx: usize, ny: usize) -> f32 {
  let convolution = [
    [0.05, 0.2, 0.05], //
    [0.2, -1., 0.2],   //
    [0.05, 0.2, 0.05], //
  ];

  // skip boundaries
  let min_i = if i == 0 { 1 } else { 0 };
  let max_i = if i == nx - 1 { 1 } else { 2 };
  let min_j = if j == 0 { 1 } else { 0 };
  let max_j = if j == ny - 1 { 1 } else { 2 };

  // this applies the laplacian convolution by getting all adjacent points to the central point.
  // Weights are defined in the `convolution` variable above
  (min_i..=max_i)
    .map(|ii| {
      (min_j..=max_j)
        .map(|jj| grid[i + ii - 1][j + jj - 1][a_b] * convolution[ii][jj])
        .sum::<f32>()
    })
    .sum::<f32>()
}

fn view(app: &App, frame: Frame) {
  let win = app.window_rect();
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
  let nx = 1000;
  let ny = 1000;
  let iterations = 1;
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

  // grid holds our quantity concentrations in the form [a, b]
  let mut grid = Vec::with_capacity(nx);
  let mut next = Vec::with_capacity(nx);
  for i in 0..nx {
    grid.push(Vec::with_capacity(ny));
    next.push(Vec::with_capacity(ny));

    // initialize with full "A" quantity
    for _j in 0..ny {
      grid[i].push([1., 0.]);
      next[i].push([1., 0.]);
    }
  }

  // drop a "circular" blob of quantity "B" in the middle
  for i in nx / 2 - 10..nx / 2 + 10 {
    for j in ny / 2 - 10..ny / 2 + 10 {
      if ((i - nx / 2) as f32).hypot((j - ny / 2) as f32) < 10. {
        grid[i][j] = [0.0, 1.0];
      }
    }
  }

  for _it in 0..iterations {
    for i in 0..nx {
      for j in 0..ny {
        let x = i as f32 / nx as f32;
        let y = j as f32 / ny as f32;
        let a = constrain(field1(x, y, 1000.));
        let b = 1. - a;

        grid[i][j] = [a, b];

        // let source = f * (1.0 - a);
        // let sink = (k + f) * b;
        // let reaction = a * b.powi(2);

        // // apply convolution
        // let diffusion_a = d_a * laplace(i, j, &grid, 0, nx, ny);
        // let diffusion_b = d_b * laplace(i, j, &grid, 1, nx, ny);

        // let a_prime = a + (diffusion_a - reaction + source) * dt;
        // let b_prime = b + (diffusion_b + reaction - sink) * dt;

        // next[i][j] = [constrain(a_prime), constrain(b_prime)];
      }
    }

    // swap next and grid
    // grid = (*next).to_vec();
  }

  for i in 0..nx {
    for j in 0..ny {
      let a = grid[i][j][0];
      let b = grid[i][j][1];

      let x = win.x.lerp(i as f32 / nx as f32);
      let y = win.y.lerp(j as f32 / ny as f32);

      // let color = if a_prime > b_prime { color_a } else { color_b };
      let hue = constrain(a - b);
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
  app
    .main_window()
    .capture_frame_threaded(util::captured_frame_path(app, &frame));
  // if frame.nth() > 0 {
  // }
  // std::process::exit(0)
}

// trying to do this, with a regular update, because
// it's really hard to see what is happening in realtime

// http://www.karlsims.com/rd.html

// xp_diffusion1
// Average update time (8401 samples): 5.445661
// Let's experiment with some data structures to speed that up!!
//
// xp_diffusion2
// Average update time (8401 samples): 5.1928344
// Slightly faster, not much

extern crate chrono;
extern crate nannou;

use std::time::{Duration, SystemTime};

use nannou::color::*;
use nannou::noise::{Curve, NoiseFn, Perlin, Worley};
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::blob::Blob;
use util::interp::{lerp, nextf, Interp, Interpolate};

fn main() {
  nannou::app(model).update(update).view(view).run();
}

struct Model {
  grid: Grid,
  nx: usize,
  ny: usize,
  update_times: Vec<u128>,
}

type Grid = Vec<Vec<Pixel>>;

struct Pixel {
  current: [f32; 2],
  next: [f32; 2],
}

impl Pixel {
  fn new(a: f32, b: f32) -> Self {
    Self {
      current: [a, b],
      next: [0.0, 0.0],
    }
  }

  fn current(&mut self, a: f32, b: f32) {
    self.current = [a, b];
  }

  fn next(&mut self, a: f32, b: f32) {
    self.next = [a, b];
  }

  fn swap(&self) -> Self {
    // self.current = self.next;
    // self
    Self {
      current: self.next,
      next: self.next,
    }
  }
}

fn laplace(i: usize, j: usize, grid: &Grid, a_b: usize, nx: usize, ny: usize) -> f32 {
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
        .map(|jj| grid[i + ii - 1][j + jj - 1].current[a_b] * convolution[ii][jj])
        .sum::<f32>()
    })
    .sum::<f32>()
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  // grid holds our quantity concentrations in the form [a, b]
  let nx = args.get("nx", 200);
  let ny = args.get("ny", 200);
  // grid holds our quantity concentrations in the form [a, b]
  let mut grid = Vec::with_capacity(nx);
  for i in 0..nx {
    grid.push(Vec::with_capacity(ny));

    // initialize with full "A" quantity
    for _j in 0..ny {
      grid[i].push(Pixel::new(1., 0.));
    }
  }

  // drop a "circular" blob of quantity "B" in the middle
  for i in nx / 2 - 10..nx / 2 + 10 {
    for j in ny / 2 - 10..ny / 2 + 10 {
      if ((i - nx / 2) as f32).hypot((j - ny / 2) as f32) < 10. {
        grid[i][j].current(0.0, 1.0);
      }
    }
  }

  app
    .new_window()
    .title(app.exe_name().unwrap())
    .build()
    .unwrap();
  // app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1000000000000)))
  Model {
    grid,
    nx,
    ny,
    update_times: vec![],
  }
}

// all calculations and grid updates and next/grid swaps should happen here
fn update(_app: &App, model: &mut Model, _update: Update) {
  let now = SystemTime::now();
  // diffusivity (D) of quantity "A" and "B"
  let d_a = 1.0;
  let d_b = 0.5;
  // the "feed" or "source" rate of quantity "A"
  let f = 0.055;
  // the "kill" or "sink" rate of quantity "B"
  let k = 0.062;
  // "delta t" - time differential between steps
  let dt = 1.0;

  let nx = model.nx;
  let ny = model.nx;

  for i in 0..nx {
    for j in 0..ny {
      let a = model.grid[i][j].current[0];
      let b = model.grid[i][j].current[1];

      let source = f * (1.0 - a);
      let sink = (k + f) * b;
      let reaction = a * b.powi(2);

      // apply convolution
      let diffusion_a = d_a * laplace(i, j, &model.grid, 0, nx, ny);
      let diffusion_b = d_b * laplace(i, j, &model.grid, 1, nx, ny);

      let a_prime = a + (diffusion_a - reaction + source) * dt;
      let b_prime = b + (diffusion_b + reaction - sink) * dt;

      model.grid[i][j].next(constrain(a_prime), constrain(b_prime));
    }
  }

  // swap next and grid
  model.grid = model
    .grid
    .iter()
    .map(|row| row.iter().map(|pixel| pixel.swap()).collect())
    .collect();
  match now.elapsed() {
    Ok(elapsed) => model.update_times.push(elapsed.as_millis()),
    Err(e) => println!("Update Error: {:?}", e),
  }
}

// the only thing this should do is draw the model.grid (or maybe model.next)
fn view(app: &App, model: &Model, frame: Frame) {
  // the view takes substantially longer to execute than the update, so only draw sometimes
  if frame.nth() % 50 != 0 {
    return;
  }
  let now = SystemTime::now();
  let win = app.window_rect();

  let xmin = win.x.start;
  let xmax = win.x.end;
  let ymin = win.y.start;
  let ymax = win.y.end;
  let nx = model.nx;
  let ny = model.ny;
  let pixel_width = (xmax - xmin) / nx as f32;
  let pixel_height = (ymax - ymin) / ny as f32;

  // Prepare to draw.
  let draw = app.draw();

  // set background color
  // let bg = Alpha::<Hsl<_, _>, f32>::new(RgbHue::from_degrees(36.0), 0.59, 0.90, 1.0);
  draw.background().color(WHITE);

  for i in 0..nx {
    for j in 0..ny {
      let a = model.grid[i][j].current[0];
      let b = model.grid[i][j].current[1];

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
  // app.main_window().capture_frame_threaded(util::captured_frame_path(app, &frame));

  match now.elapsed() {
    Ok(elapsed) => {
      let n_updates = model.update_times.len();
      println!(
        "
        Average update time ({} samples): {}
        View time: {}",
        n_updates,
        model.update_times.iter().sum::<u128>() as f32 / n_updates as f32,
        elapsed.as_millis()
      );
    }
    Err(e) => println!("View Error: {:?}", e),
  }
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

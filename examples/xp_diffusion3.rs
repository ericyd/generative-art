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
//
// xp_diffusion3
// Average update time (8401 samples): 4.683609
// ooh! getting better!

extern crate chrono;
extern crate nannou;

use std::time::SystemTime;

use nannou::color::*;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::{capture_model, captured_frame_path};

fn main() {
  nannou::app(model).update(update).view(view).run();
}

#[derive(Debug)]
struct Model {
  // default 200
  nx: usize,
  // default 200
  ny: usize,
  loops: u64,
  // diffusivity (D) of quantity "A" and "B"
  // should be in (0,1)
  // default 1.0
  d_a: f32,
  // default 0.5
  d_b: f32,
  // the "feed" or "source" rate of quantity "A" (white)
  // should be not a ton greater than 0.1
  // default: 0.055
  f: f32,
  // the "kill" or "sink" rate of quantity "B" (black)
  // default: 0.062
  k: f32,
  // "delta t" - time differential between steps
  // probably should stay at 1.0
  // default 1.0
  dt: f32,
  grid: Grid,
  update_times: Vec<u128>,
}

struct Grid {
  pixels: Vec<Vec<Pixel>>,
}

impl Grid {
  fn new(nx: usize, ny: usize) -> Self {
    let mut pixels = Vec::with_capacity(nx);
    for i in 0..nx {
      pixels.push(Vec::with_capacity(ny));

      // initialize with full "A" quantity
      for _j in 0..ny {
        pixels[i].push(Pixel::new(1., 0.));
      }
    }
    Self { pixels }
  }

  fn current_at(&self, i: usize, j: usize, a_b: usize) -> f32 {
    self.pixels[i][j].current[a_b]
  }

  fn set_current(&mut self, i: usize, j: usize, a: f32, b: f32) {
    self.pixels[i][j].current = [a, b];
  }

  fn set_next(&mut self, i: usize, j: usize, a: f32, b: f32) {
    self.pixels[i][j].next = [a, b];
  }

  fn swap(&mut self) {
    for row in &mut self.pixels {
      for pixel in row {
        pixel.swap();
      }
    }
  }
}

// implement Debug custom because otherwise
// capture_model logs a billion rows of Pixels
impl std::fmt::Debug for Grid {
  fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
    f.debug_struct("Grid")
      .field("n_rows", &self.pixels.len())
      .field("n_columns", &self.pixels.first().unwrap().len())
      .finish()
  }
}

#[derive(Debug, Clone, Copy)]
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

  fn swap(&mut self) {
    self.current = self.next;
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
        .map(|jj| grid.current_at(i + ii - 1, j + jj - 1, a_b) * convolution[ii][jj])
        .sum::<f32>()
    })
    .sum::<f32>()
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  // grid holds our quantity concentrations in the form [a, b]
  let nx = args.get("size", 200);
  let ny = args.get("size", 200);
  // grid holds our quantity concentrations in the form [a, b]
  let mut grid = Grid::new(nx, ny);

  // drop a "circular" blob of quantity "B" in the middle
  for i in nx / 2 - 10..nx / 2 + 10 {
    for j in ny / 2 - 10..ny / 2 + 10 {
      if ((i - nx / 2) as f32).hypot((j - ny / 2) as f32) < 10. {
        grid.set_current(i, j, 0.0, 1.0);
      }
    }
  }

  app
    .new_window()
    .title(app.exe_name().unwrap())
    .size(nx as u32, ny as u32)
    .build()
    .unwrap();
  let loops = args.get("loops", 32751);
  app.set_loop_mode(LoopMode::loop_ntimes(loops as usize));
  Model {
    grid,
    nx,
    ny,
    update_times: vec![],
    loops,
    // diffusivity (D) of quantity "A" and "B"
    d_a: args.get("d_a", 1.0),
    d_b: args.get("d_b", 0.5),
    // the "feed" or "source" rate of quantity "A"
    f: args.get("f", 0.055),
    // the "kill" or "sink" rate of quantity "B"
    k: args.get("k", 0.062),
    // "delta t" - time differential between steps
    dt: args.get("dt", 1.0),
  }
}

// all calculations and grid updates and next/grid swaps should happen here
fn update(_app: &App, model: &mut Model, _update: Update) {
  let now = SystemTime::now();
  let Model {
    d_a, d_b, f, k, dt, ..
  } = *model;

  let nx = model.nx;
  let ny = model.nx;

  for i in 0..nx {
    for j in 0..ny {
      let a = model.grid.current_at(i, j, 0);
      let b = model.grid.current_at(i, j, 1);

      let source = f * (1.0 - a);
      let sink = (k + f) * b;
      let reaction = a * b.powi(2);

      // apply convolution
      let diffusion_a = d_a * laplace(i, j, &model.grid, 0, nx, ny);
      let diffusion_b = d_b * laplace(i, j, &model.grid, 1, nx, ny);

      let a_prime = a + (diffusion_a - reaction + source) * dt;
      let b_prime = b + (diffusion_b + reaction - sink) * dt;

      model
        .grid
        .set_next(i, j, constrain(a_prime), constrain(b_prime));
    }
  }

  // swap next and grid
  model.grid.swap();
  match now.elapsed() {
    Ok(elapsed) => model.update_times.push(elapsed.as_millis()),
    Err(e) => println!("Update Error: {:?}", e),
  }
}

// the only thing this should do is draw the model.grid (or maybe model.next)
fn view(app: &App, model: &Model, frame: Frame) {
  // the view takes substantially longer to execute than the update, so only draw sometimes
  if frame.nth() % 50 != 0 && frame.nth() < model.loops - 1 {
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
      let a = model.grid.current_at(i, j, 0);
      let b = model.grid.current_at(i, j, 1);

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
  if frame.nth() >= model.loops - 1 {
    app
      .main_window()
      .capture_frame_threaded(captured_frame_path(app, &frame));
    capture_model(app, &frame, model);
  }

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

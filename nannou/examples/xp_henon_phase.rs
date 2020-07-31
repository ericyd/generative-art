// www.complexification.net/gallery/machines/henonPhaseDeep/appletl/henonPhaseDeep_l.pde

extern crate chrono;
extern crate nannou;

use std::time::SystemTime;

// use nannou::color::*;
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
  phase: f32,
  n_travellers: usize,
  travellers: Vec<Traveller>,
  update_times: Vec<u128>,
}

#[derive(Debug)]
struct Traveller {
  x: f32,
  px: f32,
  y: f32,
  py: f32,
  out_of_bounds: bool,
  // color: String,
  color: f32, // hue
}

const MAXPAL: i32 = 512;
const NUMPAL: i32 = 512;
// number of points to draw in each iteration
const dim: f32 = 1000.0;
const offx: f32 = 0.5;
const offy: f32 = -0.1;

impl Traveller {
  fn new(x: f32, y: f32) -> Self {
    let mut traveller = Self {
      x,
      y,
      px: x,
      py: y,
      out_of_bounds: false,
      color: random_range(0.0, 1.0),
    };
    traveller.rebirth();
    traveller
  }

  fn rebirth(&mut self) {
    self.x = random_range(0.0, 1.0);
    self.y = random_range(0.0, 1.0);
    self.out_of_bounds = false;
    let d = self.x.hypot(self.y);
    // let idx = int(numpal*d)%numpal;
    let idx = (NUMPAL * d.ceil() as i32) % NUMPAL;
    let idx = if idx >= NUMPAL {
      NUMPAL - 1
    } else if idx < 0 {
      0
    } else {
      idx
    };
    // self.color = goodcolor[idx];
    // self.color = map_range(idx, 0, MAXPAL, 0.0, 1.0);
    self.color = map_range(d, 0.0, 500.0, 0.0, 1.0);
    // println!("{} {} {}", idx, MAXPAL, self.color);
  }

  fn update(&mut self, ga: f32) {
    // move through time
    let t = self.x * ga.cos() - (self.y - self.x.powi(2)) * ga.sin();
    self.y = self.x * ga.sin() + (self.y - self.x.powi(2)) * ga.cos();
    self.x = t;
    let fuzx = random_range(-0.004, 0.004);
    let fuzy = random_range(-0.004, 0.004);

    // scale the visualization to match the applet size
    // ??????????????????????????????
    let gs = 1.4;

    self.px = fuzx + (self.x / gs + offx) * dim - dim / 2.0;
    self.py = fuzy + (self.y / gs + offy) * dim - dim / 2.0;
  }

  fn draw(&self, draw: &Draw, win: &Rect) {
    if self.px > win.left()
      && self.px < win.right()
      && self.py > win.bottom()
      && self.py < win.top()
    {
      // render
      draw
        .rect()
        .x_y(self.px - dim / 2.0, self.py)
        .hsl(self.color, 0.0, self.color)
        .w_h(1.0, 1.0);
    }
  }
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  // grid holds our quantity concentrations in the form [a, b]
  let nx = args.get("size", dim as usize);
  let ny = args.get("size", dim as usize);
  let n_travellers = args.get("n-travellers", 4000);
  // grid holds our quantity concentrations in the form [a, b]
  let travellers: Vec<Traveller> = (0..n_travellers)
    .map(|_| Traveller::new(0.0, 0.0))
    .collect();

  app
    .new_window()
    .title(app.exe_name().unwrap())
    .size(nx as u32, ny as u32)
    .build()
    .unwrap();
  let loops = args.get("loops", 32751);
  app.set_loop_mode(LoopMode::loop_ntimes(loops as usize));
  Model {
    nx,
    ny,
    n_travellers,
    travellers,
    phase: args.get("phase", PI),
    update_times: vec![],
    loops,
  }
}

// all calculations and grid updates and next/grid swaps should happen here
fn update(_app: &App, model: &mut Model, _update: Update) {
  let now = SystemTime::now();

  // accelerator
  for _ in 0..20 {
    // draw all travelers
    for traveller in &mut model.travellers {
      traveller.update(model.phase);
    }
  }

  // random mutations
  for _ in 0..2 {
    let idx = random_range(0, model.n_travellers);
    // model.travellers[idx] = model.travellers[idx].rebirth();
    model.travellers[idx].rebirth();
  }

  // swap next and grid
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
  let draw = app.draw();

  for traveller in &model.travellers {
    traveller.draw(&draw, &win);
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

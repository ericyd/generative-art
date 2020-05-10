extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::noise::{
  Cylinders, Exponent, Fbm, MultiFractal, NoiseFn, Perlin, RidgedMulti, Turbulence, Worley,
};
use nannou::prelude::*;
use nannou::window::Id;

mod util;
use util::captured_frame_path;

fn main() {
  nannou::app(model).update(update).view(view).run();
}

struct Model {
  a: WindowId,
  b: WindowId,
  c: WindowId,
  d: WindowId,
  e: WindowId,
  f: WindowId,
  g: WindowId,
}

fn model(app: &App) -> Model {
  let a = app
    .new_window()
    .title("Perlin")
    .event(event)
    .build()
    .unwrap();
  let b = app.new_window().title("Fbm").event(event).build().unwrap();

  let c = app
    .new_window()
    .title("Exponent")
    .event(event)
    .build()
    .unwrap();

  let d = app
    .new_window()
    .title("Cylinders")
    .event(event)
    .build()
    .unwrap();

  let e = app
    .new_window()
    .title("RidgedMulti")
    .event(event)
    .build()
    .unwrap();

  let f = app
    .new_window()
    .title("Turbulence")
    .event(event)
    .build()
    .unwrap();

  let g = app
    .new_window()
    .title("Worley")
    .event(event)
    .build()
    .unwrap();

  Model {
    a,
    b,
    c,
    d,
    e,
    f,
    g,
  }
}

fn update(_app: &App, _model: &mut Model, _update: Update) {}

fn event(_app: &App, _model: &mut Model, event: WindowEvent) {}

fn view(app: &App, model: &Model, frame: Frame) {
  app.set_loop_mode(LoopMode::loop_ntimes(1));
  match frame.window_id() {
    id if id == model.a => draw_noise(id, app, frame, Perlin::new()),
    id if id == model.b => draw_noise(id, app, frame, Fbm::new()),
    id if id == model.c => draw_noise(id, app, frame, Exponent::new(&Perlin::new())),
    id if id == model.d => draw_noise(id, app, frame, Cylinders::new()),
    id if id == model.e => draw_noise(id, app, frame, RidgedMulti::new()),
    id if id == model.f => draw_noise(id, app, frame, Turbulence::new(&Perlin::new())),
    id if id == model.g => draw_noise(id, app, frame, Worley::new()),
    _ => (),
  }
}

fn draw_noise<T: NoiseFn<[f64; 3]>>(id: Id, app: &App, frame: Frame, noisefn: T) {
  let win = match app.window(id) {
    Some(window) => window.rect(),
    None => return,
  };

  // Prepare to draw.
  let draw = app.draw();

  draw.background().color(WHITE);

  let noise_scale = 100.0;
  let seed = random_range(0.1, 10000.0);
  println!("noise scale: {}, seed: {}", noise_scale, seed);

  let mut noises = Vec::new();
  for _x in 0..=win.w() as i32 {
    let x = map_range(_x, 0, win.w() as i32, win.left(), win.right());
    for _y in 0..=win.h() as i32 {
      let y = map_range(_y, 0, win.h() as i32, win.bottom(), win.top());
      // let y = map_range(_y, 0, win.h(), win.bottom(), win.top());
      let noise = noisefn.get([x as f64 / noise_scale, y as f64 / noise_scale, 10.0]) as f32;
      noises.push(noise);
      // println!("{}, {}, {}", noise, x, y);
      let val = map_range(noise, -1.0, 1.0, 0.0, 255.0) / 255.0;
      draw
        .rect()
        .color(rgb(val, val, val))
        .x_y(x, y)
        .w_h(1.0, 1.0);
    }
  }

  noises.sort_by(|a, b| a.partial_cmp(b).unwrap());
  let min = noises.first().unwrap();
  let max = noises.last().unwrap();
  println!("{}, {}", max, min);

  draw.to_frame(app, &frame).unwrap();
}

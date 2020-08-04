// cargo run --release --example waves -- --scale 200 --length 500
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::noise::{NoiseFn, Perlin};
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::captured_frame_path;
use util::color::*;
use util::interp::lerp;

fn main() {
  nannou::app(model).run();
}

struct Model {
  noise_length: f32,
  noise_scale: f32,
  loops: usize,
  palette: String,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  let noise_length = args.get_f32("length", 100.);
  let noise_scale = args.get_f32("scale", 300.);
  let loops = args.get_usize("loops", 1);
  let palette = args.get_string("palette", "pink green yellow");

  app
    .new_window()
    .title(app.exe_name().unwrap())
    .view(view)
    .size(1024, 1024)
    .build()
    .unwrap();

  Model {
    noise_length,
    noise_scale,
    loops,
    palette,
  }
}

// the only thing this should do is draw the model.grid (or maybe model.next)
fn view(app: &App, model: &Model, frame: Frame) {
  let palette = get_palette(&model.palette);
  let win = app.window_rect();
  let perlin = Perlin::new();
  app.set_loop_mode(LoopMode::loop_ntimes(model.loops));

  // Prepare to draw.
  let draw = app.draw();

  // Set background
  let cream = hsl(47. / 360., 1., 0.94);
  draw.background().color(cream);

  // drawing properties
  let num_points = 360;
  let n_layers = 10;
  let n_waves = 10;

  for i in 0..n_waves {
    // wave properties
    let y_start = 500. - (i * 100) as f32;
    let (hue, sat, light) = random_color(palette).into_components();
    let seed = random_f64();
    let noise = |x, y| {
      perlin.get([
        x as f64 / model.noise_length as f64,
        y as f64 / model.noise_length as f64,
        seed,
      ]) as f32
    };

    for j in 0..n_layers {
      // layer properties
      let y_offset = y_start - (j * 30) as f32;
      let layer_factor = j as f32 / n_layers as f32;
      let bg_lightness = light * (1. + layer_factor / 2.);
      let stroke_lightness = if j == 0 {
        bg_lightness * 0.5
      } else {
        bg_lightness * 0.9
      };
      // polygon that extends outside the bounds of the window, with edge that is sinusoidal
      let mut wave_pts: Vec<Vector2> = (0..num_points)
        .map(|n| {
          let factor = n as f32 / num_points as f32;
          let x = lerp(win.x.start - 100., win.x.end + 100., factor);
          let y_base = (x / model.noise_length + y_start).sin();

          let y = y_base * noise(x, y_base) * model.noise_scale + y_offset;
          pt2(x, y)
        })
        .collect(); // ends at middle right
      wave_pts.push(pt2(win.x.end + 100., win.y.start - 100.)); // bottom right
      wave_pts.push(pt2(win.x.start - 100., win.y.start - 100.)); // bottom left
      wave_pts.push(pt2(win.x.start - 100., (win.x.start - 100.).sin() * 100.)); // middle left

      // draw wave layers
      draw
        .polygon()
        .color(Hsla::new(hue, sat, bg_lightness, 1.0))
        .stroke(Hsla::new(hue, sat, stroke_lightness, 1.0))
        .points(wave_pts);
    }
  }

  // Write to the window frame.
  draw.to_frame(app, &frame).unwrap();

  let file_path = captured_frame_path(app, &frame);
  app.main_window().capture_frame(file_path);
}

// cargo run --release --example waves_2
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::noise::{NoiseFn, Perlin};
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::brush::*;
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
  let noise_length = args.get_f32("length", 500.);
  let noise_scale = args.get_f32("scale", 200.);
  let loops = args.get_usize("loops", 1);
  let palette = args.get_string("palette", "random");

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

fn offset() -> f32 {
  random_range(-15., 15.)
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
  let num_points = 50;
  let n_layers = 8;
  let n_waves = 10;

  for i in 0..n_waves {
    // wave properties
    let y_wave_start = 500. - (i * 100) as f32;
    let (hue, sat, lum) = random_color(palette).into_components();
    // seed range from 0.5 - 0.7 is very boring/flat
    let seed = random_range(0.01, 0.45);
    let noise = |x, y| {
      perlin.get([
        x as f64 / model.noise_length as f64,
        y as f64 / model.noise_length as f64,
        seed,
      ]) as f32
    };

    for j in 0..n_layers {
      // layer properties
      let y_wave_offset = y_wave_start - (j * 30) as f32;
      let n_brush_strokes = random_range(5, 13);
      let (x_min, x_max) = (win.x.start - 100., win.x.end + 100.);
      let dx = (x_max - x_min) as f32 / n_brush_strokes as f32;

      // draw several brush strokes across the length of the wave
      for brush_stroke in 0..n_brush_strokes {
        // brush stroke properties
        // vary the hue/sat/lum a bit for each brush stroke
        let hue = random_range(
          hue.to_positive_degrees() / 360. * 0.9,
          hue.to_positive_degrees() / 360. * 1.1,
        );
        let sat = random_range(sat * 0.9, sat * 1.1);
        let lum = random_range(lum * 0.9, lum * 1.1);
        // set the brush start position for this part of the curve
        let x_offset = offset();
        let x_start = (x_min + dx * brush_stroke as f32) + x_offset;
        let x_end = (x_min + dx * (brush_stroke as f32 + 1.8)) + x_offset;
        let y_offset = offset();
        let minor_sin_wavelength = random_range(4.3, 6.8);

        // define brush stroke points
        let pts: Vec<Vector2> = (0..num_points)
          .map(|n| {
            let factor = n as f32 / num_points as f32;
            let x = lerp(x_start, x_end, factor);
            let major_sin = (x / model.noise_length + y_wave_start).sin();
            // modifying the sin wave slightly gives a bit of a humanistic flair to the brush strokes
            let minor_sin = (x / model.noise_length * minor_sin_wavelength + y_wave_start).sin();
            let y_base = (major_sin + minor_sin) / 2.;

            let y = y_base * noise(x, y_base) * model.noise_scale + y_wave_offset + y_offset;
            pt2(x, y)
          })
          .collect();

        // draw wave layers
        Brush::new()
          .width(random_range(7., 40.))
          .hsla(hue, sat, lum, 1.0)
          .path(pts, &draw);

        /*
        Alternative: draw straight lines that follow the start/end point on the sin curve.
                     This doesn't look very great, but leaving here as an example

        let y_start = (x_start / model.noise_length + y_wave_start).sin();
        let y_start = y_start * noise(x_start, y_start) * model.noise_scale + y_wave_offset + y_offset;

        let y_end = (x_end / model.noise_length + y_wave_start).sin();
        let y_end = y_end * noise(x_end, y_end) * model.noise_scale + y_wave_offset + y_offset;

        // draw wave layers
        Brush::new()
          .width(15.)
          .hsla(hue.to_positive_degrees() / 360., sat, bg_lum, 1.0)
          .stroke(pt2(x_start, y_start), pt2(x_end, y_end), &draw);
        */
      }
    }
  }

  // Write to the window frame and capture
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

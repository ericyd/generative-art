// cargo run --release --example falling_dots -- --pad 10 --palette muzli10 --max-dots 40
// cargo run --release --example falling_dots -- --loops 2 --x-pad 25 --y-pad 3 --radius 10 --palette muzli10 --max-dots 70
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::blob::Blob;
use util::captured_frame_path;
use util::color::*;
// use util::interp::{Interp, Interpolate}; // necessary for the alternate y spacing option

fn main() {
  nannou::app(model).run();
}

struct Model {
  radius: f32,
  x_padding: f32,
  y_padding: f32,
  loops: usize,
  palette: String,
  max_dots: i32,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  let radius = args.get_f32("radius", 15.);
  let padding = args.get_f32("pad", 0.);
  let x_padding = args.get_f32("x-pad", padding);
  let y_padding = args.get_f32("y-pad", padding);
  let max_dots = args.get_i32("max-dots", 50);
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
    radius,
    x_padding,
    y_padding,
    loops,
    palette,
    max_dots,
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  let palette = get_palette(&model.palette);
  let win = app.window_rect();
  app.set_loop_mode(LoopMode::loop_ntimes(model.loops));

  // Prepare to draw.
  let draw = app.draw();
  let cream = hsl(47. / 360., 1., 0.94);
  draw.background().color(cream);

  draw_texture(&draw, &win);

  draw_dots(&draw, &win, model, &palette);

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

// draw textured background:
// layering a shit ton of nearly-transparent, randomly shaped, randomly-sized,
// randomly-positioned blobs makes for a nice mottled texture!
fn draw_texture(draw: &Draw, win: &Rect) {
  for _i in 0..5000 {
    let hue = random_range(190., 216.) / 360.;
    let sat = random_range(0.3, 0.7);
    let lightness = random_range(0.15, 0.98);
    let alpha = random_range(0.02, 0.035);
    let x = random_range(win.x.start, win.x.end);
    let y = random_range(win.y.start, win.y.end);
    Blob::new()
      .x_y(x, y)
      .width(random_range(10.0, 50.0))
      .height(random_range(10.0, 50.0))
      .rotate_rad(random_range(0., PI * 2.))
      .noise_scale(random_range(0.4, 0.95))
      .hsla(hue, sat, lightness, alpha)
      .fuzziness(random_range(0., 0.8))
      .draw(draw);
  }
}

// draw "falling dots"
fn draw_dots(draw: &Draw, win: &Rect, model: &Model, palette: &[&str; 5]) {
  let dot_width = model.radius + model.x_padding;
  let dot_height = model.radius + model.y_padding;
  let n_lines = (win.x.magnitude() / dot_width).ceil() as i32 + 1;

  for i in 0..n_lines {
    let x = win.x.start + dot_width * i as f32;
    let n_dots = random_range(2, model.max_dots);

    for j in 0..n_dots {
      // the ` - (model.radius / 2.)` makes the top edge tangent to the dots.
      // Remove to have top edge bisect the dots
      let y = win.y.end - dot_height * j as f32 - (model.radius / 2.);
      // this is another nice effect!!
      // let y = Interp::exp(win.y.end, win.y.start, j as f32 / model.max_dots as f32);
      let (hue, _sat, _light) = random_color(*palette).into_components();
      let sat = random_range(_sat * 0.8, _sat * 1.2);
      let light = random_range(_light * 0.8, _light * 1.2);
      draw
        .ellipse()
        .x_y(x, y)
        .color(Hsl::new(hue, sat, light))
        // I would have expected width and height to be radius/2,
        // not sure why it needs to be the full radius.
        // I'm sure my math is off somewhere in my padding
        .w_h(model.radius, model.radius);
    }
  }
}

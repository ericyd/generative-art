// Same as contour6 with new color function.
// Algorithm in a nutshell:
//    1. create elevation function using noise
//    2. apply color to the elevation map on a per-pixel basis
//    3. draw contour lines over the colored plot
// The main advantage being that we don't need to worry about connecting the contours
// into polygons to color them in properly.
// It turns out, it is quite a tricky thing to do properly, connecting lines into polygons!
// Probably there are better implementations out there but mine sucked, so this works
// much better.
//
// cargo run --release --example contours7 -- --loops 10
// cargo run --release --example contours7 -- --grid 400 --noise-scale 800.0 --z-scale 350.0 --seed 652.3919081860134 --octaves 8 --frequency 1.7598356010257492 --lacunarity 2.4589143963376885 --persistence 0.3778940660523947 --n-contours 292 --min-contour 0.01 --max-contour 0.99 --stroke-weight 0.7
// cargo run --release --example contours7 -- --grid 300 --noise-scale 800.0 --z-scale 350.0 --seed 94495.74815492425 --octaves 8 --frequency 1.5656483551621354 --lacunarity 2.1502877368729374 --persistence 0.29562066528218933 --n-contours 356 --min-contour 0.01 --max-contour 0.99 --stroke-weight 1.2
extern crate chrono;
extern crate delaunator;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::contours::*;
use util::grid;
use util::{capture_model, captured_frame_path, point_cloud};

fn main() {
  nannou::app(model).update(update).run();
}

#[derive(Debug)]
struct Model {
  // how grid points on each the x axis and y axis
  grid: usize,
  fbm_opts: MultiFractalOptions,
  // Number of contour thresholds to draw
  n_contours: usize,
  // the min/max percent of the z_scale at which to draw contours.
  // Both values should be in the [0.0,1.0] range
  min_contour: f32,
  max_contour: f32,
  stroke_weight: f32,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();

  app
    .new_window()
    .title(app.exe_name().unwrap())
    .view(view)
    .size(1024, 1024)
    .build()
    .unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get_usize("loops", 1)));

  Model {
    grid: args.get("grid", 100),
    fbm_opts: MultiFractalOptions::default(),
    n_contours: args.get("n-contours", 70),
    min_contour: args.get("min-contour", 0.01),
    max_contour: args.get("max-contour", 0.99),
    stroke_weight: args.get("stroke-weight", 1.5),
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  let args = ArgParser::new();
  model.fbm_opts = MultiFractalOptions {
    seed: args.get("seed", random_range(1.0, 100000.0)),
    noise_scale: args.get("noise-scale", 800.0),
    z_scale: args.get("z-scale", 350.0),
    octaves: args.get("octaves", 8),
    frequency: args.get("frequency", random_range(1.2, 1.8)),
    lacunarity: args.get("lacunarity", random_range(2.0, 2.6)),
    persistence: args.get("persistence", random_range(0.28, 0.38)),
  };
  model.n_contours = args.get("n-contours", random_range(150, 250));
  model.stroke_weight = args.get("stroke-weight", random_range(0.5, 3.0));
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  draw.background().color(WHITE);
  let win = app.window_rect();

  println!(
    "Creating point cloud for {} x {} = {} points ...",
    model.grid,
    model.grid,
    model.grid * model.grid
  );
  // define a window extent larger than the actual window,
  // to increase the odds that contours will result in closed polygons
  let grid = point_cloud(
    model.grid,
    model.grid,
    win.left() * 1.25,
    win.right() * 1.25,
    win.bottom() * 1.25,
    win.top() * 1.25,
  );

  let elevation_fn = fbm_elevation_fn(&model.fbm_opts);
  let triangles = calc_triangles(elevation_fn, grid);

  draw_elevation_map(&draw, model, &win);

  for n in 0..model.n_contours {
    let threshold = map_range(
      n,
      0,
      model.n_contours - 1,
      model.fbm_opts.z_scale * model.min_contour,
      model.fbm_opts.z_scale * model.max_contour,
    );
    println!(
      "drawing contour {} of {} ({} threshold)",
      n + 1,
      model.n_contours,
      threshold
    );
    let contour_segments = calc_contour(threshold, &triangles);
    let (h, s, l) = get_color(n as f32 / (model.n_contours - 1) as f32).into_components();
    draw_contour_lines(
      &draw,
      hsl(h.to_positive_degrees() / 360., s * 2., l / 5.),
      model.stroke_weight,
      contour_segments,
    );
  }

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
  capture_model(app, &frame, model);
}

fn draw_elevation_map(draw: &Draw, model: &Model, win: &Rect) {
  println!("Painting elevation map ...");
  let elevation_map = fbm_elevation_fn(&model.fbm_opts);

  for (i, j) in grid(win.w() as usize + 1, win.h() as usize + 1) {
    let x = map_range(i, 0, win.w() as usize, win.left(), win.right());
    let y = map_range(j, 0, win.h() as usize, win.bottom(), win.top());
    let elevation = elevation_map(x.into(), y.into());
    let frac = map_range(elevation / model.fbm_opts.z_scale, 0.3, 0.7, 0.0, 1.0);
    let color = get_color(frac);
    draw.rect().color(color).x_y(x, y).w_h(1.0, 1.0);
  }
}

fn draw_contour_lines(draw: &Draw, stroke: Hsl, stroke_weight: f32, contour: Vec<Deque2>) {
  for line in contour {
    draw
      .polyline()
      .color(stroke)
      .weight(stroke_weight)
      .points(line);
  }
}

fn get_color(frac: f32) -> Hsl {
  let s = map_range(frac, 0.0, 1.0, 0.5, 0.8);
  let l = map_range(frac, 0.0, 1.0, 0.2, 0.8);
  // For some reason, `overlay` is only available on LinSrgba struct*
  // *also distinctly possible that I'm wrong about that
  let start = LinSrgba::from(hsla(0.99, s, l, 1.0 - frac));
  let end = LinSrgba::from(hsla(0.63, s, l, frac));
  Hsl::from(start.overlay(end))
}

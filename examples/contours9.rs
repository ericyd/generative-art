// Same as contour7 but using RidgedMulti noise function for the elevation map
//
// cargo run --release --example contours9 -- --loops 10
// cargo run --release --example contours9 -- --grid 100 --octaves 2 --frequency 1.1 --lacunarity 4.0 --persistence 0.3 --noise-scale 1000 --n-contours 80 --seed 44132.94229140888 --stroke-weight 2
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
use util::Line2;
use util::{capture_model, captured_frame_path, point_cloud};

fn main() {
  nannou::app(model).update(update).run();
}

#[derive(Debug)]
struct Model {
  // how grid points on each the x axis and y axis
  grid: usize,
  topo_opts: MultiFractalOptions,
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
    topo_opts: MultiFractalOptions::default(),
    n_contours: args.get("n-contours", 70),
    min_contour: args.get("min-contour", 0.01),
    max_contour: args.get("max-contour", 0.99),
    stroke_weight: args.get("stroke-weight", 1.5),
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  let args = ArgParser::new();
  model.topo_opts = MultiFractalOptions {
    seed: args.get("seed", random_range(1.0, 100000.0)),
    noise_scale: args.get("noise-scale", random_range(200., 1000.)),
    z_scale: args.get("z-scale", 350.0),
    octaves: args.get("octaves", random_range(1, 20)),
    frequency: args.get("frequency", random_range(0.2, 3.2)),
    lacunarity: args.get("lacunarity", random_range(1.0, 10.0)),
    persistence: args.get("persistence", random_range(0.1, 2.0)),
  };
  model.n_contours = args.get("n-contours", random_range(40, 60));
  model.stroke_weight = args.get("stroke-weight", random_range(0.75, 2.5));
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

  let elevation_fn = billow_elevation_fn(&model.topo_opts);
  let triangles = calc_triangles(elevation_fn, grid);

  draw_elevation_map(&draw, model, &win);

  for n in 0..model.n_contours {
    let threshold = map_range(
      n,
      0,
      model.n_contours - 1,
      model.topo_opts.z_scale * model.min_contour,
      model.topo_opts.z_scale * model.max_contour,
    );
    println!(
      "drawing contour {} of {} ({} threshold)",
      n + 1,
      model.n_contours,
      threshold
    );
    let contour_segments = calc_contour(threshold, &triangles);
    let frac = n as f32 / (model.n_contours - 1) as f32;
    let (h, s, l) = get_color(frac).saturate(1.0).into_components();
    draw_contour_lines(
      &draw,
      Hsla::new(h, s, l / 1.25, 1.0),
      model.stroke_weight,
      contour_segments,
      frac,
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
  let elevation_map = billow_elevation_fn(&model.topo_opts);

  for (i, j) in grid(win.w() as usize + 1, win.h() as usize + 1) {
    let x = map_range(i, 0, win.w() as usize, win.left(), win.right());
    let y = map_range(j, 0, win.h() as usize, win.bottom(), win.top());
    let elevation = elevation_map(x.into(), y.into());
    let frac = elevation / model.topo_opts.z_scale;
    // for some reason the color returned from get_color has really low saturation (even though it should be 100%)
    // so we bump it up a bit manually.
    let (h, s, l) = get_color(frac).saturate(1.0).into_components();
    draw
      .rect()
      .color(Hsla::new(h, s, l * 1.5, 1.))
      .x_y(x, y)
      .w_h(1.0, 1.0);
  }
}

fn draw_contour_lines(
  draw: &Draw,
  stroke: Hsla,
  stroke_weight: f32,
  contour: Vec<Deque2>,
  frac: f32,
) {
  let points: Line2 = contour.iter().flatten().cloned().collect();
  for (i, point) in points.iter().enumerate() {
    if i % map_range(frac, 0.0, 1.0, 10, 1) != 0 {
      continue;
    }
    draw
      .ellipse()
      .color(stroke)
      .w_h(stroke_weight / frac, stroke_weight / frac)
      .x_y(point.x, point.y);
  }
}

fn get_color(frac: f32) -> Hsl {
  // For some reason, `overlay` is only available on LinSrgba struct*
  // *also distinctly possible that I'm wrong about that
  let start = LinSrgba::from(hsla(10. / 360., 1.0, 0.8, 1.0 - frac));
  let end = LinSrgba::from(hsla(30. / 360., 1.0, 0.15, frac));
  Hsl::from(start.overlay(end))
}

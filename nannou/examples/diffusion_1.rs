// implementation of deconbatch's "Poor man's DLA" in Rust
// https://www.deconbatch.com/2019/10/the-poor-mans-dla-diffusion-limited.html
// cargo run --release --example diffusion_1 -- --animate true --min-dist 0.5 --max-dist 1.10 --loops 2000 --size 8 --init 0.035 --start-radius 150 --outline true --hue 8
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::captured_frame_path;
// TODO: implement palette instead of color gradient
// use util::color::*;

const HEIGHT: u32 = 1024;
const WIDTH: u32 = 1024;
const TWO_PI: f32 = PI * 2.;

struct Model {
  loops: usize,
  animation_rate: u64,
  point_size: f32,
  point_hue: f32,
  cluster: Vec<Point>,
  animate: bool,
  min_dist: f32,
  max_dist: f32,
  start_radius: f32,
  outline: bool,
}

// this is essentially a "Point2" with a hue
#[derive(Debug, Copy, Clone)]
struct Point {
  pub x: f32,
  pub y: f32,
  pub hue: f32,
}

impl Point {
  pub fn new(x: f32, y: f32, hue: f32) -> Self {
    Point { x, y, hue }
  }
}

fn main() {
  nannou::app(model).update(update).run();
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  let loops = args.get_usize("loops", 7200);
  let point_size = args.get_f32("size", 4.0);
  // min/max dist control how close together the dots can be.
  // They scale the point_size, so 1.0 means "1x point_size"
  let min_dist = args.get_f32("min-dist", 1.0);
  let max_dist = args.get_f32("max-dist", 1.2);
  // this is the starting hue - it will shift slowly over the course of the drawing
  let point_hue = args.get_f32("hue", random_range(0., 360.0) / 360.);
  let point_initializer = args.get_f32("init", random_range(0.1, 0.4));
  // should the drawing animate (as opposed to just drawing the final frame)?
  // and if so, how frequently should it update?
  let animate = args.get_bool("animate", false);
  let animation_rate = args.get_u64("rate", 50);
  // this is the size of the starting "circle" on which the points are placed
  let start_radius = args.get_f32("start-radius", 200.);
  // when true, draw circles as outlines instead of filled
  let outline = args.get_bool("outline", false);

  // initialize the cluster with some dots in the center
  let mut cluster = Vec::new();
  let mut i = 0.;
  while i < 1.0 {
    i += point_initializer;
    cluster.push(Point::new(
      start_radius * (TWO_PI * i).cos(),
      start_radius * (TWO_PI * i).sin(),
      point_hue,
    ));
  }

  app
    .new_window()
    .title(app.exe_name().unwrap())
    .view(view)
    .size(WIDTH, HEIGHT)
    .build()
    .unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(loops));

  Model {
    loops,
    point_size,
    point_hue,
    cluster,
    animate,
    animation_rate,
    min_dist,
    max_dist,
    start_radius,
    outline,
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  let Model {
    cluster,
    point_hue,
    point_size,
    min_dist,
    max_dist,
    ..
  } = model;
  // "original" version by deconbatch continuously modifies entry_radius with the following formula
  // *entry_radius = (*entry_radius * 2.) % TWO_PI;
  //
  // note from eric: I think modulus works in kind of a peudo-random way many times,
  // so I'm just going to go straight up random!
  let entry_radius = random_f32() * TWO_PI;

  let mut x = WIDTH as f32 * entry_radius.cos();
  let mut y = HEIGHT as f32 * entry_radius.sin();
  for _attempt_to_find_collision in 0..(HEIGHT as i32 * 2) {
    // walk to the center
    x -= entry_radius.cos();
    y -= entry_radius.sin();

    // avoid putting dots in the "center" of the circle.
    // removing this also creates interesting shapes, it's just not what I want right now
    if x.hypot(y) < model.start_radius {
      break;
    }

    if check_collision(&cluster, pt2(x, y), *point_size, *min_dist, *max_dist) {
      cluster.push(Point::new(x, y, *point_hue));
      *point_hue += 0.00005;
      break;
    }
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  let cream = hsl(47. / 360., 1., 0.98);
  draw.background().color(cream);

  // draw every `rate` frames if animating
  println!("{}", frame.nth()); // kind of nice to know things are running smoothly if it isn't animating
  if model.animate && frame.nth() > 0 && frame.nth() % model.animation_rate == 0 {
    draw_cluster(&model.cluster, model.point_size, model.outline, &draw);
    draw.to_frame(app, &frame).unwrap();
    // app
    // .main_window()
    // .capture_frame(captured_frame_path(app, &frame));
  }

  // draw final frame
  if frame.nth() == model.loops as u64 - 1 {
    draw_cluster(&model.cluster, model.point_size, model.outline, &draw);
    draw.to_frame(app, &frame).unwrap();
    app
      .main_window()
      .capture_frame(captured_frame_path(app, &frame));
  }
}

/// draw the points cluster
fn draw_cluster(cluster: &Vec<Point>, size: f32, outline: bool, draw: &Draw) {
  let mut sat = 40.0;
  let mut lightness = 60.0;
  let stroke_weight = if outline { size / 4. } else { 0.0 };
  for p in cluster {
    sat += 0.009;
    lightness -= 0.003;
    let color = hsla(p.hue, sat / 100., lightness / 100., 1.0);
    let stroke = if outline { color } else { hsla(0., 0., 0., 0.) };
    let fill = if outline { hsla(0., 0., 1., 1.) } else { color };

    draw
      .ellipse()
      .x_y(p.x, p.y)
      .w_h(size, size)
      .color(fill)
      .stroke(stroke)
      .stroke_weight(stroke_weight);
  }
}

/// check collision between a point and the cluster.
fn check_collision(
  cluster: &Vec<Point>,
  _p: Point2,
  size: f32,
  min_dist: f32,
  max_dist: f32,
) -> bool {
  for point in cluster {
    if pt2(point.x, point.y).distance(pt2(_p.x, _p.y)) < size * min_dist {
      return false;
    }
  }

  for p in cluster {
    if pt2(p.x, p.y).distance(pt2(_p.x, _p.y)) < size * max_dist {
      return true;
    }
  }
  return false;
}

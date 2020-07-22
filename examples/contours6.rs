// New idea: instead of trying to join polygons,
// why not just color the elevation map and then draw contours directly?
//
// This is an implementation of Bruce Hill's "Meandering Triangles" contour algorithm:
// https://blog.bruce-hill.com/meandering-triangles
//
// cargo run --release --example contours6
// cargo run --release --example contours6 -- --grid 101 --seed 28912.4 --noise-scale 800.0 --z-scale 350.0  --min-contour 0.01 --max-contour 0.99 --octaves 8 --frequency 0.8 --lacunarity 2.3 --persistence 0.35 --n-contours 189
extern crate chrono;
extern crate delaunator;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::contours::*;
use util::{capture_model, captured_frame_path, point_cloud};

fn main() {
  nannou::app(model).update(update).run();
}

#[derive(Debug)]
struct Model {
  // how grid points on each the x axis and y axis
  grid: usize,
  fbm_opts: FbmOptions,
  // Number of contour thresholds to draw
  n_contours: usize,
  // the min/max percent of the z_scale at which to draw contours.
  // Both values should be in the [0.0,1.0] range
  min_contour: f32,
  max_contour: f32,
  // when true, draw polygons. Else draw lines
  closed: bool,
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
    fbm_opts: FbmOptions {
      seed: args.get("seed", random_range(1.0, 100000.0)),
      noise_scale: args.get("noise_scale", 800.0),
      z_scale: args.get("z-scale", 350.0),
      octaves: args.get("octaves", 2),
      frequency: args.get("frequency", 1.45),
      lacunarity: args.get("lacunarity", std::f64::consts::PI),
      persistence: args.get("persistence", 0.78),
    },
    n_contours: args.get("n-contours", 70),
    min_contour: args.get("min-contour", 0.01),
    max_contour: args.get("max-contour", 0.99),
    closed: args.get("closed", false),
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  let args = ArgParser::new();
  model.fbm_opts = FbmOptions {
    seed: args.get("seed", random_range(1.0, 100000.0)),
    noise_scale: args.get("noise_scale", 800.0),
    z_scale: args.get("z-scale", 350.0),
    octaves: args.get("octaves", 8),
    frequency: args.get("frequency", random_range(1.2, 1.8)),
    lacunarity: args.get("lacunarity", random_range(2.0, 2.6)),
    persistence: args.get("persistence", random_range(0.28, 0.38)),
  };
  model.n_contours = args.get("n-contours", random_range(150, 250));
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

  let triangles = calc_triangles(&model.fbm_opts, grid);

  draw_elevation_map(&draw, model, &win);

  // draw from highest to lowest contour
  for n in 0..model.n_contours {
    let threshold = map_range(
      n,
      0,
      model.n_contours - 1,
      model.fbm_opts.z_scale * model.max_contour,
      model.fbm_opts.z_scale * model.min_contour,
    );
    println!(
      "drawing contour {} of {} ({} threshold)",
      n + 1,
      model.n_contours,
      threshold
    );
    let contour_segments = calc_contour(threshold, &triangles);
    // if we don't care about polygons, we can save the computer power of calculating them
    if model.closed {
      draw_contour_polygons(&draw, model, n, contour_segments, &win);
    } else {
      draw_contour_lines(&draw, model, n, contour_segments);
    }
  }

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
  capture_model(app, &frame, model);
}

fn draw_elevation_map(draw: &Draw, model: &Model, win: &Rect) {
  println!("drawing elevation map...");
  let elevation_map = fbm_elevation_fn(&model.fbm_opts);

  for _x in 0..=win.w() as i32 {
    let x = map_range(_x, 0, win.w() as i32, win.left(), win.right());
    for _y in 0..=win.h() as i32 {
      let y = map_range(_y, 0, win.h() as i32, win.bottom(), win.top());

      let elevation = elevation_map(x.into(), y.into());
      let frac = map_range(elevation / model.fbm_opts.z_scale, 0.3, 0.7, 0.0, 1.0);
      let hue = if elevation < model.fbm_opts.z_scale / 2. {
        map_range(frac, 0., 1., 0.04, 0.07)
      } else {
        map_range(frac, 0., 1., 0.55, 0.61)
      };
      let color = hsla(
        hue,
        map_range(frac, 0., 1., 0.4, 0.7),
        map_range(frac, 0., 1., 0.3, 0.8),
        1.,
      );
      draw.rect().color(color).x_y(x, y).w_h(1.0, 1.0);
    }
  }
}

fn has_terminal_on_sides(front: &Point2, back: &Point2, win: &Rect) -> bool {
  (front.x < win.left() && back.x > win.right()) || (back.x < win.left() && front.x > win.right())
}

fn should_go_left(front: &Point2, back: &Point2) -> bool {
  (front.x + back.x) / 2. < 0.
}

fn should_go_down(front: &Point2, back: &Point2) -> bool {
  (front.y + back.y) / 2. < 0.
}

// if the polygon doesn't connect, that should indicate that the terminals are off screen.
// We can make sure the connections for the polygon don't slice through the image
// by adding fake terminals that are substantially out of view.
// If terminals are on opposite sides of map, draw up/down
// else, draw left or right
fn get_extended_terminals(front: &Point2, back: &Point2, win: &Rect) -> (Point2, Point2) {
  if has_terminal_on_sides(front, back, win) {
    if should_go_down(front, back) {
      (
        pt2(front.x, front.y - win.h()),
        pt2(back.x, back.y - win.h()),
      )
    } else {
      (
        pt2(front.x, front.y + win.h()),
        pt2(back.x, back.y + win.h()),
      )
    }
  } else if should_go_left(front, back) {
    (
      pt2(front.x - win.w(), front.y),
      pt2(back.x - win.w(), back.y),
    )
  } else {
    (
      pt2(front.x + win.w(), front.y),
      pt2(back.x + win.w(), back.y),
    )
  }
}

fn terminals_are_in_view(front: &Point2, back: &Point2, win: &Rect) -> bool {
  win.left() < front.x
    && front.x < win.right()
    && win.left() < back.x
    && back.x < win.right()
    && win.bottom() < front.y
    && front.y < win.top()
    && win.bottom() < back.y
    && back.y < win.top()
}

fn terminals_are_out_of_view(front: &Point2, back: &Point2, win: &Rect) -> bool {
  (win.left() > front.x || front.x > win.right() || win.bottom() > front.y || front.y > win.top())
    && (win.left() > back.x || back.x > win.right() || win.bottom() > back.y || back.y > win.top())
}

fn should_show(front: &Point2, back: &Point2, win: &Rect) -> bool {
  terminals_are_in_view(front, back, win) || terminals_are_out_of_view(front, back, win)
}

// Draw all the lines for the contour.
// In contours1 and contours2, the `contour` vector contained a ton of 2-point segments.
// In this implementation, it contains full "connected" contours. However, any given
// contour can still have multiple non-contiguous segments, even if they are all closed.
// Hence, we still have a Vec<Line2>, but each Line2 has a very different meaning.
// In retrospect, I probably should have given the contour_segments a specific data type
// in contour1 and contour2, but y'know you live and learn
fn draw_contour_polygons(
  draw: &Draw,
  model: &Model,
  n: usize,
  contour_segments: Vec<Deque2>,
  win: &Rect,
) {
  let contour = connect_contour_segments(contour_segments);
  let hue = if n < model.n_contours / 2 { 0.07 } else { 0.59 };
  let color = hsla(
    hue,
    0.5,
    map_range(n, 0, model.n_contours - 1, 0.3, 0.8),
    0.1,
  );
  let stroke = hsla(hue, 0.4, 0.05, 1.);
  for mut line in contour.clone() {
    if !line.is_empty() {
      let front = line.front().unwrap().clone();
      let back = line.back().unwrap().clone();
      // if the line is not closed, then we will try our best to make it appear closed!
      if !is_somewhat_near(&front, &back) && terminals_are_out_of_view(&front, &back, win) {
        let (start, end) = get_extended_terminals(&front, &back, win);
        line.push_front(start);
        line.push_back(end);
      }
      if should_show(&front, &back, win) {
        draw
          .polygon()
          .color(color)
          .stroke_weight(1.0)
          .stroke(stroke)
          .points(line.clone());
      }
    }
  }

  for line in contour {
    // draw the stroke will full opacity
    draw.polyline().color(stroke).weight(2.0).points(line);
  }
}

fn draw_contour_lines(draw: &Draw, model: &Model, n: usize, contour: Vec<Deque2>) {
  let hue = if n > model.n_contours / 2 { 0.07 } else { 0.59 };
  let stroke = hsla(
    hue,
    0.8,
    map_range(n, 0, model.n_contours - 1, 0.1, 0.3),
    1.,
  );
  for line in contour {
    draw.polyline().color(stroke).weight(1.5).points(line);
  }
}

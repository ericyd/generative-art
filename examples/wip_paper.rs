extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::noise::{Fbm, NoiseFn};
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::{captured_frame_path, rotate, Line2};

fn main() {
  nannou::app(model).run();
}

struct Model {
  radius: f32,
  graphite: Hsl,
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
    radius: args.get("radius", 250.0),
    graphite: hsl(0.0, 0.0, 0.14),
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  let win = app.window_rect();
  draw.background().color(hsl(0.15, 0.4, 0.965));

  draw_paper_texture(&draw, model, &win);
  let arc = arc(model);
  draw_arc(&draw, &arc, model.graphite);
  draw_crosshatch(&draw, model, &arc);

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

// randomly generate short squiggles that are distored by Fbm noise (fractal brownian motion)
fn draw_paper_texture(draw: &Draw, _model: &Model, win: &Rect) {
  let fbm = Fbm::new();
  let seed = random_f64();
  let noise_scale = 60.0;
  for _ in 0..=200 {
    let y = win.y.lerp(random_f32());
    let mut x = win.x.lerp(random_f32());
    let points = (0..=200).map(|_| {
      x += 1.;
      let noise = fbm.get([x as f64 / noise_scale, y as f64 / noise_scale, seed]) as f32;
      let y = y + noise * 10.;
      pt2(x, y)
    });
    draw.polyline().color(hsl(0., 0., 0.915)).points(points);
  }
}

fn arc(model: &Model) -> Line2 {
  let fbm = Fbm::new();
  let seed = random_f64();
  // (-100..173)
  (-70..203)
    .map(|deg| {
      let angle = deg_to_rad(deg as f32);
      let x = angle.cos() * model.radius;
      let y = angle.sin() * model.radius;
      let x = x + fbm.get([x as f64 / 60.0, y as f64 / 60.0, seed]) as f32 * 3.0;
      let y = y + fbm.get([x as f64 / 60.0, y as f64 / 60.0, seed]) as f32 * 3.0;
      pt2(x, y)
    })
    .collect()
}

fn draw_arc(draw: &Draw, arc: &Line2, graphite: Hsl) {
  draw
    .polyline()
    .weight(2.75)
    .color(graphite)
    .points(arc.clone());
}

fn draw_crosshatch2(draw: &Draw, model: &Model, arc: &Line2) {
  let angle1 = PI / 2.0 + PI / 36.0;
  let angle2 = PI + PI / 18.0;
  let n = 70;
  // draw "vertical" lines
  let mut vert_lines = Vec::new();
  for (i, pt) in arc.clone()[0..=n].into_iter().enumerate() {
    let length = map_range(i, 0, n, 80.0, 10.0);
    let end_x = pt.x + angle1.cos() * length;
    let end_y = pt.y + angle1.sin() * length;
    let end = pt2(end_x, end_y);
    draw.line().color(model.graphite).start(*pt).end(end);
    vert_lines.push(vec![*pt, end]);
  }

  // Draw "horizontal" lines
  // Algorithm in a nutshell:
  // [1] find the vertical line that extends beyond the y coordinate of the start point,
  //     when adjusted for the angle of the resulting line
  // [2] If there are no vertical lines that extend beyond the start point,
  //     draw a default length line
  // [3] Else, draw a line that extends to the x position of the vertical line,
  //     and calculate the y position based on the length
  for pt in arc.clone()[0..=n].into_iter() {
    let start_y = pt.y;
    let taller_lines: Vec<Vec<Point2>> = vert_lines
      .clone()
      .into_iter()
      .filter(|line| {
        // [1]
        let top = line[1];
        let horiz_length = pt.distance(top);
        let end_y = start_y + angle2.sin() * horiz_length;
        line[1].y > end_y
      })
      .collect();
    if taller_lines.is_empty() {
      // [2]
      let horiz_length = 5.0;
      let end_x = pt.x + angle2.cos() * horiz_length;
      let end_y = pt.y + angle2.sin() * horiz_length;
      draw
        .line()
        .color(model.graphite)
        .start(*pt)
        .end(pt2(end_x, end_y));
    } else {
      // [3]
      let target = taller_lines[0][0];
      let horiz_length = pt.distance(target);
      let end_x = taller_lines[0][1].x;
      let end_y = pt.y + angle2.sin() * horiz_length;
      draw
        .line()
        .color(model.graphite)
        .start(*pt)
        .end(pt2(end_x, end_y));
    }
  }
}

fn draw_crosshatch(draw: &Draw, model: &Model, arc: &Line2) {
  // define boundary of area that will be crosshatched
  let n = 70;
  let lower_bound = (0..=n).map(|n| arc[n].clone());
  let upper_bound = (0..=n)
    .map(|n| arc[n].clone())
    .map(|pt| rotate(pt, pt2(100.0, 100.0), -0.5))
    .map(|pt| pt2(pt.x + 50.0, pt.y + 50.0));
}

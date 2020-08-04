// cargo run --release --example segmented_circle_drip -- --drips 270 --palette eric1 --stroke-hue 0.52 --stroke-weight 2
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::circle::Circle;
use util::color::*;
use util::{captured_frame_path, smooth_by, Line2};

fn main() {
  nannou::app(model).run();
}

struct Model {
  min_radius: f32,
  max_radius: f32,
  palette: String,
  n_segments: i32,
  n_drips: i32,
  stroke_weight: f32,
  stroke_hue: f32,
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
    min_radius: args.get("min-radius", 40.),
    max_radius: args.get("max-radius", 450.),
    palette: args.get_string("palette", "random"),
    n_segments: args.get("segments", 30),
    n_drips: args.get("drips", 40),
    stroke_weight: args.get("stroke-weight", 3.0),
    stroke_hue: args.get("stroke-hue", 0.6),
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  // named colors https://docs.rs/nannou/0.13.1/nannou/color/named/index.html
  draw.background().color(WHITESMOKE);

  let win = app.window_rect();
  let circles = generate_circles(model, &win);
  let start_lines = generate_drip_lines(model, &win);
  let drip_lines = drip(&circles, start_lines);
  draw_circles(&draw, circles, model);
  draw_drip_lines(&draw, drip_lines, model);

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

fn generate_circles(model: &Model, win: &Rect) -> Vec<Circle> {
  let mut circles: Vec<Circle> = Vec::with_capacity(5);
  while circles.len() < 5 {
    // arrange the circle randomly
    let mut radius = model.min_radius;
    let x_partition = win.x.magnitude() / 5.;
    let x_min = win.x.start + x_partition * circles.len() as f32;
    let x_max = win.x.start + x_partition * (circles.len() + 1) as f32;
    let mut x = random_range(x_min, x_max);
    let mut y_offset = 200.;
    let mut y = random_range(win.y.start + y_offset, win.y.end - y_offset * 2.);
    let mut circle = Circle::new(x, y, radius);

    // if the circle intersects another circle when it is initiated, try moving it so it does not intersect
    while circles.iter().any(|c| c.has_intersection(circle)) {
      x = random_range(x_min, x_max);
      y_offset -= 1.;
      y = random_range(win.y.start + y_offset, win.y.end - y_offset * 2.);
      circle = Circle::new(x, y, radius);
    }

    // grow the circle until it is approximately touching another circle
    while !circles
      .iter()
      .any(|c| c.has_padded_intersection(circle, 50.))
    {
      if radius > x_partition - 20. || radius > model.max_radius {
        break;
      }
      radius += 1.;
      circle = Circle::new(x, y, radius);
    }
    circles.push(circle);
  }
  circles
}

// create "evenly" spaced starting points for our drips
fn generate_drip_lines(model: &Model, win: &Rect) -> Vec<Point2> {
  (0..model.n_drips)
    .map(|n| {
      pt2(
        win.x.lerp(n as f32 / model.n_drips as f32) + random_range(-2.0, 2.0),
        win.y.end + 10.,
      )
    })
    .collect()
}

// drip initial points from top of view to bottom,
// while avoiding the "obstacles" of the circles.
fn drip(circles: &Vec<Circle>, drips: Vec<Point2>) -> Vec<Line2> {
  drips
    .iter()
    .map(|drip| {
      // set initial line conditions.
      // The line will always try to move in the "main_angle" direction,
      // but will by able to modify by it increments of "angle_adjustment"
      // in order to find a path that doesn't intersect a circle
      let main_angle = -PI / 2.; // straight down
      let mut x = drip.x;
      let mut y = drip.y;
      let mut angle_adjustment = 0.1;

      // create the actual line from 1200 segments
      let drip_line = (0..1200)
        .map(|_n| {
          let mut angle = main_angle;

          // if the "next point" would intersect any of the circles,
          // adjust the angle by small amounts until it no longer intersects.
          // TODO: avoid the possibility of infinite loops by breaking after a certain time
          while circles
            .iter()
            .any(|c| c.contains(pt2(x + angle.cos(), y + angle.sin())))
          {
            // if the angle is more than PI/2 away from the starting angle, then we've started to "go backwards".
            // In this case, we want to try going the opposite direction
            if angle >= main_angle + PI / 2. || angle <= main_angle - PI / 2. {
              angle = main_angle;
              angle_adjustment = angle_adjustment * -1.0;
            }
            angle += angle_adjustment;
          }

          // the angle is acceptable! Apply + create new point
          x += angle.cos();
          y += angle.sin();
          pt2(x, y)
        })
        .collect();

      // smooth the final line (looks much better)
      smooth_by(10, &drip_line)
    })
    .collect()
}

fn draw_drip_lines(draw: &Draw, drips: Vec<Line2>, model: &Model) {
  for line in drips {
    draw
      .polyline()
      .weight(model.stroke_weight)
      .color(hsl(
        random_range(model.stroke_hue * 0.95, model.stroke_hue * 1.05),
        0.44,
        random_range(0.45, 0.53),
      ))
      .points(line);
  }
}

fn draw_circles(draw: &Draw, circles: Vec<Circle>, model: &Model) {
  let palette = palette_to_hsl(get_palette(&model.palette).to_vec());
  circles.iter().enumerate().for_each(|(i, circle)| {
    // shrink the circle slightly, so the drip line appear to have some "padding" around the circle
    let max = circle.radius - 5.;
    draw_segments(
      draw,
      max * 0.1,
      max,
      model.n_segments,
      circle.x,
      circle.y,
      palette[i as usize],
    )
  });
}

/// draw all segments around the circle
fn draw_segments(draw: &Draw, min: f32, max: f32, n_segments: i32, x: f32, y: f32, color: Hsl) {
  // TODO: explore having a random radians_per_segment
  let radians_per_segment = (2. * PI) / n_segments as f32;
  for n in 0..n_segments {
    let start_theta = n as f32 * radians_per_segment;
    let end_theta = (n + 1) as f32 * radians_per_segment;

    let min = random_range(min, max * 0.25);

    draw_segment(draw, min, max, start_theta, end_theta, x, y, color);
  }
}

fn draw_segment(
  draw: &Draw,
  min_radius: f32,
  max_radius: f32,
  start: f32,
  end: f32,
  x: f32,
  y: f32,

  color: Hsl,
) {
  let total_points = 20;
  // when this is 1 less than total_points, it results in a "borderless" feel.
  // when it is equal, there is a "borders" feel
  let points_per_segment = 20.;
  // reverse these points so the resulting polygon is concave
  let inner_point_mapper = point_mapper(min_radius, start, end, x, y, points_per_segment);
  let inner_points = (0..total_points).map(inner_point_mapper).rev();
  let outer_point_mapper = point_mapper(max_radius, start, end, x, y, points_per_segment);
  let outer_points = (0..total_points).map(outer_point_mapper);
  let points: Vec<Point2> = inner_points.chain(outer_points).collect();

  let mod_color = get_segment_color(color);
  draw.polygon().color(mod_color).points(points);
}

// Not sure if it's better to return the closure with this `impl Fn` notation
// as recommended by this
// https://doc.rust-lang.org/stable/rust-by-example/fn/closures/output_parameters.html
// or by wrapping in a Box as recommended by this
// https://doc.rust-lang.org/book/ch19-05-advanced-functions-and-closures.html
//
// I think the impl Fn is a bit cleaner looking and also a little less verbose
// so I'm going with that style here
fn point_mapper(
  radius: f32,
  start_theta: f32,
  end_theta: f32,
  x: f32,
  y: f32,
  n_points: f32,
) -> impl Fn(i32) -> Point2 {
  move |n| {
    let factor = n as f32 / n_points;
    let theta = start_theta + factor * (end_theta - start_theta);
    let x = theta.cos() * radius + x;
    let y = theta.sin() * radius + y;
    pt2(x, y)
  }
}

// Slightly adjust hue, saturation, and luminosity of the base color
fn get_segment_color(color: Hsl) -> Hsl {
  let (hue, sat, lum) = color.into_components();
  let hue = hue.to_positive_degrees() / 360.;
  let h = random_range(hue * 0.95, hue * 1.05);
  let s = random_range(sat * 0.9, sat * 1.1);
  let l = random_range(lum * 0.9, lum * 1.1);
  hsl(h, s, l)
}

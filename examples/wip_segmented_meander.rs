// cargo run --release --example segmented_conch -- --segments 71 --min-radius 10 --max-radius 70
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::captured_frame_path;
use util::interp::lerp;

fn main() {
  nannou::app(model).run();
}

struct Model {
  x_start: f32,
  x_end: f32,
  y_start: f32,
  y_end: f32,
  hue: f32,
  sat: f32,
  lum: f32,
  detail: i32,
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
  let win = app.window_rect();

  Model {
    x_start: args.get("x-start", random_range(win.x.start, win.x.end)),
    x_end: args.get("x-end", random_range(win.x.start, win.x.end)),
    y_start: args.get("y-start", win.y.start),
    y_end: args.get("y-end", win.y.end),
    hue: args.get("hue", 0.25),
    sat: args.get("sat", 0.6),
    lum: args.get("lum", 0.4),
    // "detail" is the number of recursions we will use in our meandering line.
    // It needn't go much above 10. Very high numbers might crash something - it's exponential growth
    detail: args.get("detail", 11),
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  // named colors https://docs.rs/nannou/0.13.1/nannou/color/named/index.html
  draw.background().color(IVORY);

  let line = vec![
    pt2(0., -200.),
    pt2(-150., -50.),
    pt2(0., 200.),
    pt2(50., 100.),
    pt2(100., 150.),
  ];
  // let line = meander(model);
  draw_segments(&draw, model, line);

  // Write to the window frame. and capture image
  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
}

// Finite subdivision rule algorithm to create a "fractal" line.
// Each subdivision diplaces the point by a random amount, perpendicular
// to the orientation of the line.
//
// The algorithm uses in-place mutation of an array -- my suspicion
// is that all the inserts are pretty inefficient. I would like to investigate
// using a linked list for this, or possibly another data structure
fn meander(model: &Model) -> Vec<Point2> {
  let start = pt2(model.x_start, model.y_start);
  let end = pt2(model.x_end, model.y_end);
  let mut points: Vec<Point2> = vec![start, end];

  for _recursion in 0..model.detail {
    let temp_points = points.clone();
    let iter_max = temp_points.len() - 1;

    for i in 0..iter_max {
      let one = temp_points[i];
      let two = temp_points[i + 1];
      let x_mid = (two.x + one.x) / 2.0;
      let y_mid = (two.y + one.y) / 2.0;
      let distance = one.distance(two);
      let orientation = ((two.y - one.y) / (two.x - one.x)).atan();
      let perpendicular = orientation + PI / 2.;

      let new = pt2(
        x_mid + perpendicular.cos() * (distance / 2. * random_f32()),
        y_mid + perpendicular.sin() * (distance / 2. * random_f32()),
      );
      points.insert(i * 2 + 1, new);
    }
  }

  points
}

// https://en.wikipedia.org/wiki/Dot_product
fn calc_theta(one: Point2, two: Point2) -> f32 {
  println!(
    "  one {} {}
  two {} {}
  ",
    one.x, one.y, two.x, two.y
  );
  ((two.x * one.x + two.y * one.y) / (two.x.hypot(two.y) * one.x.hypot(one.y)))
    .acos()
    .abs()
}

/*




HOLY
FUCKING
SHIT

https://github.com/nannou-org/nannou/pull/570/files#diff-5a6b0a5023dac3b688c9a77ffe127a38R16-R17

JUST BUMP + IT SHOULD WORK


ALSO THIS!!!!!!
https://github.com/nannou-org/nannou/pull/570/files#diff-5a6b0a5023dac3b688c9a77ffe127a38R39
I SHOULD DO THIS TOO!!!!




HOLY CRAP WHAT IS THIS!!!!
https://github.com/nannou-org/nannou/pull/570/files#diff-5a6b0a5023dac3b688c9a77ffe127a38R69-R70
*/

// fn draw_segments(draw: &Draw, model: &Model, line: Vec<Point2>) {
//   draw
//     .polyline()
//     .color(hsl(0.1, 0.5, 0.5))
//     .points(line.clone());
//   line.iter().enumerate().for_each(|(i, pt)| {
//     if i >= line.len() - 3 {
//       return;
//     }
//     let one = pt.clone();
//     let two = line[i + 1].clone();
//     let three = line[i + 2].clone();
//     let four = line[i + 3].clone();
//     let distance = one.distance(two);
//     let theta1 = calc_theta(one - two, three - two);
//     let theta2 = calc_theta(two - three, four - three);
//     let mut orientation1 = ((two.y - one.y) / (two.x - one.x)).atan();
//     let mut orientation2 = ((three.y - two.y) / (three.x - two.x)).atan();
//     let mut orientation3 = ((two.y - three.y) / (two.x - three.x)).atan();
//     let mut orientation4 = ((four.y - three.y) / (four.x - three.x)).atan();

//     if orientation1 < 0. {
//       orientation1 += PI;
//     }

//     if orientation2 < 0. {
//       orientation2 += PI;
//     }

//     if orientation3 < 0. {
//       orientation3 += PI;
//     }

//     if orientation4 < 0. {
//       orientation4 += PI;
//     }

//     // this isn't right because this way, the bisector could be one of
//     // two possible orientations, since the crossing lines form an X and any
//     // of the inner corners qualify.
//     // probably need to bust out some slightly more developed math for this
//     // https://en.wikipedia.org/wiki/Angle_bisector_theorem
//     let mut o1 = vec![orientation1, orientation2, orientation3];
//     o1.sort_by(|a, b| a.partial_cmp(b).unwrap_or(std::cmp::Ordering::Equal));
//     let mut bisect1 = theta1 / 2. + o1.first().unwrap();

//     let point_on_bisect1 = pt2(two.x + bisect1.cos(), two.y + bisect1.sin());
//     // if calc_theta(point_on_bisect1 - two, one - two) == calc_theta(point_on_bisect1 - two, three - two) {
//     if !(orientation1 < bisect1 && bisect1 < orientation2) {
//       // ... adjust bisect1?????
//       // bisect1 = bisect1 + PI / 2.;
//       // bisect1 = theta1 / 2. + orientation1;
//     }

//     let mut o2 = vec![orientation2, orientation3, orientation4];
//     o2.sort_by(|a, b| a.partial_cmp(b).unwrap_or(std::cmp::Ordering::Equal));
//     let mut bisect2 = theta2 / 2. + o2.first().unwrap();

//     // let mut bisect1 = theta1 / 2. + orientation1;
//     // let mut bisect2 = theta2 / 2. + orientation4;

//     let point_on_bisect2 = pt2(two.x + bisect2.cos(), two.y + bisect2.sin());
//     // if calc_theta(point_on_bisect2 - three, two - three) == calc_theta(point_on_bisect2 - three, four - three) {
//     if !(orientation3 < bisect2 && bisect2 < orientation4) {
//       // ... adjust bisect2?????
//       // bisect2 = bisect2 + PI / 2.;
//       // bisect2 = theta2 / 2. + orientation4;
//     }

//     // let bisect1 = if bisect1 < 0. {
//     //   theta1 / 2. + orientation2
//     // } else { bisect1 };
//     // let bisect2 = if bisect2 < 0. {
//     //   theta2 / 2. + orientation4
//     // } else { bisect2};

//     println!(
//       "
//     theta1 {}
//     theta2 {}
//     orientation1 {}
//     orientation2 {}
//     orietnation4 {}
//     bisect1 {}
//     bisect2 {}
//     ",
//       theta1, theta2, orientation1, orientation2, orientation4, bisect1, bisect2
//     );
//     // let perpendicular = orientation1 + PI / 2.;

//     let length = 50.;

//     let points = vec![
//       pt2(
//         two.x - bisect1.cos() * length,
//         two.y - bisect1.sin() * length,
//       ),
//       pt2(
//         three.x - bisect2.cos() * length,
//         three.y - bisect2.sin() * length,
//       ),
//       pt2(
//         three.x + bisect2.cos() * length,
//         three.y + bisect2.sin() * length,
//       ),
//       pt2(
//         two.x + bisect1.cos() * length,
//         two.y + bisect1.sin() * length,
//       ),
//     ];

//     draw
//       .polygon()
//       .color(hsla(0., 0., 0., 0.3))
//       .stroke(hsla(0., 0., 0., 0.7))
//       .points(points);
//   });
// }

fn draw_segments(draw: &Draw, model: &Model, line: Vec<Point2>) {
  draw
    .polyline()
    .color(hsl(0.1, 0.5, 0.5))
    .points(line.clone());
  line.iter().enumerate().for_each(|(i, pt)| {
    if i >= line.len() - 3 {
      return;
    }
    let one = pt.clone();
    let two = line[i + 1].clone();
    let three = line[i + 2].clone();
    let four = line[i + 3].clone();
    let distance = one.distance(two);
    let v1 = one - two;
    let v2 = three - two;
    let theta1 = v1.angle_between(v2);
    let v3 = two - three;
    let v4 = four - three;
    let theta2 = v3.angle_between(v4);
    // let theta1 = calc_theta(one - two, three - two);
    // let theta2 = calc_theta(two - three, four - three);
    let mut orientation1 = ((two.y - one.y) / (two.x - one.x)).atan();
    let mut orientation2 = ((three.y - two.y) / (three.x - two.x)).atan();
    let mut orientation3 = ((two.y - three.y) / (two.x - three.x)).atan();
    let mut orientation4 = ((four.y - three.y) / (four.x - three.x)).atan();

    let mut bisect1 = theta1 / 2. + orientation1;
    let mut bisect2 = theta2 / 2. + orientation3;

    let length = 50.;

    let points = vec![
      one.rotate(theta1),
      two.rotate(theta2),
      two.rotate(theta2) * -1.0,
      one.rotate(theta1) * -1.0,
    ];

    draw
      .polygon()
      .color(hsla(0., 0., 0., 0.3))
      .stroke(hsla(0., 0., 0., 0.7))
      .points(points);
  });
}

/*
close, but bisectors are not quite right
fn draw_segments(draw: &Draw, model: &Model, line: Vec<Point2>) {
  draw
    .polyline()
    .color(hsl(0.1, 0.5, 0.5))
    .points(line.clone());
  line.iter().enumerate().for_each(|(i, pt)| {
    if i >= line.len() - 3 {
      return;
    }
    let one = pt.clone();
    let two = line[i + 1].clone();
    let three = line[i + 2].clone();
    let four = line[i + 3].clone();
    let distance = one.distance(two);
    let orientation1 = ((two.y - one.y) / (two.x - one.x)).atan();
    let orientation2 = ((two.y - three.y) / (two.x - three.x)).atan();
    let orientation3 = ((three.y - two.y) / (three.x - two.x)).atan();;
    let orientation4 = ((three.y - four.y) / (three.x - four.x)).atan();
    // this isn't right because this way, the bisector could be one of
    // two possible orientations, since the crossing lines form an X and any
    // of the inner corners qualify.
    // probably need to bust out some slightly more developed math for this
    // https://en.wikipedia.org/wiki/Angle_bisector_theorem
    let bisect1 = (orientation1 - orientation2) / 2.;
    let bisect2 = (orientation3 - orientation4) / 2.;
    // let perpendicular = orientation1 + PI / 2.;

    let length = 50.;

    let points = vec![
      pt2(two.x - bisect1.cos() * length, two.y - bisect1.sin() * length),
      pt2(three.x - bisect2.cos() * length, three.y - bisect2.sin() * length),
      pt2(three.x + bisect2.cos() * length, three.y + bisect2.sin() * length),
      pt2(two.x + bisect1.cos() * length, two.y + bisect1.sin() * length),
    ];

    draw.polygon().color(hsla(0., 0., 0., 0.3)).stroke(hsla(0., 0., 0., 0.7)).points(points);
  });
}
*/

/*
// this function draws rectangles with one axis that matches the orientation
// of the line segment on which the rectangle lies.
// This is OK, but I want it to shift with each vertex, so overlaps are avoided or minimized
fn draw_segments2(draw: &Draw, model: &Model, line: Vec<Point2>) {
  draw
    .polyline()
    .color(hsl(0.1, 0.5, 0.5))
    .points(line.clone());
  line.iter().enumerate().for_each(|(i, pt)| {
    if i == line.len() - 1 {
      return;
    }
    let start = pt.clone();
    let end = line[i + 1].clone();
    let distance = start.distance(end);
    let orientation = ((end.y - start.y) / (end.x - start.x)).atan();
    let perpendicular = orientation + PI / 2.;

    let length = 50.;

    let points = vec![
      pt2(start.x - perpendicular.cos() * length, start.y - perpendicular.sin() * length),
      pt2(end.x - perpendicular.cos() * length, end.y - perpendicular.sin() * length),
      pt2(end.x + perpendicular.cos() * length, end.y + perpendicular.sin() * length),
      pt2(start.x + perpendicular.cos() * length, start.y + perpendicular.sin() * length),
    ];

    draw.polygon().color(hsla(0., 0., 0., 0.3)).points(points);
  });
}
*/

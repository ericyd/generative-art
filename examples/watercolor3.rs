// See watercolor2 for notes on the process.
// cargo run --release --example watercolor3
//
// kind of an absurd amount of parameterization, but this is what was used for the "final"
// cargo run --release --example watercolor3 -- --loops 5 --n-polygons 30 --n-layers 30 --n-lines 329 --n-steps 690 --seed 4156.9765710838 --noise-scale 153.01631213664376 --stroke-weight 1.0 --ctrl1 -6.185910976184958 --ctrl2 4.8322466261624 --ctrl3 -5.379601209352063 --ctrl4 5.869373073646324 --ctrl5 -2.952850461752945 --radius 0.57792294
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::prelude::*;

mod util;
use nannou::noise::{NoiseFn, OpenSimplex, Terrace};
use util::args::ArgParser;
use util::color::palette_to_hsl;
use util::meander;
use util::{capture_model, captured_frame_path};

const PALETTE1: [&str; 4] = [
  "#FAAA0A", // orange1
  "#EB7C05", // orange2
  "#FB6D46", // orange3
  "#D20B04", // red1
];

const PALETTE2: [&str; 4] = [
  "#D63D52", // red1
  "#DD855F", // orange1
  "#E5AC38", // orange-yellow
  "#D95479", // pinkish
];

fn main() {
  nannou::app(model).update(update).run();
}

#[derive(Debug)]
struct Model {
  n_polygons: usize,
  n_layers: usize,
  n_lines: usize,
  n_steps: usize,
  seed: f64,
  noise_scale: f64,
  stroke_weight: f32,
  control_point1: f64,
  control_point2: f64,
  control_point3: f64,
  control_point4: f64,
  control_point5: f64,
  // unlike many other radius args, this one is a percentage of the diagonal distance of the window rect
  radius: f32,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  app
    .new_window()
    .size(args.get("w", 768), args.get("h", 1024))
    .title(app.exe_name().unwrap())
    .view(view)
    .build()
    .unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));

  // these values will be discarded after `update` runs
  Model {
    n_polygons: 30,
    n_layers: 20,
    n_lines: 100,
    n_steps: 400,
    seed: random_range(0.1, 10000.0),
    noise_scale: random_range(200.0, 400.0),
    stroke_weight: args.get("stroke-weight", 1.0),
    control_point1: random_range(-10.0, 0.0),
    control_point2: random_range(0.0, 10.0),
    control_point3: random_range(-10.0, 0.0),
    control_point4: random_range(0.0, 10.0),
    control_point5: random_range(-10.0, 0.0),
    radius: random_range(0.15, 0.6),
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  let args = ArgParser::new();
  model.n_polygons = args.get("n-polygons", 20);
  model.n_layers = args.get("n-layers", 20);
  model.n_lines = args.get("n-lines", random_range(300, 500));
  model.n_steps = args.get("n-steps", random_range(600, 800));
  model.seed = args.get("seed", random_range(0.1, 10000.0));
  model.noise_scale = args.get("noise-scale", random_range(150.0, 280.0));
  model.control_point1 = args.get("ctrl1", random_range(-10.0, 0.0));
  model.control_point2 = args.get("ctrl2", random_range(0.0, 10.0));
  model.control_point3 = args.get("ctrl3", random_range(-10.0, 0.0));
  model.control_point4 = args.get("ctrl4", random_range(0.0, 10.0));
  model.control_point5 = args.get("ctrl5", random_range(-10.0, 0.0));
  model.radius = args.get("radius", random_range(0.3, 0.7));
}

fn view(app: &App, model: &Model, frame: Frame) {
  let bg = IVORY;
  frame.clear(bg);
  let draw = app.draw();
  draw.background().color(bg);
  let win = app.window_rect();

  draw_shapes_layer1(&draw, model, &win);

  draw_field(&draw, model, &win);

  draw_shapes_layer2(&draw, model, &win);

  draw.to_frame(app, &frame).unwrap();
  app
    .main_window()
    .capture_frame(captured_frame_path(app, &frame));
  capture_model(app, &frame, model);
}

fn shape(n_verticies: i32, radius: f32, x: f32, y: f32) -> Vec<Point2> {
  (0..=n_verticies)
    .map(|n| {
      let angle = map_range(n, 0, n_verticies, 0.0, 2.0 * PI);
      pt2(angle.cos() * radius + x, angle.sin() * radius + y)
    })
    .collect()
}

fn generate_shapes(
  n_polygons: usize,
  n_layers: usize,
  win: &Rect,
  palette: Vec<&str>,
) -> Vec<(Vec<Point2>, Hsla, Point2)> {
  let palette = palette_to_hsl(palette);
  let n_colors = palette.len();
  (0..n_polygons)
    .map(|_| {
      let n_verticies = random_range(4, 9);
      let y_min = win.bottom() * 1.3;
      let y_max = win.top() * 1.3;
      let y = random_range(y_min, y_max);
      let x = random_range(win.left() * 1.3, win.right() * 1.3);
      let polygon = shape(
        n_verticies,
        random_range(win.w() / 5.0, win.w() / 2.0),
        x,
        y,
      );
      // polygon's y-position as a fraction of total height
      let y_pos = map_range(y, y_min, y_max, 0.0, 1.0);

      // map the y_pos to the length of the
      let color_index = map_range(y, y_min, y_max, 0, n_colors);

      // for each "color_index", the polygons should slightly fade from dark to light
      // as it gets closer to the next color
      let color_frac = 1.0 / n_colors as f32;
      let color_adjust = map_range(
        y_pos,
        color_frac * color_index as f32,
        color_frac * (color_index as f32 + 1.0),
        0.8,
        1.1,
      );

      // fetch color and modify it's components
      let (h, s, l) = palette[color_index].into_components();
      let h = h.to_positive_degrees() / 360.0;

      // small chance to grab nearly-black
      let color = if random_f32() < 0.1 {
        hsla(0.01, 0.01, 0.01, 1.0)
      } else {
        hsla(
          random_range(h * 0.9, h * 1.1),
          random_range(s * 0.9, s * 1.1),
          l * color_adjust,
          1.0 / n_layers as f32,
        )
      };
      (meander(&polygon, 5, 0.8), color, pt2(x, y))
    })
    .collect()
}

fn draw_shapes_layer1(draw: &Draw, model: &Model, win: &Rect) {
  let shapes = generate_shapes(model.n_polygons, model.n_layers, win, PALETTE1.to_vec());

  // This triple nested loop is a bit awkward, but the intention is to
  // cycle through each shape and draw them so they all overlap/intersect with each other.
  // Drawing several per cycle (the "layers" value) gives a bit more definition
  // to each shape while it's being blended.
  let layers = 3;
  let cycles = model.n_layers / layers;
  for _ in 0..cycles {
    for (shape, color, _center) in &shapes {
      for __ in 0..layers {
        draw.polygon().color(*color).points(meander(&shape, 5, 0.5));
      }
    }
  }
}

// same as draw_shapes_layer1 except only draw the shapes if the
// polygon center is above line y=x
fn draw_shapes_layer2(draw: &Draw, model: &Model, win: &Rect) {
  let shapes = generate_shapes(model.n_polygons / 3, model.n_layers, win, PALETTE2.to_vec());

  let layers = 3;
  let cycles = model.n_layers / layers;
  for _ in 0..cycles {
    for (shape, color, center) in &shapes {
      if center.y > center.x {
        for __ in 0..layers {
          draw.polygon().color(*color).points(meander(&shape, 5, 0.5));
        }
      }
    }
  }
}

fn noise_field<T: NoiseFn<[f64; 3]>>(
  seed: f64,
  noise_scale: f64,
  x: f32,
  y: f32,
  noisefn: &T,
) -> f32 {
  let noise = noisefn.get([x as f64 / noise_scale, y as f64 / noise_scale, seed]);
  map_range(noise, -1.0, 1.0, 0.0, 2.0 * PI)
}

fn draw_field(draw: &Draw, model: &Model, win: &Rect) {
  let color = hsl(0.0, 0.0, 0.02);
  let source = OpenSimplex::new();
  let noisefn = Terrace::new(&source)
    .add_control_point(model.control_point1)
    .add_control_point(model.control_point2)
    .add_control_point(model.control_point3)
    .add_control_point(model.control_point4)
    .add_control_point(model.control_point5);

  for _i in 0..=model.n_lines {
    // circular placement
    // let init_angle = map_range(_i, 0, model.n_lines, 0.0, 2.0 * PI);
    // let mut x = init_angle.cos() * win.left().hypot(win.bottom()) * model.radius;
    // let mut y = init_angle.sin() * win.left().hypot(win.bottom()) * model.radius;

    // random rectangular placement
    let diag = win.left().hypot(win.bottom());
    let mut x = random_range(diag * model.radius / 2.0, diag * -model.radius / 2.0);
    let mut y = random_range(diag * model.radius / 2.0, diag * -model.radius / 2.0);

    let _init_x = x;
    let init_y = y;

    // Generate points for the line that do not intersect other lines
    let points = (0..model.n_steps)
      .map(|_n| {
        // let angle = field(model, x, y);
        // hmm, not quite sure why this is necessary
        let angle = noise_field(model.seed, model.noise_scale, x, y, &noisefn);
        if init_y < 0.0 {
          x -= angle.cos();
          y -= angle.sin();
        } else {
          x += angle.cos();
          y += angle.sin();
        };

        let point = pt2(x, y);
        Some(point)
      })
      // Skip any initial Nones
      // Take all the Somes until we hit a None.
      .skip_while(|&o| o.is_none())
      .take_while(|&o| o.is_some())
      .filter_map(|o| o);

    draw
      .polyline()
      .caps_round()
      .weight(model.stroke_weight)
      .color(color)
      .points(points);
  }
}

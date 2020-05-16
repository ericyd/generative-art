// cargo run --release --example hex2
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::prelude::*;
use nannou::Draw;

mod util;
use util::args::ArgParser;
use util::captured_frame_path;
use util::color::*;
use util::hexagon::*;

const WIDTH: u32 = 1024;
const HEIGHT: u32 = 1024;

struct HexTree {
  node: Hexagon,
  children: Vec<HexTree>,
}

fn main() {
  nannou::app(model).update(update).run();
}

struct Model {
  radius: f32,
  depth: usize,
  padding: f32,
  palette: String,
  origin_x: f32,
  origin_y: f32,
  chance_of_children: f32,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  app
    .new_window()
    .size(WIDTH, HEIGHT)
    .title(app.exe_name().unwrap())
    .view(view)
    .build()
    .unwrap();
  app.set_loop_mode(LoopMode::loop_ntimes(args.get("loops", 1)));
  let win = app.window_rect();
  Model {
    padding: args.get("padding", random_range(0.0, 10.0)),
    radius: args.get("radius", random_range(20.0, 60.0)),
    depth: args.get("depth", 35),
    palette: args.get_string("palette", "random"),
    origin_x: args.get("x", win.left() / 2.0),
    origin_y: args.get("y", win.bottom() / 2.0),
    chance_of_children: args.get("chance", 0.05),
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  let args = ArgParser::new();
  model.palette = args.get_string("palette", "random");
  model.padding = args.get("padding", random_range(0.0, 10.0));
  model.radius = args.get("radius", random_range(20.0, 60.0));
}

fn view(app: &App, model: &Model, frame: Frame) {
  let palette = get_palette(&model.palette);

  let draw = app.draw();
  draw.background().color(IVORY);

  let tree = generate_tree(model);

  draw_tree(&draw, palette, tree);

  // Write to the window frame.
  draw.to_frame(app, &frame).unwrap();

  app
    .main_window()
    .capture_frame_threaded(captured_frame_path(app, &frame));
}

fn generate_tree(model: &Model) -> HexTree {
  let node = Hexagon::new(model.origin_x, model.origin_y, model.radius);
  let children = generate_children(&node, 0, model);
  HexTree { node, children }
}

fn generate_children(hex: &Hexagon, depth: usize, model: &Model) -> Vec<HexTree> {
  if depth > model.depth {
    return vec![];
  }
  let n_children = if random_f32() < model.chance_of_children {
    2
  } else {
    1
  };
  let mut available_angles: Vec<f32> = vec![
    PI / 6.0 * 1.0,
    PI / 6.0 * 3.0,
    PI / 6.0 * 5.0,
    // PI / 6.0 * 7.0,
    // PI / 6.0 * 9.0,
    PI / 6.0 * 11.0,
  ]; //.iter().enumerate().filter(|(i,_a)| (i + (depth & 1)) % 2 == 0).map(|(_, a)| *a).collect();

  (0..n_children)
    .map(|_n| {
      let angle_index = random_range(0, available_angles.len());
      let angle = available_angles[angle_index];
      available_angles.remove(angle_index);
      let node = next_hex(&hex, angle, model.padding);
      let children = generate_children(&node, depth + 1, model);
      HexTree { node, children }
    })
    .collect()
}

fn draw_tree(draw: &Draw, palette: [&str; 5], tree: HexTree) {
  draw
    .polygon()
    .x_y(tree.node.x, tree.node.y)
    .color(random_color(palette))
    .points(tree.node.points());
  for child in tree.children {
    draw_tree(draw, palette, child);
  }
}

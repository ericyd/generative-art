// implementation of deconbatch's "Poor man's DLA" in Rust
// https://www.deconbatch.com/2019/10/the-poor-mans-dla-diffusion-limited.html
// cargo run --release --example diffusion_3 -- --radius 50 --init 12 --weight 4.5 --size 4 --max-dist 3.3 --loops 1200 --palette pastel1 --animate true
extern crate chrono;
extern crate nannou;

use nannou::color::*;
use nannou::draw::Draw;
use nannou::prelude::*;

mod util;
use util::args::ArgParser;
use util::captured_frame_path;
use util::color::*;

const HEIGHT: u32 = 1024;
const WIDTH: u32 = 1024;
const TWO_PI: f32 = PI * 2.;

struct Tree {
  pub x: f32,
  pub y: f32,
  pub children: Vec<Tree>,
}

impl Tree {
  pub fn new(x: f32, y: f32) -> Self {
    Tree {
      x,
      y,
      children: Vec::<Tree>::new(),
    }
  }
}

fn main() {
  nannou::app(model).update(update).run();
}

struct Model {
  loops: usize,
  animation_rate: u64,
  point_size: f32,
  animate: bool,
  min_dist: f32,
  max_dist: f32,
  start_radius: f32,
  trees: Vec<Tree>,
  stroke_weight: f32,
  palette: String,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  let loops = args.get("loops", 7200);
  // this is the size of the starting "circle" on which the points are placed
  let start_radius = args.get("radius", 200.);
  let n_initial_points = args.get("init", random_range(2, 10));

  // initialize the cluster with some dots in the center
  let mut trees = Vec::new();
  for i in 0..n_initial_points {
    let factor = i as f32 / n_initial_points as f32;
    trees.push(Tree::new(
      start_radius * (TWO_PI * factor).cos(),
      start_radius * (TWO_PI * factor).sin(),
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
    start_radius,
    trees,
    point_size: args.get("size", 4.0),
    // should the drawing animate (as opposed to just drawing the final frame)?
    // and if so, how frequently should it update?
    animate: args.get("animate", false),
    animation_rate: args.get("rate", 50),
    // min/max dist control how close together the dots can be.
    // They scale the point_size, so 1.0 means "1x point_size"
    min_dist: args.get("min-dist", 1.0),
    max_dist: args.get("max-dist", 1.2),
    stroke_weight: args.get("weight", 1.0),
    palette: args.get_string("palette", "muzli8"),
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  let Model {
    trees,
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

    // Uncomment to avoid putting dots in the "center" of the circle.
    // if x.hypot(y) < model.start_radius {
    //   break;
    // }

    if check_tree_collision(trees, pt2(x, y), *point_size, *min_dist, *max_dist) {
      break;
    }
  }
}

fn view(app: &App, model: &Model, frame: Frame) {
  // Prepare to draw.
  let draw = app.draw();
  // let cream = hsl(47. / 360., 1., 0.98);
  let dark_blue = hsl(221. / 360., 0.58, 0.03);
  draw.background().color(dark_blue);

  // draw every `rate` frames if animating
  println!("{}", frame.nth()); // kind of nice to know things are running smoothly if it isn't animating
  if model.animate && frame.nth() > 0 && frame.nth() % model.animation_rate == 0 {
    draw_tree(&model.trees, model.stroke_weight, &model.palette, &draw);
    draw.to_frame(app, &frame).unwrap();
  }

  // draw final frame
  if frame.nth() == model.loops as u64 - 1 {
    draw_tree(&model.trees, model.stroke_weight, &model.palette, &draw);
    draw.to_frame(app, &frame).unwrap();
    app
      .main_window()
      .capture_frame(captured_frame_path(app, &frame));
  }
}

type Line = Vec<Point2>;

fn draw_tree(trees: &Vec<Tree>, stroke_weight: f32, palette: &String, draw: &Draw) {
  let palette = get_palette(&palette);
  for tree in trees {
    let color = random_color(palette);
    let line = vec![pt2(tree.x, tree.y)];
    let paths = build_paths(&line, &tree.children);
    for path in paths {
      draw
        .ellipse()
        .x_y(path[0].x, path[0].y)
        .w_h(stroke_weight * 2.25, stroke_weight * 2.25)
        .color(color);
      draw
        .polyline()
        .color(color)
        .caps_round()
        .join_round()
        .stroke_weight(stroke_weight)
        .points(path);
    }
  }
}

// I don't think this is a great pathfinding algorithm,
// but it seems to work, and I'm OK with that.
// Hopefully I will revisit this in the future.
fn build_paths(line: &Line, children: &Vec<Tree>) -> Vec<Line> {
  let mut paths = vec![];

  // for each child,
  // create a new line by concatenating `line` and the child point.
  // Then, recursively build all the paths from it's descendants.
  // If there are descendant paths, push those into the paths array.
  // If they are not descendant paths, just push the new line onto the array
  for (i, child) in children.iter().enumerate() {
    let path = if i > 0 {
      vec![pt2(child.x, child.y)]
    } else {
      [&line[..], &[pt2(child.x, child.y)]].concat()
    };

    // I would love a more elegant solve for this...
    if child.children.len() == 0 {
      paths.push(path);
    } else {
      for path in build_paths(&path, &child.children) {
        paths.push(path);
      }
    }
  }

  paths
}

/// check collision between a point and the cluster.
/// This becomes VERY inefficient as the tree becomes more deeply nested.
/// Would be prudent to consider a new way of determining "collision",
/// or a new data structure for determining collision.
fn check_tree_collision(
  trees: &mut Vec<Tree>,
  point: Point2,
  size: f32,
  min_dist: f32,
  max_dist: f32,
) -> bool {
  for tree in trees {
    if pt2(tree.x, tree.y).distance(pt2(point.x, point.y)) < size * min_dist {
      return false;
    }
    if pt2(tree.x, tree.y).distance(pt2(point.x, point.y)) < size * max_dist {
      tree.children.push(Tree::new(point.x, point.y));
      return true;
    }
    if check_tree_collision(&mut tree.children, point, size, min_dist, max_dist) {
      return true;
    }
  }
  false
}

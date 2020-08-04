extern crate nannou;

use nannou::prelude::*;
use nannou::Draw;

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

type Line = Vec<Point2>;

fn draw_tree(trees: &Vec<Tree>, draw: &Draw) {
  for tree in trees {
    let line = vec![pt2(tree.x, tree.y)];
    let paths = build_paths(&line, &tree.children);
    for path in paths {
      println!("path");
      printline(&path);
      draw.polyline().color(hsla(0.4, 0.5, 0.5, 1.0)).points(path);
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
  for child in children {
    let path = [&line[..], &[pt2(child.x, child.y)]].concat();

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

fn printline(line: &Vec<Point2>) {
  for pt in line {
    println!("{}, {}", pt.x, pt.y);
  }
}

fn main() {
  nannou::sketch(view).size(512, 512).run();
}

fn view(app: &App, frame: Frame) {
  let win = app.window_rect();
  app.set_loop_mode(LoopMode::loop_ntimes(1));

  // Prepare to draw.
  let draw = app.draw();

  // set background color
  draw.background().color(WHITE);

  // build our fake tree
  let mut tree = Tree::new(0., 0.);

  tree.children.push(Tree::new(30., 30.));
  tree.children[0].children.push(Tree::new(40., 60.));
  tree.children[0].children.push(Tree::new(28., 55.));
  tree.children[0].children[1]
    .children
    .push(Tree::new(34., 80.));

  tree.children.push(Tree::new(10., 30.));
  tree.children[1].children.push(Tree::new(-10., 60.));

  tree.children.push(Tree::new(-30., 30.));
  tree.children[2].children.push(Tree::new(-40., 60.));

  // draw it
  draw_tree(&vec![tree], &draw);

  // Write to the window frame.
  draw.to_frame(app, &frame).unwrap();
}

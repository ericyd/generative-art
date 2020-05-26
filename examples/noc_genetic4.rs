// https://natureofcode.com/book/chapter-9-the-evolution-of-code/
// https://github.com/nannou-org/nannou/blob/b174e64351a6debf7586900b12b4d42bb3fd06dc/nature_of_code/chp_09_genetic_algorithms/9_1_ga_shakespeare.rs

// left off on
// 9.10 Evolving Forces: Smart Rockets
// https://natureofcode.com/book/chapter-9-the-evolution-of-code/
extern crate chrono;
extern crate nannou;

use nannou::prelude::*;

mod util;

use util::args::ArgParser;

fn main() {
  nannou::app(model).update(update).run()
}

#[derive(Debug, Clone)]
struct DNA {
  genes: Vec<i32>,
  fitness: f32,
}

impl DNA {
  fn new(size: usize) -> Self {
    // random_range(32, 128).into()
    // random_ascii()
    let genes = (0..size).map(|_| random_range(0, 256)).collect();

    DNA {
      genes,
      fitness: 0.0,
    }
  }

  fn calc_fitness_1(mut self, target: &Vec<i32>) -> Self {
    let mut score = 0;
    for i in 0..self.genes.len() {
      if self.genes[i] == target[i] {
        score += 1;
      }
    }

    self.fitness = score as f32 / target.len() as f32;
    self
  }

  // this uses an exponential fit that is MUCH MUCH MUCH faster at finding a "solution".
  fn calc_fitness(mut self, target: &Vec<i32>) -> Self {
    // closer to the target number is better
    let r_fitness = 1.0 - ((self.genes[0] - target[0]).abs() as f32) / 255.0;
    let g_fitness = 1.0 - ((self.genes[1] - target[1]).abs() as f32) / 255.0;
    let b_fitness = 1.0 - ((self.genes[2] - target[2]).abs() as f32) / 255.0;

    let fitness = (r_fitness + b_fitness + g_fitness) / 3.0;

    self.fitness = fitness;
    self
  }

  fn crossover(&self, other: &Self) -> Self {
    let mut child = DNA::new(self.genes.len());

    // let midpoint = random_range(0, child.genes.len());
    for i in 0..child.genes.len() {
      // midpoint is the original implementation
      // if i > midpoint {

      // This method uses the "coin flip" methodology from `Exercise 9.5` from https://natureofcode.com/book/chapter-9-the-evolution-of-code/
      // > Rewrite the crossover function to use the “coin flipping” method instead,
      // > in which each gene has a 50% chance of coming from parent A and a 50% chance of coming from parent B.
      if random_f32() < 0.5 {
        child.genes[i] = self.genes[i];
      } else {
        child.genes[i] = other.genes[i];
      }
    }

    child
  }

  fn mutate(&mut self, mutation_rate: f32) {
    for i in 0..self.genes.len() {
      if random_f32() < mutation_rate {
        self.genes[i] = random_range(0, 256);
      }
    }
  }

  fn print(&self) {
    println!(
      "fitness: {}, rgb: {} {} {}",
      self.fitness, self.genes[0], self.genes[1], self.genes[2]
    );
  }
}

struct Population {
  population: Vec<DNA>,
  target: Vec<i32>,
  mating_pool: Vec<DNA>,
  mutation_rate: f32,
  finished: bool,
  perfect_score: f32,
}

impl Population {
  fn new(target: Vec<i32>, size: usize, mutation_rate: f32) -> Self {
    Population {
      population: (0..size)
        .map(|_| DNA::new(target.len()).calc_fitness(&target))
        .collect(),
      target,
      mating_pool: Vec::new(),
      mutation_rate,
      finished: false,
      perfect_score: 1.0,
    }
  }

  fn generate_mating_pool_simple(&self) -> Vec<DNA> {
    let mut pool = Vec::new();
    for dna in &self.population {
      for _ in 0..(dna.fitness * 100.0).floor() as i32 {
        pool.push(dna.clone());
      }
    }
    pool
  }

  // not sure this really works...
  fn generate_mating_pool_ordinals(&mut self) -> Vec<DNA> {
    let mut pool = Vec::new();
    self
      .population
      .sort_by(|a, b| a.fitness.partial_cmp(&b.fitness).unwrap());
    let n_population = self.population.len();
    for (i, dna) in self.population.iter().enumerate() {
      let qty = ((n_population - i) as f32 / (n_population * 2) as f32 * 100.0) as i32;
      // println!("qty {}", qty);
      for _ in 0..qty {
        pool.push(dna.clone());
      }
    }
    pool
  }

  fn generate_mating_pool(&mut self) {
    self.mating_pool.clear();
    // sorting the entire array is probably not the _best_ way of doing this, but it makes it easy to determine the max item
    self
      .population
      .sort_by(|a, b| a.fitness.partial_cmp(&b.fitness).unwrap());
    let max_fitness = self.population.last().unwrap().fitness;

    for dna in &self.population {
      let fitness = map_range(dna.fitness, 0.0, max_fitness, 0, 100);
      for _ in 0..fitness {
        self.mating_pool.push(dna.clone());
      }
    }
  }

  fn reproduce(&mut self) {
    for i in 0..self.population.len() {
      let a = random_range(0, self.mating_pool.len());
      let b = random_range(0, self.mating_pool.len());
      let parent_a = &self.mating_pool[a];
      let parent_b = &self.mating_pool[b];
      // ensure our parents are unique
      // this actually makes the algorithm substantially worse...
      // while parent_b.genes != parent_a.genes {
      //   parent_b = &self.mating_pool[random_range(0, self.mating_pool.len())];
      // }

      let mut child = parent_a.crossover(parent_b);
      child.mutate(self.mutation_rate);
      self.population[i] = child.calc_fitness(&self.target);
    }
  }

  fn get_best(&mut self) -> DNA {
    let best = self.population.last().unwrap();
    if best.fitness >= self.perfect_score {
      self.finished = true;
    }
    best.clone()
  }
}

struct Model {
  loops: usize,
  population: Population,
  best: DNA,
  nx: usize,
  ny: usize,
}

fn model(app: &App) -> Model {
  let args = ArgParser::new();
  let loops = args.get_usize("loops", 1);
  // app.set_loop_mode(LoopMode::loop_ntimes(loops));
  let nx = 512;
  let ny = 512;
  app
    .new_window()
    .view(view)
    .size(nx as u32, ny as u32)
    .build()
    .unwrap();
  let mutation_rate = args.get("mutation-rate", 0.01);
  let population_size = args.get("population-size", nx * ny);

  let target = vec![100, 145, 194];
  let population = Population::new(target, population_size, mutation_rate);
  let initial_best = population.population[0].clone();

  Model {
    loops,
    population,
    best: initial_best,
    nx,
    ny,
  }
}

fn update(_app: &App, model: &mut Model, _update: Update) {
  // model.population.print();
  model.population.generate_mating_pool();
  model.population.reproduce();
  model.best = model.population.get_best();
}

fn view(app: &App, model: &Model, frame: Frame) {
  let win = app.window_rect();
  let draw = app.draw();
  println!("{} generations", frame.nth());

  // if frame.nth() == model.loops as u64 - 1 {
  draw_population(&draw, model, &win);
  model.best.print();
  if model.population.finished || frame.nth() > 1000 {
    // model.population.print_best();
    app.set_loop_mode(LoopMode::loop_ntimes(1));
    // std::process::exit(0)
  }

  draw.to_frame(app, &frame).unwrap();
  // app
  //   .main_window()
  //   .capture_frame(captured_frame_path(app, &frame));
}

// TODO: might be cleaner to have dna.genes as an Array to allow destructuring
fn rgb_from_genes(genes: &Vec<i32>) -> Rgb {
  let r = genes[0] as f32 / 255.0;
  let g = genes[1] as f32 / 255.0;
  let b = genes[2] as f32 / 255.0;
  rgb(r, g, b)
}

// with a 2D array represented in 1D, the indices correspond by:
// idx(x, y) = y * nx + x
// so we have to go backwards to find the relevant (x,y) from the index
fn x_y_from_index(i: usize, nx: usize, x_start: f32, y_start: f32) -> (f32, f32) {
  let x = i % nx;
  let y = (i - x) / nx;
  let x = x as f32 + x_start;
  let y = y as f32 + y_start;
  (x, y)
}

fn draw_population(draw: &Draw, model: &Model, win: &Rect) {
  // for (i, dna) in model.population.population.iter().enumerate() {
  for i in 0..model.population.population.len() {
    let dna = &model.population.population[i];
    let (x, y) = x_y_from_index(i, model.nx, win.left(), win.bottom());

    let color = rgb_from_genes(&dna.genes);
    draw
      .rect()
      .x_y(x as f32, y as f32)
      .w_h(1.0, 1.0)
      .color(color);
  }
}

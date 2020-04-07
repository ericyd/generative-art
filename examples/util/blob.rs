use super::interp::{Interp, Interpolate};
use nannou::draw::Draw;
use nannou::noise::{NoiseFn, Perlin};
use nannou::prelude::*;
use std::fmt::{self, Display, Formatter};

#[derive(Debug)]
pub struct Blob {
  x: f32,
  y: f32,
  noise_scale: f32,
  width: f32,
  height: f32,
  seed: f32,
  // this is a janky way of declaring the hsla color space,
  // but honestly Rust's type system is so annoying some times,
  // I'm just trying to be quick-n-dirty here
  color: [f32; 4],
  fuzziness: f32,
  rotation: f32,
}

impl Blob {
  pub fn new() -> Blob {
    Blob {
      x: 0.0,
      y: 0.0,
      noise_scale: Interp::lin(0.5, 1.0, random_f32()),
      width: 1.0,
      height: 1.0,
      seed: Interp::lin(1.0, 10.0.powi(11), random_f32()),
      color: [random_f32(), random_f32(), random_f32(), random_f32()],
      fuzziness: 0.0,
      rotation: 0.,
    }
  }

  pub fn draw(&self, draw: &Draw) {
    draw
      .polygon()
      .hsla(self.color[0], self.color[1], self.color[2], self.color[3])
      .points(self.points());
  }

  pub fn points(&self) -> Vec<Vector2> {
    let perlin = Perlin::new();

    // **heavily** borrowed from https://observablehq.com/@makio135/generating-svgs/10
    (0..=360)
      .map(|i| {
        let angle = deg_to_rad(i as f32);
        let cos = angle.cos();
        let sin = angle.sin();
        // the `* 0.5 + 0.5` factor makes the resulting blob more uniformly convex,
        // though I'm not sure if that's desirable or not
        let noise = perlin.get([cos as f64, sin as f64, self.seed as f64]) as f32 * 0.5 + 0.5;

        // alternative using radius and "stretch" rather than explicit width and height
        // let r = self.radius * (1.0 + noise * (self.noise_scale.powf(3.0)));
        // let x = (cos * r + self.x) * self.x_stretch;
        // let y = (sin * r + self.y) * self.y_stretch;

        let r = 1.0 + noise * (self.noise_scale.powf(3.0));
        let x = cos * r * self.width + self.x;
        let y = sin * r * self.height + self.y;
        let (x, y) = self.rotate(x, y);
        // add fuzziness. If fuzziness is 0, no fuzziness is applied
        let x =
          x + perlin.get([x as f64, y as f64, self.seed as f64]).cos() as f32 * self.fuzziness;
        let y =
          y + perlin.get([x as f64, y as f64, self.seed as f64]).sin() as f32 * self.fuzziness;
        pt2(x, y)
      })
      .collect()
  }

  pub fn x_y(mut self, x: f32, y: f32) -> Self {
    self.x = x;
    self.y = y;
    self
  }

  pub fn noise_scale(mut self, noise_scale: f32) -> Self {
    self.noise_scale = noise_scale;
    self
  }

  pub fn width(mut self, width: f32) -> Self {
    self.width = width;
    self
  }

  pub fn height(mut self, height: f32) -> Self {
    self.height = height;
    self
  }

  // this is a bit of a misnomer, but since we use a distorted circle
  // as the base for blobs it works as a quick way to scale
  pub fn radius(mut self, radius: f32) -> Self {
    self.width = radius / 2.0;
    self.height = radius / 2.0;
    self
  }

  pub fn w_h(mut self, width: f32, height: f32) -> Self {
    self.width = width;
    self.height = height;
    self
  }

  // useful if you want to control the noise pattern of the blob
  pub fn seed(mut self, seed: f32) -> Self {
    self.seed = seed;
    self
  }

  pub fn color(mut self, color: [f32; 4]) -> Self {
    self.color = color;
    self
  }

  pub fn hsla(mut self, h: f32, s: f32, l: f32, a: f32) -> Self {
    self.color = [h, s, l, a];
    self
  }

  pub fn fuzziness(mut self, fuzziness: f32) -> Self {
    self.fuzziness = fuzziness;
    self
  }

  pub fn rotate_rad(mut self, rotation: f32) -> Self {
    self.rotation = rotation;
    self
  }

  pub fn rotate_deg(mut self, rotation: f32) -> Self {
    self.rotation = deg_to_rad(rotation);
    self
  }

  // SO FTW https://stackoverflow.com/a/2259502
  fn rotate(&self, x: f32, y: f32) -> (f32, f32) {
    let sin = self.rotation.sin();
    let cos = self.rotation.cos();

    // translate point back to origin:
    let x = x - self.x;
    let y = y - self.y;

    // rotate point
    let xnew = x * cos - y * sin;
    let ynew = x * sin + y * cos;

    // translate point back:
    let x = xnew + self.x;
    let y = ynew + self.y;
    (x, y)
  }
}

impl Display for Blob {
  fn fmt(&self, f: &mut Formatter) -> fmt::Result {
    write!(f, "Blob<>")
  }
}

use super::chance;
use nannou::prelude::*;

#[derive(Debug, Clone)]
pub struct GravitationalBody {
  pub mass: f32,
  pub x: f32,
  pub y: f32,
}

impl GravitationalBody {
  pub fn new(mass: f32, x: f32, y: f32) -> Self {
    Self { mass, x, y }
  }

  // Simple generator with some usable defaults
  pub fn generate() -> Self {
    let mass = random_range(125.0, 300.0);
    let x = random_range(-500.0, 500.0);
    let y = random_range(-500.0, 500.0);

    Self { mass, x, y }
  }

  // Generate GravitationalBodies with a given mass range,
  // x range, and y range.
  // When allow_negative_mass is true, masses may randomly
  // be generated in the negative of the range provided.
  pub fn generate_with(
    mass_range: std::ops::Range<f32>,
    allow_negative_mass: bool,
    x_range: std::ops::Range<f32>,
    y_range: std::ops::Range<f32>,
  ) -> Self {
    let mass = if allow_negative_mass && chance() {
      random_range(-mass_range.start, -mass_range.end)
    } else {
      random_range(mass_range.start, mass_range.end)
    };
    let x = random_range(x_range.start, x_range.end);
    let y = random_range(y_range.start, y_range.end);
    Self { mass, x, y }
  }

  // Generates GravitationalBodies within a given mass range
  // (and optionally negative masses)
  // as well as using a given Rect for bounds.
  // When `outside` is true, the GravitationalBodies will be placed
  // outside of the Rect. When false, they will be inside the Rect.
  pub fn generate_from_rect(
    mass_range: std::ops::Range<f32>,
    allow_negative_mass: bool,
    win: &Rect,
    outside: bool,
  ) -> Self {
    let mass = if allow_negative_mass && chance() {
      random_range(-mass_range.start, -mass_range.end)
    } else {
      random_range(mass_range.start, mass_range.end)
    };

    let x_is_outside = outside && chance();
    let y_is_outside = outside && !x_is_outside;
    let x = if x_is_outside && chance() {
      random_range(win.left() * 1.05, win.left() * 1.4)
    } else if x_is_outside {
      random_range(win.right() * 1.05, win.right() * 1.4)
    } else {
      random_range(win.left(), win.right())
    };

    let y = if y_is_outside && chance() {
      random_range(win.bottom() * 1.05, win.bottom() * 1.4)
    } else if y_is_outside {
      random_range(win.top() * 1.05, win.top() * 1.4)
    } else {
      random_range(win.bottom(), win.top())
    };

    Self { mass, x, y }
  }

  // This corresponds to the "radius" in most Gravitational force equations
  fn distance(&self, x: f32, y: f32) -> f32 {
    (x - self.x).hypot(y - self.y)
  }

  // Assuming the GravitationalBody is the origin, calculate
  // the angle to the given point at (x,y)
  fn angle(&self, x: f32, y: f32) -> f32 {
    ((y - self.y) / (x - self.x)).atan()
  }

  // Based on
  // https://en.wikipedia.org/wiki/Newton%27s_law_of_universal_gravitation
  // However, not sure if the radius (self.distance) should be raised to the
  // 2nd or 3rd power. In the Three body problem, it is raised to the third power.
  // Should the power be equal to the number of bodies in the problem?
  // Gut says "yes" but experimentally it doesn't improve the look so I'm going to skip it.
  fn force_x(&self, g: f32, _n_bodies: i32, x: f32, y: f32) -> f32 {
    -g * self.mass * (x - self.x) / self.distance(x, y).powi(3) * self.angle(x, y).cos()
  }

  // logic would dictate that force_y should be multiplied by `self.angle().sin()`,
  // but for some reason that's causing some weird discontinuities in the resulting
  // field, so we're going to use `self.angle().cos()` for this field too.
  fn force_y(&self, g: f32, _n_bodies: i32, x: f32, y: f32) -> f32 {
    -g * self.mass * (y - self.y) / self.distance(x, y).powi(3) * self.angle(x, y).cos()
  }
}

/// GravitySystems encompass a gravitational constant (g)
/// and a set of GravitationalBodies.
/// The system can easily calculate the component force vectors that
/// would act on a point mass at any position.
#[derive(Debug, Clone)]
pub struct GravitySystem {
  // "force of gravity"
  g: f32,
  // list of gravitational bodies in the system
  pub bodies: Vec<GravitationalBody>,
  n_bodies: i32,
}

impl GravitySystem {
  pub fn new(g: f32, bodies: Vec<GravitationalBody>) -> Self {
    let n_bodies = bodies.len() as i32;
    Self {
      g,
      bodies,
      n_bodies,
    }
  }

  /// Simple generator with fixed `g` and randomized GravitationalBodies
  pub fn generate(n_bodies: i32) -> Self {
    let bodies: Vec<GravitationalBody> = (0..n_bodies)
      .map(|_| GravitationalBody::generate())
      .collect();
    Self {
      g: 10.0.powi(3),
      bodies,
      n_bodies,
    }
  }

  /// Set the gravitational constant for the system
  pub fn g(mut self, g: f32) -> Self {
    self.g = g;
    self
  }

  /// Set the gravitational bodies for the system
  pub fn bodies(mut self, bodies: Vec<GravitationalBody>) -> Self {
    self.bodies = bodies;
    self
  }

  /// Calculate the x force component
  fn force_x(&self, x: f32, y: f32) -> f32 {
    self.bodies.iter().fold(0.0, |force, body| {
      force + body.force_x(self.g, self.n_bodies, x, y)
    })
  }

  /// Calculate the y force component
  fn force_y(&self, x: f32, y: f32) -> f32 {
    self.bodies.iter().fold(0.0, |force, body| {
      force + body.force_y(self.g, self.n_bodies, x, y)
    })
  }

  /// Calculate the sum of all gravitational forces on the point.
  /// Returns a tuple of (Force_x, Force_y)
  /// where each force component is normalized
  /// Based on the "Restricted three body problem"
  /// References:
  ///   https://en.wikipedia.org/wiki/Three-body_problem
  ///   https://physics.stackexchange.com/questions/17285/split-gravitational-force-into-x-y-and-z-componenets
  pub fn force(&self, x: f32, y: f32) -> (f32, f32) {
    let f_x = self.force_x(x, y);
    let f_y = self.force_y(x, y);
    let unit_vec = f_x.hypot(f_y);
    (f_x / unit_vec, f_y / unit_vec)
  }
}

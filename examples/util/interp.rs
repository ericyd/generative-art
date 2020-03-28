use nannou::prelude::*;
use std::f64::consts::E;

pub struct Interp {}

pub trait Interpolate<T> {
  fn lin(start: T, end: T, x: T) -> T;
  fn exp(start: T, end: T, x: T) -> T;
  fn reverse_exp(start: T, end: T, x: T) -> T;
  fn euler(start: T, end: T, x: T) -> T;
  fn sin(start: T, end: T, x: T) -> T;
  fn sin_3(start: T, end: T, x: T) -> T;
  fn sin_4(start: T, end: T, x: T) -> T;
  fn cos(start: T, end: T, x: T) -> T;
  fn inv(start: T, end: T, x: T) -> T;
  fn log(start: T, end: T, x: T) -> T;
}

impl Interpolate<f64> for Interp {
  fn lin(start: f64, end: f64, x: f64) -> f64 {
    start + x * (end - start)
  }
  
  fn exp(start: f64, end: f64, x: f64) -> f64 {
    start + (x * x) * (end - start)
  }

  fn reverse_exp(start: f64, end: f64, x: f64) -> f64 {
    start + (1.0 - (x * x)) * (end - start)
  }

  fn euler(start: f64, end: f64, x: f64) -> f64 {
    start + E.powf(-1.0 * x) * (end - start)
  }

  fn sin(start: f64, end: f64, x: f64) -> f64 {
    start + (x * PI as f64).sin() * (end - start)
  }
  
  fn sin_3(start: f64, end: f64, x: f64) -> f64 {
    start + ((x * PI as f64 - (PI as f64 / 2.0)).sin().powi(3) / 2.0 + 0.5) * (end - start)
  }

  // not really "ready for production" but getting warmer for sure
  fn sin_4(start: f64, end: f64, x: f64) -> f64 {
    start + ((x * PI as f64 * 0.8 - (PI as f64 * 0.8 / 2.0)).sin().powi(3) * 0.6 + 0.516) * (end - start)
  }

  fn cos(start: f64, end: f64, x: f64) -> f64 {
    start + ((x * PI as f64).cos() / 2.0 + 0.5) * (end - start)
  }
  
  fn inv(start: f64, end: f64, x: f64) -> f64 {
    start + (1.0 / x) * (end - start)
  }

  fn log(start: f64, end: f64, x: f64) -> f64 {
    start + x.log10() * (end - start)
  }
}

impl Interpolate<f32> for Interp {
  fn lin(start: f32, end: f32, x: f32) -> f32 {
    start + x * (end - start)
  }
  
  fn exp(start: f32, end: f32, x: f32) -> f32 {
    start + (x * x) * (end - start)
  }

  fn reverse_exp(start: f32, end: f32, x: f32) -> f32 {
    start + (1.0 - (x * x)) * (end - start)
  }

  fn euler(start: f32, end: f32, x: f32) -> f32 {
    start + (E as f32).powf(-1.0 * x) * (end - start)
  }

  fn sin(start: f32, end: f32, x: f32) -> f32 {
    start + (x * PI as f32).sin() * (end - start)
  }
  
  fn sin_3(start: f32, end: f32, x: f32) -> f32 {
    start + ((x * PI - (PI / 2.0)).sin().powi(3) / 2.0 + 0.5) * (end - start)
  }

  // not really "ready for production" but getting warmer for sure
  fn sin_4(start: f32, end: f32, x: f32) -> f32 {
    start + ((x * PI * 0.8 - (PI * 0.8 / 2.0)).sin().powi(3) * 0.6 + 0.516) * (end - start)
  }

  fn cos(start: f32, end: f32, x: f32) -> f32 {
    start + ((x * PI).cos() / 2.0 + 0.5) * (end - start)
  }
  
  fn inv(start: f32, end: f32, x: f32) -> f32 {
    start + (1.0 / x) * (end - start)
  }

  fn log(start: f32, end: f32, x: f32) -> f32 {
    start + x.log10() * (end - start)
  }
}

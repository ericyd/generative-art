// AGAINST ALL ODDS, THIS WORKS.....
use std::collections::HashMap;
use std::fmt::{self, Display, Formatter};

#[derive(Debug)]
pub struct ArgParser {
  map: HashMap<String, String>,
}

// simple argument collector
impl ArgParser {
  pub fn new() -> Self {
    let args: Vec<String> = std::env::args().collect();
    let map = args
      .iter()
      .enumerate()
      .fold(HashMap::new(), |mut mut_map, (i, arg)| {
        match arg.get(0..2) {
          Some(slice) if Some(slice) == Some("--") => {
            if i >= std::env::args().len() - 1 {
              mut_map
            } else {
              // must create String from &str arg in order to take ownership
              mut_map.insert(String::from(arg.get(2..).unwrap()), args[i + 1].clone());
              mut_map
            }
          }
          _ => mut_map,
        }
      });

    ArgParser { map }
  }

  pub fn get_f32(&self, key: &str, default: f32) -> f32 {
    match self.map.get(&String::from(key)) {
      Some(num) => num.parse::<f32>().unwrap(),
      None => default,
    }
  }

  pub fn get_f64(&self, key: &str, default: f64) -> f64 {
    match self.map.get(&String::from(key)) {
      Some(num) => num.parse::<f64>().unwrap(),
      None => default,
    }
  }

  pub fn get_i32(&self, key: &str, default: i32) -> i32 {
    match self.map.get(&String::from(key)) {
      Some(num) => num.parse::<i32>().unwrap(),
      None => default,
    }
  }

  pub fn get_usize(&self, key: &str, default: usize) -> usize {
    match self.map.get(key) {
      Some(num) => num.parse::<usize>().unwrap(),
      None => default,
    }
  }

  pub fn get_bool(&self, key: &str, default: bool) -> bool {
    match self.map.get(&String::from(key)) {
      Some(num) => num.parse::<bool>().unwrap(),
      None => default,
    }
  }

  pub fn get_string(&self, key: &str, default: &str) -> String {
    match self.map.get(&String::from(key)) {
      Some(thing) => thing.to_string(),
      None => default.to_string(),
    }
  }
}

impl Display for ArgParser {
  fn fmt(&self, f: &mut Formatter) -> fmt::Result {
    let key_values = self
      .map
      .iter()
      .fold(Vec::<String>::new(), |vec, (k, v)| {
        [&vec[..], &vec![format!("{}: {}", k, v)]].concat()
      })
      .join(", ");
    write!(f, "ArgParser <{}>", key_values)
  }
}

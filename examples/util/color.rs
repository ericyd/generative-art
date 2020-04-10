use nannou::prelude::*;

// this is such a nice place to get simple color palettes
// https://observablehq.com/@makio135/give-me-colors

type Palette5<'a> = [&'a str; 5];

pub const PALETTES: [(&str, Palette5); 2] = [
  (
    "pink green yellow",
    ["#f9b4ab", "#fdebd3", "#264e70", "#679186", "#bbd4ce"],
  ),
  (
    "red orange blue",
    ["#69D2E7", "#A7DBD8", "#E0E4CC", "#F38630", "#FA6900"],
  ),
];

// honestly not sure if this is a good way of doing this or not
pub fn get_palette(name: &str) -> Palette5 {
  *PALETTES
    .to_vec()
    .iter()
    .filter(|(title, _palette)| &name == title)
    .map(|(_title, palette)| palette)
    .nth(0)
    .unwrap()
}

pub fn random_color(palette: Palette5) -> Hsl {
  select_random(&palette_to_hsl(palette.to_vec()))
}

pub fn random_hsl(palette: Vec<Hsl>) -> Hsl {
  select_random(&palette)
}

// convenience method to select a random element from an array
fn select_random<T: Copy>(vec: &Vec<T>) -> T {
  vec[(random_f32() * vec.len() as f32).floor() as usize]
}

// Could consider accepting and returning a Result,
// but it feels like overkill for this use case
fn hex_to_f32(hex: &str) -> f32 {
  i32::from_str_radix(hex, 16).unwrap() as f32
}

pub fn palette_to_hsl(palette: Vec<&str>) -> Vec<Hsl> {
  palette
    .iter()
    .map(|palette| {
      let r = hex_to_f32(palette.get(1..3).unwrap()) / 255.;
      let g = hex_to_f32(palette.get(3..5).unwrap()) / 255.;
      let b = hex_to_f32(palette.get(5..7).unwrap()) / 255.;
      Hsl::from(rgb(r, g, b))
    })
    .collect()
}

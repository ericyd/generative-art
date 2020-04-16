use nannou::prelude::*;

// this is such a nice place to get simple color palettes
// https://observablehq.com/@makio135/give-me-colors

pub type Palette5<'a> = [&'a str; 5];

pub const PALETTES: [(&str, Palette5); 38] = [
  (
    "pink green yellow",
    ["#f9b4ab", "#fdebd3", "#264e70", "#679186", "#bbd4ce"],
  ),
  (
    "red orange blue",
    ["#69D2E7", "#A7DBD8", "#E0E4CC", "#F38630", "#FA6900"],
  ),
  (
    "muzli1",
    ["#8a00d4", "#d527b7", "#f782c2", "#f9c46b", "#e3e3e3"],
  ),
  (
    "muzli2",
    ["#e74645", "#fb7756", "#facd60", "#fdfa66", "#1ac0c6"],
  ),
  (
    "muzli3",
    ["#454d66", "#309975", "#58b368", "#dad873", "#efeeb4"],
  ),
  (
    "muzli4",
    ["#272643", "#ffffff", "#e3f6f5", "#bae8e8", "#2c698d"],
  ),
  (
    "muzli5",
    ["#361d32", "#543c52", "#f55951", "#edd2cb", "#f1e8e6"],
  ),
  (
    "muzli6",
    ["#072448", "#54d2d2", "#ffcb00", "#f8aa4b", "#ff6150"],
  ),
  (
    "muzli7",
    ["#12492f", "#0a2f35", "#f56038", "#f7a325", "#ffca7a"],
  ),
  (
    "muzli8",
    ["#122c91", "#2a6fdb", "#48d6d2", "#81e9e6", "#fefcbf"],
  ),
  (
    "muzli9",
    ["#27104e", "#64379f", "#9854cb", "#ddacf5", "#75e8e7"],
  ),
  (
    "muzli10",
    ["#f7a400", "#3a9efd", "#3e4491", "#292a73", "#1a1b4b"],
  ),
  (
    "muzli11",
    ["#343090", "#5f59f7", "#6592fd", "#44c2fd", "#8c61ff"],
  ),
  (
    "muzli12",
    ["#1f306e", "#553772", "#8f3b76", "#c7417b", "#f5487f"],
  ),
  (
    "muzli13",
    ["#e0f0ea", "#95adbe", "#574f7d", "#503a65", "#3c2a4d"],
  ),
  // duplicate of "pink green yellow"
  (
    "muzli14",
    ["#f9b4ab", "#fdebd3", "#264e70", "#679186", "#bbd4ce"],
  ),
  (
    "muzli15",
    ["#492b7c", "#301551", "#ed8a0a", "#f6d912", "#fff29c"],
  ),
  (
    "muzli16",
    ["#ffa822", "#134e6f", "#ff6150", "#1ac0c6", "#dee0e6"],
  ),
  (
    "colorlovers1",
    ["#69D2E7", "#A7DBD8", "#E0E4CC", "#F38630", "#FA6900"],
  ),
  (
    "colorlovers2",
    ["#FE4365", "#FC9D9A", "#F9CDAD", "#C8C8A9", "#83AF9B"],
  ),
  (
    "colorlovers3",
    ["#ECD078", "#D95B43", "#C02942", "#542437", "#53777A"],
  ),
  (
    "colorlovers4",
    ["#556270", "#4ECDC4", "#C7F464", "#FF6B6B", "#C44D58"],
  ),
  (
    "colorlovers5",
    ["#774F38", "#E08E79", "#F1D4AF", "#ECE5CE", "#C5E0DC"],
  ),
  (
    "colorlovers6",
    ["#E8DDCB", "#CDB380", "#036564", "#033649", "#031634"],
  ),
  (
    "colorlovers7",
    ["#490A3D", "#BD1550", "#E97F02", "#F8CA00", "#8A9B0F"],
  ),
  (
    "colorlovers8",
    ["#594F4F", "#547980", "#45ADA8", "#9DE0AD", "#E5FCC2"],
  ),
  (
    "colorlovers9",
    ["#00A0B0", "#6A4A3C", "#CC333F", "#EB6841", "#EDC951"],
  ),
  (
    "colorlovers10",
    ["#E94E77", "#D68189", "#C6A49A", "#C6E5D9", "#F4EAD5"],
  ),
  (
    "colorlovers11",
    ["#3FB8AF", "#7FC7AF", "#DAD8A7", "#FF9E9D", "#FF3D7F"],
  ),
  (
    "colorlovers12",
    ["#D9CEB2", "#948C75", "#D5DED9", "#7A6A53", "#99B2B7"],
  ),
  (
    "colorlovers13",
    ["#FFFFFF", "#CBE86B", "#F2E9E1", "#1C140D", "#CBE86B"],
  ),
  (
    "colorlovers14",
    ["#EFFFCD", "#DCE9BE", "#555152", "#2E2633", "#99173C"],
  ),
  (
    "colorlovers15",
    ["#343838", "#005F6B", "#008C9E", "#00B4CC", "#00DFFC"],
  ),
  (
    "colorlovers16",
    ["#413E4A", "#73626E", "#B38184", "#F0B49E", "#F7E4BE"],
  ),
  (
    "colorlovers17",
    ["#FF4E50", "#FC913A", "#F9D423", "#EDE574", "#E1F5C4"],
  ),
  (
    "colorlovers18",
    ["#99B898", "#FECEA8", "#FF847C", "#E84A5F", "#2A363B"],
  ),
  (
    "colorlovers19",
    ["#655643", "#80BCA3", "#F6F7BD", "#E6AC27", "#BF4D28"],
  ),
  (
    "colorlovers20",
    ["#00A8C6", "#40C0CB", "#F9F2E7", "#AEE239", "#8FBE00"],
  ),
];

// honestly not sure if this is a good way of doing this or not
pub fn get_palette(name: &str) -> Palette5 {
  match name {
    name if name == "random" => {
      let palette = select_random(
        &PALETTES
          .to_vec()
          .iter()
          .map(|(_title, palette)| *palette)
          .collect(),
      );
      println!("palette: {}", palette.to_vec().join(", "));
      palette
    }
    _ => *PALETTES
      .to_vec()
      .iter()
      .filter(|(title, _palette)| &name == title)
      .map(|(_title, palette)| palette)
      .nth(0)
      .unwrap(),
  }
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

// https://github.com/mitchmindtree/nannou-sketches/blob/master/aquatic-juice/src/main.rs

// cargo run --release has substantially better performance with large amount of values

// docs: https://docs.rs/nannou/0.13.1/nannou/
// prelud: https://docs.rs/nannou/0.13.1/nannou/prelude/index.html

extern crate nannou;

use nannou::color::*;
use nannou::noise::{NoiseFn, Perlin};
use nannou::prelude::*;
use std::path::Path;

fn main() {
  nannou::sketch(view).run();
}

fn view(app: &App, frame: Frame) {
  // Handy constants.
  let t = app.duration.since_start.secs() as f64;
  let win = app.window_rect();
  let mid_to_corner = win.xy().distance(win.top_right());
  let pi2 = PI * 2.0;
  //   app.set_loop_mode(LoopMode::NTimes {
  //     number_of_updates: 2,
  //   });

  let num_steps = 10;

  let default_length = 1.0;

  let noise_scale = 500.0;
  let seed = 15848324.0; // Date.now() // 1584832432240
                         // let TAU = pi2;

  // Prepare to draw.
  let draw = app.draw();

  // Clear the background to black.
  draw.background().color(BLACK);

  let resolution = 45;

  for i in 0..resolution {
    let x_factor = i as f32 / resolution as f32;

    for j in 0..resolution {
      let y_factor = j as f32 / resolution as f32;

      // // this works!
      // draw.ellipse()
      //     .x_y(x, y)
      //     .resolution(200)
      //     .radius(2.0)
      //     .hsla(0.8, 0.6, 0.5, 0.5);

      let mut current_x = win.x.lerp(x_factor);
      let mut current_y = win.y.lerp(y_factor);

      let mut points = Vec::new();

      for n in 0..num_steps {
        let step_factor = n as f32 / num_steps as f32;

        // wow, multiplying this by a factor like 3 makes a spiral kind of shape <:o
        let x_component = current_x / win.w() * 4.0;
        let y_component = current_y / win.h() * 4.0;

        let angle = Perlin::new().get([
          (current_x / noise_scale) as f64,
          (current_y / noise_scale) as f64,
          t,
        ]) * pi2 as f64;

        // let angle = ((x_factor * step_factor).powi(6) + (y_factor * step_factor).powi(3)).sqrt() * pi2;
        // let angle = ((x_component).powi(2) + (y_component).powi(2)).sqrt() * pi2;
        let new_x = angle.cos() * default_length + current_x as f64;
        let new_y = angle.sin() * default_length + current_y as f64;

        // not sure how to use this Alpha struct
        // let color = Alpha::new(hue = RgbHue::from_radians(step_factor * pi2), saturation = 0.9, lightness = 0.5, alpha = 0.6);
        let color = Hsl::new(RgbHue::from_radians(step_factor * pi2), 0.9, 0.5);
        points.push((pt2(new_x as f32, new_y as f32), color));
        current_x = new_x as f32;
        current_y = new_y as f32;
      }

      draw.polyline().weight(0.5).points_colored(points);
    }
  }

  // Write to the window frame.
  draw.to_frame(app, &frame).unwrap();
  //   app.main_window().capture_frame(Path::new("/Users/eric/misc/generative-art/rust/flow-field-1/nannou1.jpeg"));
  //   app.main_window().capture_frame(Path::new("./nannou11.png"));
  //   app.main_window().capture_frame("/Users/eric/misc/generative-art/rust/flow-field-1/nannou1.png");
  //   app.main_window().capture_frame_threaded(Path::new("/Users/eric/misc/generative-art/rust/flow-field-1/nannou2.jpeg"));
  //   app.main_window().capture_frame_threaded("/Users/eric/misc/generative-art/rust/flow-field-1/nannou2.png");

  // https://github.com/nannou-org/nannou/blob/master/examples/simple_capture.rs
  // Capture the frame!
  //
  // NOTE: You can speed this up with `capture_frame_threaded`, however be aware that if the
  // image writing threads can't keep up you may quickly begin to run out of RAM!

  // this does work, but only if you have at least two loops
  // let file_path = captured_frame_path(app, &frame);
  // println!("{}",file_path.as_path().display());
  // app.main_window().capture_frame(file_path);

  // https://github.com/nannou-org/nannou/issues/187

  // let image: glium::texture::RawImage2d<u8> = display.read_front_buffer().unwrap();
  // let image = image::ImageBuffer::from_raw(image.width, image.height, image.data.into_owned()).unwrap();
  // let image = image::DynamicImage::ImageRgba8(image).flipv();
  // image.save("glium-example-screenshot.png").unwrap();

  // optional if all you're doing is drawing
  //   if frame.nth() == 1 {
  //       std::process::exit(0);
  //   }
}

fn captured_frame_path(app: &App, frame: &Frame) -> std::path::PathBuf {
  // Create a path that we want to save this frame to.
  app
    .project_path()
    .expect("failed to locate `project_path`")
    // Capture all frames to a directory called `/<path_to_nannou>/nannou/simple_capture`.
    .join("assets")
    // Name each file after the number of the frame.
    .join(format!("{}_{:03}", app.exe_name().unwrap(), frame.nth()))
    // The extension will be PNG. We also support tiff, bmp, gif, jpeg, webp and some others.
    .with_extension("png")
}

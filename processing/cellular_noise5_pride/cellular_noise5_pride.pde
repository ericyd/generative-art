/*
* Happy Pride!
*
* This sketch is simply a wrapper around a fragment shader
* which actually does all the work.
*
* So, please see the accompanying shader.frag
* for notes on how this works!
*/

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

PShader shader;

void setup() {
  size(1000, 1000, P2D);
  noStroke();

  shader = loadShader("shader.frag");
}

void draw() {
  shader.set("u_resolution", float(width), float(height));
  shader.set("u_time", millis() / 1000.0);
  shader(shader);
  rect(0,0,width,height);
  saveFrame("assets/img_" + now() + ".png");
//   exit();
}

String now() {
  LocalDateTime date = LocalDateTime.now();
  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
  return date.format(formatter);
}

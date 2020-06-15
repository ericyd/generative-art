import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

PShader shader;

void setup() {
  size(2000, 2000, P2D);
  noStroke();

  shader = loadShader("shader.frag");
}

void draw() {
  shader.set("u_resolution", float(width), float(height));
  shader(shader);
  rect(0,0,width,height);
  saveFrame("assets/img_" + now() + ".png");
  exit();
}

String now() {
  LocalDateTime date = LocalDateTime.now();
  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
  return date.format(formatter);
}

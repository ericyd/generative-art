import java.time.LocalDateTime;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

PShader shader;

// Set to constant value if same results are desired each time
long seed = Instant.now().toEpochMilli() & 1111111111;
// long seed = 28673;

void setup() {
  size(1000, 1000, P2D);
  noStroke();

  shader = loadShader("shader.frag");
}

void draw() {
  shader.set("iResolution", float(width), float(height));
  shader.set("iSeed", seed);
  shader.set("iTime", millis() / 1000.0);
  shader(shader);
  rect(0,0,width,height);

  saveFrame("assets/img_" + now() + "_seed-" + seed + ".png");
  exit();
}

String now() {
  LocalDateTime date = LocalDateTime.now();
  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
  return date.format(formatter);
}

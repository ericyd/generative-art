import java.time.LocalDateTime;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

PShader shader;
PShape can;

// Set to constant value if same results are desired each time
// long seed = Instant.now().toEpochMilli() & 1111111111;
float seed = 1146.942;

void setup() {
  size(1000, 1000, P2D);
  noStroke();
  PImage label = loadImage("i-love-you-portland3-blur.png");
  can = createCanvas(label);

  shader = loadShader("fragment.glsl", "vertex.glsl");
  shader.set("iResolution", float(width), float(height));
}

void draw() {
  background(0);
  shader.set("iTime", millis() / 1000.0);
  shader(shader);
  shape(can);

  saveFrame("assets/img_" + now() + "_seed-" + seed + ".png");
  exit();
}

String now() {
  LocalDateTime date = LocalDateTime.now();
  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
  return date.format(formatter);
}

// This just creates a square shape that matches the size of the image.
// The purpose of this (I believe) is to pass the texture data from `tex`
// into the vertex shader,
// which extracts some data and saves it to varying attributes.
// The vertex shader then sends that data on to the fragment shader,
// which can use the data when rendering the colors.
PShape createCanvas(PImage tex) {
  textureMode(NORMAL);
  PShape sh = createShape();
  sh.beginShape();
  sh.noStroke();
  sh.texture(tex);
  sh.vertex(0.0, 0.0, 0.0, 0.0);
  sh.vertex(0.0, height, 0.0, 1.0);
  sh.vertex(width, height, 1.0, 1.0);
  sh.vertex(width, 0, 1.0, 0.0);
  sh.endShape();
  return sh;
}

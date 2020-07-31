// this is nice because it only prints errors.
// However, debugging it can be tedious.
// Consider using ./scripts/build_all_examples.sh
// if the error messages are not forthcoming.
fn main() {
  let output = std::process::Command::new("cargo")
    .args(&["build", "--release", "--examples"])
    .output()
    .expect("failed to run `cargo build -p package --examples`");
  if !output.stderr.is_empty() {
    let stderr = String::from_utf8(output.stderr).unwrap();
    if stderr.contains("error:") {
      panic!("failed to build examples for package:\n{}", stderr);
    }
  }
}

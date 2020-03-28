# Generative Art in Rust

Generative art made with nannou.cc. All Rust All the Time.

## Structure

Since these are all sketches made with [nannou](https://nannou.cc/), it is much more convenient to structure them around "examples" rather than creating individual repos, submodules, or directories for each one. Also, it ensures that they all work with the same dependencies, which would be nice for any future lookers.

## Running a sketch

```
cargo run --release --example name_of_example
```

Where `name_of_example` corresponds to the filename (minus the `.rs` extension) of the example you want to run.

Note that the `--release` flag is technically optional, but it is _soooooooo_ much faster with it that you'll definitely want to use it by default.

## Where can I see the finished products without running the code?

Still figuring that out. I may end up committing them to the repo but I'd rather not.

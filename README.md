# Generative Art in Rust (_and Processing!_)

Generative art made with [nannou.cc](https://nannou.cc/) and [processing](https://processing.org/). All ~Rust~ Kickass All the Time.

_Update 2020-06-09: Now some things are using Processing too!_

## Rust stuff

Since these are all sketches made with [nannou](https://nannou.cc/), it is much more convenient to structure them around "examples" rather than creating individual repos, submodules, or directories for each one. Also, it ensures that they all work with the same dependencies, which would be nice for any future lookers.

### Running a sketch

```
cargo run --release --example name_of_example
```

Where `name_of_example` corresponds to the filename (minus the `.rs` extension) of the example you want to run.

**Notes**
* the `--release` flag is technically optional, but it is _soooooooo_ much faster with it that you'll definitely want to use it by default.
* any examples prepended with "xp_" are intended as "exploratory" sketches - not intended as final output
* If any examples look hella weird, try reseting to nannou 0.13.1 and see if it works better

```bash
git reset c6f0676ddb8bf3fad3b087eb32059cd607edeb2e
```

## Processing stuff

Originally I was planning on only using Rust. Then I realized I was interested in learning to use shaders. It turns out, Rust + Shaders = Nightmare. Yes, it's very much possible but holy crap it's a lot of work for not a lot of gain. Conversely, there are a billion tools out there that make shaders easy to use and Processing is a great candidate.

### Running a sketch

I personally like the Processing CLI because I dislike the Processing editor, so I run my sketches with

```
processing-java --sketch=`pwd`/processing/sketch_name --run
```

## Where can I see the finished products without running the code?

[Gallery](./Gallery.md)

----

## Why is the primary branch named `main` instead of `master`?

To support [anti-racist language in tech!](https://dev.to/damcosset/replacing-master-in-git-2jim)

If you'd like to do the same:

```bash
$  git checkout master
$  git pull origin master
$  git branch -M main
$  git push origin main
# Update Settings/Branches/Default branch to main on GitHub or your git server of choice
$  git push origin :master
```

If you ascribe more to the stance that [renaming master is harmful](https://dev.to/dandv/8-problems-with-replacing-master-in-git-2hck) then my recommendation to you is to stop being so fragile, it's really no big deal.

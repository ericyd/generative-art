# Generative Art
Built with [Nannou][], [Processing][], and [OpenRNDR][]

## Where can I see the finished products without running the code?

Please see [my personal site](https://ericyd.com/generative-art) or [my instagram](https://www.instagram.com/ericydauenhauer/)

## Broken links? 😱

Did you follow a link from Instagram or elsewhere and get a 404???

**TLDR**: replace `master` or `main` in the URL with `5f072452563a05ea9fa4e895336504654df5f971` and it should work.

**Not too long, did read**: Originally I was planning to only write code using [Nannou][].

Then I wanted to explore shaders and didn't like how low-level I had to get with Nannou. So I explored [Processing][] (the classic). It's aight, but something about it didn't sing to me. I wanted something else. Also, I like Kotlin and when I learned about [OpenRNDR][] it seemed like a cool framework to explore.

I always wanted a single repo for my art, not spread out by language or framework. I had to reorganize code that was once very distinctly a [Rust][]-only project into a format that could logically support multiple frameworks and languages. Obviously this is a weird way to structure a repo, but for creative coding purposes I think it's suitable. So now I have art separated by framework. Any files at the top level are there because they have to be (I'm looking at you [Gradle][] 🙄), but the actual art code is nested. The downside of this was effectively a "breaking change" to the organization of the repo, and many links attached to my [Instagram][] posts will not work. Questions or comments? Reach out with an issue in this repo or on my [Instagram][]!  

## Nannou (Rust) sketches ([/nannou](./nannou))

Since these are all sketches made with [Nannou][], it is much more convenient to structure them around "examples" rather than creating individual repos, submodules, or directories for each one. Also, it ensures that they all work with the same dependencies, which would be nice for any future lookers.

### Running a sketch

```
cd nannou
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

### Formatting

I ❤️ auto-formatting (also props to [rustfmt][], it is such a great and fast formatter)

```bash
cargo fmt
```


## Processing sketches ([/processing](./processing))

Originally I was planning on only using Rust. Then I realized I was interested in learning to use shaders. It turns out, Rust + Shaders = Nightmare. Yes, it's very much possible but holy crap it's a lot of work for not a lot of gain. Conversely, there are a billion tools out there that make shaders easy to use and Processing is a great candidate.

### Running a sketch

I personally like the Processing CLI because I dislike the Processing editor, so I run my sketches with

```
processing-java --sketch=`pwd`/processing/sketch_name --run
```

## OpenRNDR (Kotlin) sketches ([/openrndr](./openrndr))

Honestly, Rust can be such a pain in the ass. Kotlin is just much more flexible and I got to a point where I wanted that flexibility for my creative coding. I also like that OpenRNDR exposes the shader API at a high level (at least this appears to be the case, I haven't used this functionality yet at time of writing). We'll see if I stick with it, but it looks like a nice, intuitive framework right now.

### Running a sketch

As recommended by the [OpenRNDR][] project, IntelliJ CE makes it much easier. I mean, heck, they built Kotlin so why wouldn't they have first-class Kotlin support?? If you're using IntelliJ, you can run any sketch with the "Run" icon next to the `main()` function in the sketch.

If you'd like to run via command line, you totally can!

```bash
# title argument can end in "Kt" or not, your choice
# Do not include `sketch` in the title, but do include any other packages
./gradlew sketch -Ptitle=flow.MySketch
```

### Formatting

I ❤️ auto-formatting

```bash
./gradlew ktlintFormat
```

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

<!-- Links -->

[Nannou]: https://nannou.cc/
[Processing]: https://processing.org/
[OpenRNDR]: https://openrndr.org/
[Instagram]: https://www.instagram.com/ericydauenhauer/
[Gradle]: https://www.baeldung.com/gradle
[Rust]: https://www.rust-lang.org/
[rustfmt]: https://github.com/rust-lang/rustfmt

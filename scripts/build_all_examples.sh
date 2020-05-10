#!/bin/zsh
#
# This script will attempt to build (compile) all the examples in the `examples` directory.
# This is a useful way to ensure that none of the examples are completely broken by a dependency update.
# It doesn't check against visual regressions obviously, but I think that's OK since I tend to introduce
# visual regressions myself by editing the way my utilities work.

for file in examples/*.rs; do
  # on non-Mac machines, may need to change this to
  # sed -r 's/(examples\/)//g'
  # references:
  # https://stackoverflow.com/a/2871217/3991555
  # https://unix.stackexchange.com/questions/13711/differences-between-sed-on-mac-osx-and-other-standard-sed
  file=$(echo $file | sed -E 's/(examples\/)//g')
  file=$(echo $file | sed -E 's/(\.rs)//g')
  echo "Building $file ..."
  if [ "$1" != "--dry" ]; then
    cargo build --release --example $file
  fi
done

#!/bin/sh

filename=$1
file_prefix=$2

if [ ! -f "$filename" ]; then
  echo "File not found: $filename"
  exit 1
fi

dataset="cells"
echo "[" > "$file_prefix-$dataset.txt"
h5dump --dataset $dataset --noindex $filename | rg -Uo 'DATA \{[\n\s0-9,]*}' | rg -v 'DATA \{|\}' >> "$file_prefix-$dataset.txt"
echo "]" >> "$file_prefix-$dataset.txt"

echo "wrote to $file_prefix-$dataset.txt"

dataset="coords"
echo "[" > "$file_prefix-$dataset.txt"
h5dump --dataset $dataset --noindex $filename | rg -Uo 'DATA \{[\n\s0-9e,.-]*}' | rg -v 'DATA \{|\}' >> "$file_prefix-$dataset.txt"
echo "]" >> "$file_prefix-$dataset.txt"

echo "wrote to $file_prefix-$dataset.txt"


```
docker pull badlandsmodel/badlands

docker run -it -p 8888:8888 -v "$PWD":/live/share badlandsmodel/badlands
```

Navigate to localhost:8888 to see the notebooks.

Open a `ipynb` notebook, and run each cell in order.

When you get to the last step:

```
model.run_to_time(1000000)
```

Running it will print outputs such as:

```
- Writing outputs (1.03 seconds; tNow = 0.0)
- Writing outputs (0.85 seconds; tNow = 10000.0)
```

Move files from /workshop/basic/output/** into /share/badlands/basin/**

Download "HDFView": https://www.hdfgroup.org/download-hdfview/

Probably better: Download the hdf5 program directly: https://www.hdfgroup.org/download-hdf5/

The running a command like this: 

```shell
file_prefix="stream-power-law"

dataset="cells"
echo "[" > "$file_prefix-$dataset.txt"
~/Downloads/HDF_Group/HDF5/1.14.6/bin/./h5dump --dataset $dataset --noindex tin.time9.hdf5 | rg -Uo 'DATA \{[\n\s0-9,]*}' | rg -v 'DATA \{|\}' >> "$file_prefix-$dataset.txt"
echo "]" >> "$file_prefix-$dataset.txt"

dataset="coords"
echo "[" > "$file_prefix-$dataset.txt"
~/Downloads/HDF_Group/HDF5/1.14.6/bin/./h5dump --dataset $dataset --noindex tin.time9.hdf5 | rg -Uo 'DATA \{[\n\s0-9e,.-]*}' | rg -v 'DATA \{|\}' >> "$file_prefix-$dataset.txt"
echo "]" >> "$file_prefix-$dataset.txt"
```

Outputs a file like this:
```
HDF5 "tin.time9.hdf5" {
DATASET "cells" {
   DATATYPE  H5T_STD_I32LE
   DATASPACE  SIMPLE { ( 414534, 3 ) / ( 414534, 3 ) }
   DATA {
      9398, 4666, 4667,
      4671, 4672, 9857,
      4665, 4666, 9317,
...
      193322, 214257, 193316
   }
}
}
```

Maybe I extract the data using a regex/strip and process it as a 1-D array.


FUCK YES!!!!! FUCKING YES!!!!!!!!!!

1. With HDFView, open the file `h5/tin.time100.hdf5`
2. Open the "cells" dataset, copy/paste to text file
3. `processToJSON()`
  - replace all `\t` with `,`
  - replace all `\n` with `],\n[`
  - prepend `[[` to beginning of file
  - append `]]` to end of file
  - yes, this could be done in the script file, just hasn't been done yet.
4. Open the "coords" dataset, copy/paste to text file
5. `processToJSON()`
6. Use the algo from `homegrown-svg/sketch/20250310-badlands1.ts` to process. TLDR:
  - `cells.json` contains triangle definitions, where the indices of each element correspond to the index in the `coords.json` array.
  - `coords.json` are the spatial vertices in `[x, y, z]` format
  - Combining these together gives a TIN with the elevations.

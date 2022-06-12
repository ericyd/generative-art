// definitely do not like this... need a better way to write to file
import { draw } from "./scripts/layers-with-shadow.js";
import { writeFileSync } from "fs";

writeFileSync(`output-${Date.now()}.svg`, draw());

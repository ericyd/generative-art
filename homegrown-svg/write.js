// definitely do not like this... need a better way to write to file
import { draw } from "./scripts/walker2.js";
import { writeFileSync } from "fs";

writeFileSync(`screenshots/output-${Date.now()}.svg`, draw());

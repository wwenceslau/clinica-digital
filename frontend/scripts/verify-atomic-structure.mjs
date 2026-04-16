import fs from "node:fs";
import path from "node:path";

const requiredDirs = [
  "src/components/atoms",
  "src/components/molecules",
  "src/components/organisms",
  "src/components/templates",
];

const root = process.cwd();
const missing = requiredDirs.filter((dir) => !fs.existsSync(path.join(root, dir)));

if (missing.length > 0) {
  console.error("Atomic structure check failed. Missing directories:");
  for (const dir of missing) {
    console.error(`- ${dir}`);
  }
  process.exit(1);
}

console.log("Atomic structure check passed.");

import fs from "node:fs";
import path from "node:path";

const blockedPatterns = ["fetch(", "axios", "/api/", "repository", "sql", "jdbc"];
const scopeRoots = ["src/components", "src/context"];

const root = process.cwd();
const violations = [];

function walk(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(fullPath);
      continue;
    }
    if (!/\.(ts|tsx)$/.test(entry.name)) {
      continue;
    }
    const content = fs.readFileSync(fullPath, "utf8");
    for (const pattern of blockedPatterns) {
      if (content.includes(pattern)) {
        violations.push({ file: path.relative(root, fullPath), pattern });
      }
    }
  }
}

for (const relativeRoot of scopeRoots) {
  const absoluteRoot = path.join(root, relativeRoot);
  if (fs.existsSync(absoluteRoot)) {
    walk(absoluteRoot);
  }
}

if (violations.length > 0) {
  console.error("Shell scope guard failed. Possible domain/business logic usage detected:");
  for (const item of violations) {
    console.error(`- ${item.file} -> pattern: ${item.pattern}`);
  }
  process.exit(1);
}

console.log("Shell scope guard passed.");

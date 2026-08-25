// Regenerates src/generated/<service>.d.ts from packages/shared-contracts/openapi/<service>.yaml.
// Run via `pnpm generate:api-client` from the repo root, or `pnpm generate` here.
// Generated output is gitignored — these specs are the source of truth
// (packages/shared-contracts/openapi/*.yaml), not the generated types.
import { existsSync, mkdirSync, readdirSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";
import openapiTS, { astToString } from "openapi-typescript";

const here = path.dirname(fileURLToPath(import.meta.url));
const specsDir = path.resolve(here, "../../shared-contracts/openapi");
const outDir = path.resolve(here, "../src/generated");

if (!existsSync(outDir)) mkdirSync(outDir, { recursive: true });

const specFiles = readdirSync(specsDir).filter((f) => f.endsWith(".yaml"));
if (specFiles.length === 0) {
  console.error(`No OpenAPI specs found in ${specsDir}`);
  process.exit(1);
}

for (const file of specFiles) {
  const service = file.replace(/\.yaml$/, "");
  const specPath = path.join(specsDir, file);
  console.log(`Generating types for ${service}...`);
  const ast = await openapiTS(new URL(`file://${specPath.replace(/\\/g, "/")}`));
  const output = astToString(ast);
  writeFileSync(path.join(outDir, `${service}.d.ts`), output);
}

console.log(`Generated ${specFiles.length} type files into ${outDir}`);

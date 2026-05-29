const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const sourceRoot = path.join(root, 'src/main/java/com/fap');
const docsDir = path.join(root, 'docs/api');

function walk(dir) {
  const result = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      result.push(...walk(fullPath));
    } else if (entry.name.endsWith('Controller.java')) {
      result.push(fullPath);
    }
  }
  return result;
}

function mappingPath(annotationArgs) {
  const match = annotationArgs.match(/"([^"]*)"/);
  return match ? match[1] : '';
}

function joinPaths(basePath, subPath) {
  return `/${[basePath, subPath].filter(Boolean).join('/')}`
    .replace(/\/+/g, '/')
    .replace(/\/\{/g, '/{');
}

const methodMap = {
  GetMapping: 'GET',
  PostMapping: 'POST',
  PutMapping: 'PUT',
  PatchMapping: 'PATCH',
  DeleteMapping: 'DELETE'
};

const endpoints = [];

for (const file of walk(sourceRoot).sort()) {
  const source = fs.readFileSync(file, 'utf8');
  const baseMatch = source.match(/@RequestMapping\(([^\n)]*)\)[\s\S]*?public class/);
  const basePath = baseMatch ? mappingPath(baseMatch[1]) : '';
  const tag = (source.match(/@Tag\(name = "([^"]+)"\)/) || [, 'Untagged'])[1];
  const lines = source.split(/\r?\n/);

  for (let i = 0; i < lines.length; i += 1) {
    const mapping = lines[i].trim().match(/^@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)\b(.*)$/);
    if (!mapping) {
      continue;
    }
    const previousAnnotations = lines.slice(Math.max(0, i - 10), i).join('\n');
    const summary = (previousAnnotations.match(/@Operation\(summary = "([^"]+)"\)/) || [, ''])[1];
    const responseCodes = [...previousAnnotations.matchAll(/responseCode = "(\d+)"/g)].map(match => match[1]);
    endpoints.push({
      tag,
      method: methodMap[mapping[1]],
      path: joinPaths(basePath, mappingPath(mapping[2])),
      summary,
      responseCodes: [...new Set(responseCodes)].join(', ')
    });
  }
}

endpoints.sort((a, b) => a.tag.localeCompare(b.tag) || a.path.localeCompare(b.path) || a.method.localeCompare(b.method));

const endpointsByTag = new Map();
for (const endpoint of endpoints) {
  if (!endpointsByTag.has(endpoint.tag)) {
    endpointsByTag.set(endpoint.tag, []);
  }
  endpointsByTag.get(endpoint.tag).push(endpoint);
}

const output = [];
output.push('# FAP OpenAPI Endpoint Documentation');
output.push('');
output.push('Generated from Spring MVC controller mappings and OpenAPI annotations.');
output.push('');
output.push('## Swagger / OpenAPI URLs');
output.push('');
output.push('| Resource | URL |');
output.push('|---|---|');
output.push('| Swagger UI | `/swagger-ui/index.html` |');
output.push('| OpenAPI JSON | `/v3/api-docs` |');
output.push('| OpenAPI YAML | `/v3/api-docs.yaml` |');
output.push('');
output.push('## Authentication');
output.push('');
output.push('Most `/api/v1/**` endpoints require JWT Bearer authentication. Use Swagger UI **Authorize** with:');
output.push('');
output.push('```text');
output.push('Bearer <access-token>');
output.push('```');
output.push('');
output.push(`## Endpoint Inventory (${endpoints.length} operations)`);
output.push('');

for (const [tag, items] of endpointsByTag) {
  output.push(`### ${tag}`);
  output.push('');
  output.push('| Method | Path | Summary | Documented responses |');
  output.push('|---|---|---|---|');
  for (const endpoint of items) {
    output.push(`| ${endpoint.method} | \`${endpoint.path}\` | ${endpoint.summary || '-'} | ${endpoint.responseCodes || '-'} |`);
  }
  output.push('');
}

output.push('## Verification Checklist');
output.push('');
output.push('- Every controller has `@Tag`.');
output.push('- Every mapped endpoint has `@Operation`.');
output.push('- Every mapped endpoint has documented OpenAPI response codes.');
output.push('- Bearer JWT security scheme is configured as `bearerAuth`.');
output.push('- `Accept-Language` header is globally documented.');

fs.mkdirSync(docsDir, { recursive: true });
fs.writeFileSync(path.join(docsDir, 'openapi-endpoints.md'), `${output.join('\n')}\n`, 'utf8');
console.log(`Generated docs/api/openapi-endpoints.md with ${endpoints.length} operations.`);


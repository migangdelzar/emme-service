import { execFileSync } from 'node:child_process';
import assert from 'node:assert/strict';
import process from 'node:process';

const composeExecutable = (() => {
  try {
    execFileSync('docker', ['compose', 'version'], { stdio: 'ignore' });
    return ['docker', 'compose'];
  } catch {
    return ['docker-compose'];
  }
})();

const composeFiles = [
  'deployment/compose/compose.yaml',
  'deployment/compose/compose.runtime-jvm.yaml',
  'deployment/compose/compose.age.yaml',
];

const output = execFileSync(
  composeExecutable[0],
  [
    ...composeExecutable.slice(1),
    ...composeFiles.flatMap((file) => ['-f', file]),
    'config',
    '--format',
    'json',
  ],
  {
    cwd: process.cwd(),
    encoding: 'utf8',
    env: {
      ...process.env,
      POSTGRES_AGE_IMAGE: 'emme-postgres-age-pgvector:contract',
      EMME_SERVICE_IMAGE: 'emme-service:age-contract',
    },
  },
);

const services = JSON.parse(output).services;

assert.equal(services.postgres.image, 'emme-postgres-age-pgvector:contract');
assert.equal(services['emme-platform'].environment.EMME_AI_AGE_ENABLED, 'true');
assert.equal(services['emme-platform'].environment.EMME_AI_AGE_GRAPH_PREFIX, 'emme_ai_graph_');

console.log('AGE Compose deployment contract passed.');

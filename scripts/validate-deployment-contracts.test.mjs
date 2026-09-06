import { readFile } from 'node:fs/promises';
import assert from 'node:assert/strict';

const workflow = await readFile('.github/workflows/ci-backend.yml', 'utf8');
const deployment = await readFile('infra/kubernetes/base/backend-deployment.yaml', 'utf8');
const migrationJob = await readFile('infra/kubernetes/jobs/migration-job.yaml', 'utf8');

assert.match(workflow, /node scripts\/validate-deployment-contracts\.mjs/);
assert.match(deployment, /path: \/actuator\/health\/liveness/);
assert.match(deployment, /path: \/actuator\/health\/readiness/);
assert.match(deployment, /runAsNonRoot: true/);
assert.match(migrationJob, /name: emme-secrets/);
assert.match(migrationJob, /key: postgres-password/);

console.log('Deployment contract passed.');

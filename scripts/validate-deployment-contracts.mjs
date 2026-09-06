import { readFile } from 'node:fs/promises';

const [workflow, deployment, migrationJob] = await Promise.all([
  readFile('.github/workflows/ci-backend.yml', 'utf8'),
  readFile('infra/kubernetes/base/backend-deployment.yaml', 'utf8'),
  readFile('infra/kubernetes/jobs/migration-job.yaml', 'utf8'),
]);

const required = [
  ['backend workflow invokes deployment validation', workflow, 'node scripts/validate-deployment-contracts.mjs'],
  ['backend liveness probe', deployment, 'path: /actuator/health/liveness'],
  ['backend readiness probe', deployment, 'path: /actuator/health/readiness'],
  ['backend runs as non-root', deployment, 'runAsNonRoot: true'],
  ['migration job uses the shared secret', migrationJob, 'name: emme-secrets'],
  ['migration job reads the database password', migrationJob, 'key: postgres-password'],
];

for (const [description, source, fragment] of required) {
  if (!source.includes(fragment)) {
    throw new Error(`Deployment contract is missing ${description}: ${fragment}`);
  }
}

console.log('Deployment contract passed.');

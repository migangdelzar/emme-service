import { readFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
import test from 'node:test';

import { validateSensitiveEnvironmentValues } from './validate-deployment-contracts.mjs';

const workflow = await readFile('.github/workflows/ci-backend.yml', 'utf8');
const deployment = await readFile('infra/kubernetes/base/backend-deployment.yaml', 'utf8');
const migrationJob = await readFile('infra/kubernetes/jobs/migration-job.yaml', 'utf8');
const productionJvmOverlay = await readFile(
  'infra/kubernetes/overlays/k3s-production-jvm/kustomization.yaml',
  'utf8',
);
const productionNativeOverlay = await readFile(
  'infra/kubernetes/overlays/k3s-production-native/kustomization.yaml',
  'utf8',
);
const localPostgres = await readFile('infra/kubernetes/local/postgres.yaml', 'utf8');
const localKeycloak = await readFile('infra/kubernetes/local/keycloak.yaml', 'utf8');

assert.match(workflow, /node scripts\/validate-deployment-contracts\.mjs/);
assert.match(deployment, /path: \/actuator\/health\/liveness/);
assert.match(deployment, /path: \/actuator\/health\/readiness/);
assert.match(deployment, /runAsNonRoot: true/);
assert.match(migrationJob, /name: emme-secrets/);
assert.match(migrationJob, /key: postgres-password/);
assert.doesNotMatch(deployment, /APP_GOOGLE_OAUTH_/);
assert.doesNotMatch(deployment, /replace-with-32-char-secure-key!!/);

console.log('Deployment contract passed.');

test('rejects literal passwords, secrets, encryption keys, and OAuth values', () => {
  const unsafeManifest = `
containers:
  - name: backend
    env:
      - name: DB_PASSWORD
        value: emme
      - name: GOOGLE_TOKEN_ENCRYPTION_KEY
        value: replace-with-32-char-secure-key!!
      - name: GOOGLE_OAUTH_CLIENT_SECRET
        value: ''
      - name: GOOGLE_OAUTH_REDIRECT_URI
        value: https://dev.emme.lat/callback
`;

  let error;
  assert.throws(() => {
    try {
      validateSensitiveEnvironmentValues(unsafeManifest, 'unsafe fixture');
    } catch (caught) {
      error = caught;
      throw caught;
    }
  });
  for (const environmentName of [
    'DB_PASSWORD',
    'GOOGLE_TOKEN_ENCRYPTION_KEY',
    'GOOGLE_OAUTH_CLIENT_SECRET',
    'GOOGLE_OAUTH_REDIRECT_URI',
  ]) {
    assert.match(error.message, new RegExp(`unsafe fixture: ${environmentName}`));
  }
});

test('accepts sensitive values sourced from a Kubernetes Secret', () => {
  const safeManifest = `
containers:
  - name: backend
    env:
      - name: DB_PASSWORD
        valueFrom:
          secretKeyRef:
            name: emme-secrets
            key: postgres-password
      - name: GOOGLE_TOKEN_ENCRYPTION_KEY
        valueFrom:
          secretKeyRef:
            name: emme-secrets
            key: google-token-encryption-key
      - name: GOOGLE_OAUTH_CLIENT_SECRET
        valueFrom:
          secretKeyRef:
            name: emme-secrets
            key: google-oauth-client-secret
      - name: GOOGLE_OAUTH_REDIRECT_URI
        valueFrom:
          secretKeyRef:
            name: emme-secrets
            key: google-oauth-redirect-uri
`;

  assert.deepEqual(
    validateSensitiveEnvironmentValues(safeManifest, 'safe fixture'),
    [],
  );
});

test('production overlays keep backend credentials behind Secret references', () => {
  for (const overlay of [productionJvmOverlay, productionNativeOverlay]) {
    assert.doesNotThrow(() => validateSensitiveEnvironmentValues(overlay, 'production overlay'));
    assert.match(overlay, /name: DB_PASSWORD[\s\S]*key: postgres-password/);
    assert.match(overlay, /name: GOOGLE_TOKEN_ENCRYPTION_KEY[\s\S]*key: google-token-encryption-key/);
  }
});

test('local Kubernetes infrastructure retains its disposable defaults', () => {
  assert.match(localPostgres, /name: POSTGRES_PASSWORD\s+value: emme/);
  assert.match(localKeycloak, /name: KEYCLOAK_ADMIN_PASSWORD\s+value: admin/);
});

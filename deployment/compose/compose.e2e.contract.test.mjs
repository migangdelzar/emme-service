import { execFileSync } from 'node:child_process';
import assert from 'node:assert/strict';
import process from 'node:process';

const composeFiles = [
  'deployment/compose/compose.yaml',
  'deployment/compose/compose.runtime-jvm.yaml',
  'deployment/compose/compose.environment-e2e.yaml',
];

const composeExecutable = (() => {
  try {
    execFileSync('docker', ['compose', 'version'], { stdio: 'ignore' });
    return ['docker', 'compose'];
  } catch {
    return ['docker-compose'];
  }
})();

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
      EMME_SERVICE_IMAGE: 'emme-service:e2e-contract',
      EMME_E2E_KEYCLOAK_ADMIN_PASSWORD: 'contract-only-password',
    },
  },
);

const services = JSON.parse(output).services;

for (const serviceName of ['emme-platform', 'postgres', 'redis', 'keycloak', 'database-migrations']) {
  assert.ok(services[serviceName], `missing ${serviceName} service`);
}

assert.equal(services['emme-platform'].image, 'emme-service:e2e-contract');
assert.equal(
  services['emme-platform'].environment.SPRING_DATASOURCE_URL,
  'jdbc:postgresql://postgres:5432/emme?currentSchema=e2e_studio,emme_core,public',
);
assert.equal(services['emme-platform'].environment.SPRING_DATA_REDIS_HOST, 'redis');
assert.equal(services['emme-platform'].environment.APP_IDENTITY_LOGIN_RATE_LIMIT_MAX_ATTEMPTS, '20');
assert.equal(services['emme-platform'].environment.SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA, undefined);
assert.equal(
  services['emme-platform'].environment.GOOGLE_TOKEN_ENCRYPTION_KEY,
  'e2e-token-encryption-32-byte-key',
);
assert.equal(
  services['emme-platform'].environment.SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_CLIENT_ID,
  'emme-salon-app',
);
assert.equal(
  services['emme-platform'].environment.SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_ISSUER_URI,
  'http://keycloak:8080/realms/emme',
);
assert.equal(
  services['emme-platform'].environment.APP_KEYCLOAK_BASE_URL,
  'http://keycloak:8080',
);

console.log('E2E Compose contract passed.');

import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const sensitiveEnvironmentName =
  /(PASSWORD|SECRET|TOKEN|ENCRYPTION_KEY|PRIVATE_KEY|API_KEY|ACCESS_KEY|OAUTH)/;

function leadingSpaces(line) {
  return line.match(/^\s*/u)?.[0].length ?? 0;
}

function environmentEntries(source) {
  const lines = source.split('\n');
  const entries = [];

  for (let index = 0; index < lines.length; index += 1) {
    const match = lines[index].match(/^(\s*)-\s+name:\s*([A-Z][A-Z0-9_]*)\s*$/u);
    if (!match) {
      continue;
    }

    const indent = match[1].length;
    const block = [];
    for (let next = index + 1; next < lines.length; next += 1) {
      const line = lines[next];
      if (line.trim() !== '' && leadingSpaces(line) < indent) {
        break;
      }
      if (
        line.trim() !== '' &&
        leadingSpaces(line) === indent &&
        /^-\s+name:/u.test(line.trim())
      ) {
        break;
      }
      block.push(line);
    }

    entries.push({ name: match[2], block });
  }

  return entries;
}

export function validateSensitiveEnvironmentValues(source, sourceName) {
  const violations = [];

  for (const { name, block } of environmentEntries(source)) {
    if (!sensitiveEnvironmentName.test(name)) {
      continue;
    }

    const hasSecretKeyRef = block.some((line) => /\bsecretKeyRef:\s*$/u.test(line));
    if (!hasSecretKeyRef) {
      violations.push(
        `${sourceName}: ${name} must use secretKeyRef; literal or non-Secret values are forbidden`,
      );
    }
  }

  if (violations.length > 0) {
    throw new Error(violations.join('\n'));
  }

  return [];
}

async function readDeploymentSources() {
  const sourcePaths = [
    '.github/workflows/ci-backend.yml',
    'infra/kubernetes/base/backend-deployment.yaml',
    'infra/kubernetes/jobs/migration-job.yaml',
    'infra/kubernetes/overlays/k3d-jvm/kustomization.yaml',
    'infra/kubernetes/overlays/k3d-native/kustomization.yaml',
    'infra/kubernetes/overlays/k3s-production-jvm/kustomization.yaml',
    'infra/kubernetes/overlays/k3s-production-native/kustomization.yaml',
  ];

  const sources = await Promise.all(
    sourcePaths.map(async (sourcePath) => [
      sourcePath,
      await readFile(sourcePath, 'utf8'),
    ]),
  );

  return new Map(sources);
}

export async function validateDeploymentContracts() {
  const sources = await readDeploymentSources();
  const workflow = sources.get('.github/workflows/ci-backend.yml');
  const deployment = sources.get('infra/kubernetes/base/backend-deployment.yaml');
  const migrationJob = sources.get('infra/kubernetes/jobs/migration-job.yaml');

  const required = [
    ['backend workflow invokes deployment validation', workflow, 'node scripts/validate-deployment-contracts.mjs'],
    ['backend liveness probe', deployment, 'path: /actuator/health/liveness'],
    ['backend readiness probe', deployment, 'path: /actuator/health/readiness'],
    ['backend runs as non-root', deployment, 'runAsNonRoot: true'],
    ['migration job uses the shared secret', migrationJob, 'name: emme-secrets'],
    ['migration job reads the database password', migrationJob, 'key: postgres-password'],
  ];

  const forbidden = [
    ['obsolete Google OAuth environment names', deployment, 'APP_GOOGLE_OAUTH_'],
    ['Google OAuth encryption-key placeholder', deployment, 'replace-with-32-char-secure-key!!'],
  ];

  const errors = [];
  for (const [description, source, fragment] of required) {
    if (!source.includes(fragment)) {
      errors.push(`Deployment contract is missing ${description}: ${fragment}`);
    }
  }

  for (const [description, source, fragment] of forbidden) {
    if (source.includes(fragment)) {
      errors.push(`Deployment contract contains ${description}: ${fragment}`);
    }
  }

  for (const [sourcePath, source] of sources) {
    if (sourcePath.startsWith('infra/kubernetes/')) {
      try {
        validateSensitiveEnvironmentValues(source, sourcePath);
      } catch (error) {
        errors.push(error.message);
      }
    }
  }

  if (errors.length > 0) {
    throw new Error(errors.join('\n'));
  }
}

if (path.resolve(process.argv[1] ?? '') === fileURLToPath(import.meta.url)) {
  await validateDeploymentContracts();
  console.log('Deployment contract passed.');
}

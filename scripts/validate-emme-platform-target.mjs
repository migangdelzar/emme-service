import { access, readFile } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

export async function validateTargetFiles({ repositoryRoot, rules }) {
  const errors = [];

  for (const rule of rules.files) {
    const relativePath = rule.path;
    const filePath = path.join(repositoryRoot, relativePath);

    try {
      await access(filePath);
    } catch {
      errors.push(`${relativePath}: file does not exist`);
      continue;
    }

    const source = await readFile(filePath, 'utf8');

    for (const token of rule.required ?? []) {
      if (!source.includes(token)) {
        errors.push(`${relativePath}: missing required token "${token}"`);
      }
    }

    for (const token of rule.forbidden ?? []) {
      if (source.includes(token)) {
        errors.push(`${relativePath}: contains forbidden token "${token}"`);
      }
    }
  }

  return errors;
}

export const canonicalTargetRules = {
  files: [
    {
      path: '.github/workflows/ci-module-boundaries.yml',
      required: [':applications:emme-platform:test'],
      forbidden: [':applications:studio-api:test'],
    },
    {
      path: 'mise.toml',
      required: [':applications:emme-platform:build', ':applications:emme-platform:test'],
      forbidden: [':applications:studio-api:test'],
    },
    {
      path: 'build-logic/src/main/kotlin/com/emme/buildlogic/deployment/provider/KubernetesProvider.kt',
      required: ['KubernetesWorkload.DEPLOYMENT_NAME', 'KubernetesWorkload.POD_SELECTOR'],
      forbidden: ['studio-api', 'app=studio-api'],
    },
    {
      path: 'build-logic/src/main/kotlin/com/emme/buildlogic/deployment/provider/KubernetesWorkload.kt',
      required: ['DEPLOYMENT_NAME = "backend"', 'POD_SELECTOR = "app=emme-backend"'],
      forbidden: ['studio-api'],
    },
    {
      path: 'deployment/compose/compose.yml',
      required: ['emme-platform', 'ghcr.io/migangdelzar/emme-service:'],
      forbidden: ['studio-api', 'emme-service-studio-api'],
    },
    {
      path: 'deployment/compose/compose.local.yml',
      required: ['emme-platform'],
      forbidden: ['studio-api'],
    },
    {
      path: 'deployment/compose/compose.test.yml',
      required: ['emme-platform'],
      forbidden: ['studio-api'],
    },
    {
      path: 'deployment/helm/emme/values.yaml',
      required: ['repository: ghcr.io/migangdelzar/emme-service'],
      forbidden: ['emme-service-studio-api'],
    },
    {
      path: 'deployment/kubernetes/base/kustomization.yml',
      required: ['emme-platform/deployment.yml', 'emme-platform/service.yml'],
      forbidden: ['studio-api/'],
    },
    {
      path: 'deployment/kubernetes/base/emme-platform/deployment.yml',
      required: ['name: emme-platform', 'app: emme-platform', 'ghcr.io/migangdelzar/emme-service:'],
      forbidden: ['studio-api', 'emme-service-studio-api'],
    },
    {
      path: 'deployment/kubernetes/base/emme-platform/service.yml',
      required: ['name: emme-platform', 'app: emme-platform'],
      forbidden: ['studio-api'],
    },
    {
      path: 'deployment/kubernetes/base/ingress/ingress.yml',
      required: ['name: emme-platform', 'secretName: emme-platform-tls'],
      forbidden: ['studio-api'],
    },
    {
      path: 'deployment/kubernetes/overlays/local/kustomization.yml',
      required: ['ghcr.io/migangdelzar/emme-service', 'name: emme-platform'],
      forbidden: ['studio-api', 'emme-service-studio-api'],
    },
    {
      path: 'deployment/kubernetes/overlays/production/kustomization.yml',
      required: ['ghcr.io/migangdelzar/emme-service', 'name: emme-platform'],
      forbidden: ['studio-api', 'emme-service-studio-api'],
    },
    {
      path: 'deployment/kubernetes/components/network-policies/kustomization.yml',
      required: ['deny-all.yml'],
      forbidden: ['studio-api'],
    },
    {
      path: 'deployment/scripts/wait-for-cluster.sh',
      required: ['emme-platform'],
      forbidden: ['studio-api'],
    },
  ],
};

const currentFile = fileURLToPath(import.meta.url);
const invokedFile = path.resolve(process.argv[1] ?? '');

if (currentFile === invokedFile) {
  const errors = await validateTargetFiles({
    repositoryRoot: process.cwd(),
    rules: canonicalTargetRules,
  });

  if (errors.length > 0) {
    console.error(errors.join('\n'));
    process.exitCode = 1;
  } else {
    console.log('emme-platform deployment target validation passed.');
  }
}

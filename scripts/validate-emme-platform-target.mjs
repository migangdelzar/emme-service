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
      path: 'settings.gradle.kts',
      required: ['include(":applications:emme-platform")'],
      forbidden: ['include(":applications:studio-api")'],
    },
    {
      path: '.github/workflows/ci-backend.yml',
      required: [':applications:emme-platform:test'],
      forbidden: [':applications:studio-api:test'],
    },
    {
      path: '.github/workflows/ci-backend.yml',
      required: [':applications:emme-platform:bootJar'],
      forbidden: [':applications:studio-api:bootJar', './gradlew deploy', '-Pprovider='],
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
      path: 'deployment/compose/compose.yaml',
      required: ['emme-platform', 'ghcr.io/migangdelzar/emme-service:'],
      forbidden: ['studio-api', 'emme-service-studio-api'],
    },
    {
      path: 'deployment/compose/compose.runtime-jvm.yaml',
      required: ['services:', 'emme-platform:', 'EMME_SERVICE_IMAGE'],
      forbidden: ['EMME_PLATFORM_NATIVE_IMAGE', 'studio-api'],
    },
    {
      path: 'deployment/compose/compose.runtime-native.yaml',
      required: ['services:', 'emme-platform:', 'EMME_SERVICE_IMAGE'],
      forbidden: ['EMME_PLATFORM_JVM_IMAGE', 'studio-api'],
    },
    {
      path: 'deployment/compose/compose.environment-local.yaml',
      required: ['emme-platform'],
      forbidden: ['studio-api'],
    },
    {
      path: 'deployment/compose/compose.environment-ci.yaml',
      required: ['emme-platform'],
      forbidden: ['studio-api'],
    },
    {
      path: 'infra/kubernetes/overlays/k3d-jvm/kustomization.yaml',
      required: ['JVM runtime overlay', 'newTag: dev'],
      forbidden: ['k3d-native', 'dev-native'],
    },
    {
      path: 'infra/kubernetes/overlays/k3d-native/kustomization.yaml',
      required: ['Native Image overlay', 'newTag: dev-native'],
      forbidden: ['k3d-jvm', 'dev-jvm'],
    },
    {
      path: 'infra/kubernetes/overlays/k3s-production-jvm/kustomization.yaml',
      required: ['JVM runtime overlay', 'newTag: 0.1.0'],
      forbidden: ['k3s-production-native', '0.1.0-native'],
    },
    {
      path: 'infra/kubernetes/overlays/k3s-production-native/kustomization.yaml',
      required: ['Native Image overlay', 'newTag: 0.1.0-native'],
      forbidden: ['k3s-production-jvm', '0.1.0-jvm'],
    },
    {
      path: 'deployment/helm/emme/values.yaml',
      required: ['repository: ghcr.io/migangdelzar/emme-service'],
      forbidden: ['emme-service-studio-api'],
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

import { mkdtemp, mkdir, readFile, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import test from 'node:test';
import assert from 'node:assert/strict';

import {
  canonicalTargetRules,
  validateTargetFiles,
} from './validate-emme-platform-target.mjs';

test('rejects an active deployment file that targets studio-api', async () => {
  const repositoryRoot = await mkdtemp(path.join(tmpdir(), 'emme-target-'));
  await mkdir(path.join(repositoryRoot, 'deployment'), { recursive: true });
  await writeFile(
    path.join(repositoryRoot, 'deployment', 'compose.yml'),
    'services:\n  studio-api:\n    image: ghcr.io/migangdelzar/emme-service-studio-api:latest\n',
  );

  const errors = await validateTargetFiles({
    repositoryRoot,
    rules: {
      files: [
        {
          path: 'deployment/compose.yml',
          required: ['emme-platform', 'ghcr.io/migangdelzar/emme-service:'],
          forbidden: ['studio-api', 'emme-service-studio-api'],
        },
      ],
    },
  });

  assert.deepEqual(errors, [
    'deployment/compose.yml: missing required token "emme-platform"',
    'deployment/compose.yml: missing required token "ghcr.io/migangdelzar/emme-service:"',
    'deployment/compose.yml: contains forbidden token "studio-api"',
    'deployment/compose.yml: contains forbidden token "emme-service-studio-api"',
  ]);
});

test('accepts an emme-platform deployment file', async () => {
  const repositoryRoot = await mkdtemp(path.join(tmpdir(), 'emme-target-'));
  await mkdir(path.join(repositoryRoot, 'deployment'), { recursive: true });
  await writeFile(
    path.join(repositoryRoot, 'deployment', 'compose.yml'),
    'services:\n  emme-platform:\n    image: ghcr.io/migangdelzar/emme-service:latest\n',
  );

  const errors = await validateTargetFiles({
    repositoryRoot,
    rules: {
      files: [
        {
          path: 'deployment/compose.yml',
          required: ['emme-platform', 'ghcr.io/migangdelzar/emme-service:'],
          forbidden: ['studio-api', 'emme-service-studio-api'],
        },
      ],
    },
  });

  assert.deepEqual(errors, []);
});

test('repository deployment surfaces target emme-platform', async () => {
  const errors = await validateTargetFiles({
    repositoryRoot: process.cwd(),
    rules: canonicalTargetRules,
  });

  assert.deepEqual(errors, []);
});

test('JVM and native Compose overlays select exactly one runtime image family', async () => {
  const composeDirectory = path.join(process.cwd(), 'deployment', 'compose');
  const jvmOverlay = await readFile(
    path.join(composeDirectory, 'compose.jvm.yml'),
    'utf8',
  );
  const nativeOverlay = await readFile(
    path.join(composeDirectory, 'compose.native.yml'),
    'utf8',
  );

  assert.match(jvmOverlay, /EMME_PLATFORM_JVM_IMAGE/);
  assert.doesNotMatch(jvmOverlay, /EMME_PLATFORM_NATIVE_IMAGE/);
  assert.match(nativeOverlay, /EMME_PLATFORM_NATIVE_IMAGE/);
  assert.doesNotMatch(nativeOverlay, /EMME_PLATFORM_JVM_IMAGE/);
  assert.match(jvmOverlay, /EMME_PLATFORM_JVM_IMAGE:-ghcr\.io\/migangdelzar\/emme-service:\$\{TAG:-latest\}/);
  assert.match(nativeOverlay, /EMME_PLATFORM_NATIVE_IMAGE:-ghcr\.io\/migangdelzar\/emme-service:\$\{TAG:-latest\}-native\}/);
});

test('K3d and K3s overlays select JVM or native images explicitly', async () => {
  const overlaysDirectory = path.join(
    process.cwd(),
    'infra',
    'kubernetes',
    'overlays',
  );
  const devJvm = await readFile(
    path.join(overlaysDirectory, 'dev', 'kustomization.yaml'),
    'utf8',
  );
  const devNative = await readFile(
    path.join(overlaysDirectory, 'dev-native', 'kustomization.yaml'),
    'utf8',
  );
  const prodJvm = await readFile(
    path.join(overlaysDirectory, 'prod', 'kustomization.yaml'),
    'utf8',
  );
  const prodNative = await readFile(
    path.join(overlaysDirectory, 'prod-native', 'kustomization.yaml'),
    'utf8',
  );

  assert.match(devJvm, /newTag: dev/);
  assert.match(devNative, /newTag: dev-native/);
  assert.match(prodJvm, /newTag: 0\.1\.0/);
  assert.match(prodNative, /newTag: 0\.1\.0-native/);
  assert.match(devNative, /path: \/spec\/template\/spec\/containers\/0\/env\/1/);
  assert.match(prodNative, /path: \/spec\/template\/spec\/containers\/0\/env\/1/);
});

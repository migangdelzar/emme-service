import { mkdtemp, mkdir, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import test from 'node:test';
import assert from 'node:assert/strict';

import { validateTargetFiles } from './validate-emme-platform-target.mjs';

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

import { readFile } from 'node:fs/promises';
import test from 'node:test';
import assert from 'node:assert/strict';

test('Mise exposes explicit JVM and native Java 25 validation lanes', async () => {
  const mise = await readFile(new URL('../mise.toml', import.meta.url), 'utf8');

  assert.match(mise, /\[tasks\."toolchain:jvm"\]/);
  assert.match(mise, /\[tasks\."toolchain:native"\]/);
  assert.match(mise, /node scripts\/verify-java25-runtime\.mjs/);
  assert.match(mise, /node scripts\/verify-native-image-runtime\.mjs/);
  assert.match(mise, /nativeCompile -Pemme\.native-image=true/);
});

test('backend CI validates the Java 25 runtime after installing Java', async () => {
  const workflow = await readFile(
    new URL('../.github/workflows/ci-backend.yml', import.meta.url),
    'utf8',
  );

  assert.match(workflow, /name: Validate Java 25 runtime/);
  assert.match(workflow, /node scripts\/verify-java25-runtime\.mjs/);
});

test('the manual native-image CI lane validates its GraalVM runtime', async () => {
  const workflow = await readFile(
    new URL('../.github/workflows/container-image.yml', import.meta.url),
    'utf8',
  );

  assert.match(workflow, /node scripts\/verify-java25-runtime\.mjs/);
  assert.match(workflow, /node scripts\/verify-native-image-runtime\.mjs/);
});

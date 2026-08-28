import test from 'node:test';
import assert from 'node:assert/strict';

import {
  assertNativeImage25Runtime,
  parseNativeImageMajorVersion,
} from './verify-native-image-runtime.mjs';

test('parses the Native Image major version', () => {
  assert.equal(
    parseNativeImageMajorVersion('Native Image 25.0.1 2025-04-15\nSubstrate VM'),
    25,
  );
});

test('accepts a GraalVM 25 Native Image runtime', () => {
  assert.equal(
    assertNativeImage25Runtime(
      'GraalVM Community Edition 25.0.1\nNative Image 25.0.1',
    ),
    25,
  );
});

test('rejects an older Native Image runtime', () => {
  assert.throws(
    () => assertNativeImage25Runtime('Native Image 24.0.2 2025-01-21'),
    /Emme requires GraalVM Native Image 25 or newer; detected version 24/,
  );
});

test('rejects output without a Native Image version', () => {
  assert.throws(
    () => parseNativeImageMajorVersion('GraalVM is installed'),
    /Unable to determine the Native Image major version/,
  );
});

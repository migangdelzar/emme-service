import test from 'node:test';
import assert from 'node:assert/strict';

import {
  assertJava25Runtime,
  parseJavaMajorVersion,
} from './verify-java25-runtime.mjs';

test('parses the major version from Java 25 output', () => {
  assert.equal(
    parseJavaMajorVersion('openjdk 25.0.2 2025-07-15\nOpenJDK Runtime Environment'),
    25,
  );
});

test('parses the major version from legacy Java version output', () => {
  assert.equal(
    parseJavaMajorVersion('java version "25.0.1" 2025-04-15\nJava(TM) SE Runtime Environment'),
    25,
  );
});

test('accepts Java 25 and newer runtimes', () => {
  assert.equal(assertJava25Runtime('openjdk 25.0.2 2025-07-15'), 25);
  assert.equal(assertJava25Runtime('openjdk 26.0.1 2026-04-21'), 26);
});

test('rejects runtimes older than Java 25 with an actionable message', () => {
  assert.throws(
    () => assertJava25Runtime('openjdk 24.0.2 2025-01-21'),
    /Emme requires Java 25 or newer; detected Java 24/,
  );
});

test('rejects output without a detectable Java version', () => {
  assert.throws(
    () => parseJavaMajorVersion('not a Java version'),
    /Unable to determine the Java major version/,
  );
});

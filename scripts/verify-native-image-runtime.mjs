import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const execFileAsync = promisify(execFile);
const REQUIRED_NATIVE_IMAGE_MAJOR_VERSION = 25;

export function parseNativeImageMajorVersion(versionOutput) {
  const nativeImageMatch = versionOutput.match(
    /\bnative[- ]image\s+["']?(\d+)/i,
  );
  const graalVmMatch = versionOutput.match(
    /\bgraalvm(?:\s+(?:community|enterprise|edition))+\s+["']?(\d+)/i,
  );
  const match = nativeImageMatch ?? graalVmMatch;

  if (!match) {
    throw new Error(
      'Unable to determine the Native Image major version from the runtime output.',
    );
  }

  return Number.parseInt(match[1], 10);
}

export function assertNativeImage25Runtime(versionOutput) {
  if (!/\bnative[- ]image\b/i.test(versionOutput)) {
    throw new Error(
      'The configured GraalVM runtime does not expose the Native Image executable.',
    );
  }

  const majorVersion = parseNativeImageMajorVersion(versionOutput);

  if (majorVersion < REQUIRED_NATIVE_IMAGE_MAJOR_VERSION) {
    throw new Error(
      `Emme requires GraalVM Native Image ${REQUIRED_NATIVE_IMAGE_MAJOR_VERSION} or newer; ` +
        `detected version ${majorVersion}.`,
    );
  }

  return majorVersion;
}

function nativeImageExecutableFromEnvironment(environment = process.env) {
  const runtimeHome = environment.GRAALVM_HOME ?? environment.JAVA_HOME;
  return runtimeHome ? path.join(runtimeHome, 'bin', 'native-image') : 'native-image';
}

export async function readNativeImageVersionOutput(
  nativeImageExecutable = nativeImageExecutableFromEnvironment(),
) {
  try {
    const result = await execFileAsync(nativeImageExecutable, ['--version'], {
      encoding: 'utf8',
    });
    return `${result.stdout}${result.stderr}`;
  } catch (error) {
    const output = `${error.stdout ?? ''}${error.stderr ?? ''}`;
    const details = output || error.message;
    throw new Error(
      `Unable to execute ${nativeImageExecutable} --version: ${details}`,
      { cause: error },
    );
  }
}

export async function verifyNativeImage25Runtime(
  nativeImageExecutable = nativeImageExecutableFromEnvironment(),
) {
  const versionOutput = await readNativeImageVersionOutput(nativeImageExecutable);
  return assertNativeImage25Runtime(versionOutput);
}

const currentFile = fileURLToPath(import.meta.url);
const invokedFile = path.resolve(process.argv[1] ?? '');

if (currentFile === invokedFile) {
  try {
    const majorVersion = await verifyNativeImage25Runtime();
    console.log(
      `GraalVM Native Image ${majorVersion} runtime verified (minimum: 25).`,
    );
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}

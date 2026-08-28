import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const execFileAsync = promisify(execFile);
export const REQUIRED_JAVA_MAJOR_VERSION = 25;

export function parseJavaMajorVersion(versionOutput) {
  const match = versionOutput.match(
    /\b(?:openjdk|java|jdk)(?:\s+version)?\s+["']?(\d+)/i,
  );

  if (!match) {
    throw new Error(
      'Unable to determine the Java major version from the runtime output.',
    );
  }

  return Number.parseInt(match[1], 10);
}

export function assertJava25Runtime(versionOutput) {
  const majorVersion = parseJavaMajorVersion(versionOutput);

  if (majorVersion < REQUIRED_JAVA_MAJOR_VERSION) {
    throw new Error(
      `Emme requires Java ${REQUIRED_JAVA_MAJOR_VERSION} or newer; detected Java ${majorVersion}. ` +
        'Set JAVA_HOME to a Java 25 installation or run the command through Mise.',
    );
  }

  return majorVersion;
}

function javaExecutableFromEnvironment(environment = process.env) {
  return environment.JAVA_HOME
    ? path.join(environment.JAVA_HOME, 'bin', 'java')
    : 'java';
}

async function captureVersionOutput(javaExecutable, argument) {
  try {
    const result = await execFileAsync(javaExecutable, [argument], {
      encoding: 'utf8',
    });
    return `${result.stdout}${result.stderr}`;
  } catch (error) {
    const output = `${error.stdout ?? ''}${error.stderr ?? ''}`;
    if (output.length > 0) {
      return output;
    }

    throw new Error(
      `Unable to execute ${javaExecutable} ${argument}: ${error.message}`,
      { cause: error },
    );
  }
}

export async function readJavaVersionOutput(
  javaExecutable = javaExecutableFromEnvironment(),
) {
  const modernOutput = await captureVersionOutput(javaExecutable, '--version');

  try {
    parseJavaMajorVersion(modernOutput);
    return modernOutput;
  } catch {
    return captureVersionOutput(javaExecutable, '-version');
  }
}

export async function verifyJava25Runtime(
  javaExecutable = javaExecutableFromEnvironment(),
) {
  const versionOutput = await readJavaVersionOutput(javaExecutable);
  return assertJava25Runtime(versionOutput);
}

const currentFile = fileURLToPath(import.meta.url);
const invokedFile = path.resolve(process.argv[1] ?? '');

if (currentFile === invokedFile) {
  try {
    const majorVersion = await verifyJava25Runtime();
    console.log(`Java ${majorVersion} runtime verified (minimum: Java 25).`);
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}

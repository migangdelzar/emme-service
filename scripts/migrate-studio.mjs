import {
  mkdirSync,
  readdirSync,
  readFileSync,
  statSync,
  writeFileSync,
} from 'node:fs';
import { dirname, join, relative } from 'node:path';

const REPOSITORY_ROOT = new URL('..', import.meta.url).pathname.replace(/\/$/, '');
const SOURCE_ROOTS = {
  main: join(REPOSITORY_ROOT, 'modules/studio/src/main/java'),
  test: join(REPOSITORY_ROOT, 'modules/studio/src/test/java'),
};

const TARGET_ROOTS = {
  main: join(REPOSITORY_ROOT, 'src/main/java'),
  test: join(REPOSITORY_ROOT, 'src/test/java'),
};

const TARGET_MODULES = [
  'services',
  'appointments',
  'salon',
  'clients',
  'subscriptions',
  'documents',
];

const CORE_CONTEXTS = [
  ['Appointment', 'appointments'],
  ['AvailableSlot', 'appointments'],
  ['Dashboard', 'appointments'],
  ['ExternalCalendarStatus', 'appointments'],
  ['BusinessConfiguration', 'salon'],
  ['BusinessProfile', 'salon'],
  ['OperatingHours', 'salon'],
  ['BookingPolicy', 'salon'],
  ['NotificationPreference', 'salon'],
  ['Customer', 'clients'],
  ['Artist', 'services'],
  ['Service', 'services'],
  ['StudioResourceNotFound', 'services'],
  ['BusinessDay', 'salon'],
  ['DayOfWeek', 'salon'],
  ['TemplatePolicy', 'salon'],
  ['Profile', 'salon'],
  ['Hours', 'salon'],
  ['Policy', 'salon'],
];

const TEST_CONTEXTS = [
  ['Appointment', 'appointments'],
  ['Dashboard', 'appointments'],
  ['Artist', 'services'],
  ['Service', 'services'],
  ['Customer', 'clients'],
  ['Business', 'salon'],
  ['Salon', 'salon'],
];

function className(relativePath) {
  return relativePath.split('/').at(-1).replace(/\.java$/, '');
}

function contextForName(name, contexts = CORE_CONTEXTS) {
  return contexts.find(([prefix]) => name.includes(prefix))?.[1] ?? null;
}

function isPackageInfo(relativePath) {
  return className(relativePath) === 'package-info';
}

function contextForNestedCapability(relativePath) {
  if (relativePath.includes('/documents/')) return 'documents';
  if (relativePath.includes('/subscriptions/')) return 'subscriptions';
  return null;
}

export function classifyProductionFile(relativePath) {
  const normalized = relativePath.replaceAll('\\', '/');
  const nestedTarget = contextForNestedCapability(normalized);
  if (nestedTarget) return nestedTarget;
  if (isPackageInfo(normalized)) return null;

  const nameTarget = contextForName(className(normalized));
  if (nameTarget) return nameTarget;
  if (normalized.includes('/adapter/out/messaging/')) return 'appointments';
  if (normalized.includes('/adapter/in/web/sse/')) return 'appointments';

  throw new Error(`No Studio module mapping for production file: ${relativePath}`);
}

export function classifyTestFile(relativePath) {
  const normalized = relativePath.replaceAll('\\', '/');
  if (normalized.endsWith('/StudioPackageConventionTest.java')) return null;

  const nestedTarget = contextForNestedCapability(normalized);
  if (nestedTarget) return nestedTarget;
  if (isPackageInfo(normalized)) return null;

  const nameTarget = contextForName(className(normalized), TEST_CONTEXTS);
  if (nameTarget) return nameTarget;

  throw new Error(`No Studio module mapping for test file: ${relativePath}`);
}

function importTarget(importedClass, importedPackage, defaultTarget) {
  if (importedPackage.startsWith('documents')) return 'documents';
  if (importedPackage.startsWith('subscriptions')) return 'subscriptions';
  return contextForName(importedClass) ?? defaultTarget;
}

export function rewriteJavaSource(source, defaultTarget) {
  const packageRewritten = source
    .replace(
      /package com\.emme\.(studio\.(?:documents|subscriptions)|salon)(\.[a-zA-Z0-9_.]+)?;/,
      (_, sourceNamespace, suffix = '') => {
        const nestedNamespace = sourceNamespace.split('.').at(-1);
        if (nestedNamespace === 'documents' || nestedNamespace === 'subscriptions') {
          return `package com.emme.${nestedNamespace}${suffix};`;
        }
        return `package com.emme.${defaultTarget}${suffix};`;
      },
    )
    .replace(
      /package com\.emme\.studio(\.[a-zA-Z0-9_.]+)?;/,
      (_, suffix = '') => `package com.emme.${defaultTarget}${suffix};`,
    );

  const importsRewritten = packageRewritten.replace(
    /import com\.emme\.studio(?:\.([a-zA-Z0-9_.]+))?\.([A-Z][A-Za-z0-9_]*);/g,
    (match, importedPackage = '', importedClass) => {
      const target = importTarget(importedClass, importedPackage, defaultTarget);
      const nestedPrefix = importedPackage.split('.')[0];
      if (nestedPrefix === 'documents' || nestedPrefix === 'subscriptions') {
        return match.replace(
          `com.emme.studio.${nestedPrefix}`,
          `com.emme.${target}`,
        );
      }
      return match.replace('com.emme.studio', `com.emme.${target}`);
    },
  );

  return importsRewritten.replace(
    /com\.emme\.studio(?:\.([A-Za-z0-9_.]+))?/g,
    (match, suffix = '') => {
      const nestedPrefix = suffix.split('.')[0];
      if (nestedPrefix === 'documents' || nestedPrefix === 'subscriptions') {
        return `com.emme.${nestedPrefix}${suffix.slice(nestedPrefix.length)}`;
      }
      const target = contextForName(suffix.split('.').at(-1)) ?? defaultTarget;
      return `com.emme.${target}${suffix ? `.${suffix}` : ''}`;
    },
  );
}

function packagePath(source) {
  const match = source.match(/^package ([a-zA-Z0-9_.]+);/m);
  if (!match) throw new Error('Java source has no package declaration');
  return match[1].replaceAll('.', '/');
}

function outputPath(kind, sourceFile, source, target) {
  return join(
    REPOSITORY_ROOT,
    `modules/${target}/src/${kind === 'main' ? 'main/java' : 'test/java'}`,
    packagePath(source),
    sourceFile.split('/').at(-1),
  );
}

function walk(directory) {
  if (!statSync(directory, { throwIfNoEntry: false })?.isDirectory()) return [];
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) return walk(path);
    return entry.name.endsWith('.java') ? [path] : [];
  });
}

function migrateKind(kind, checkOnly) {
  const sourceRoot = SOURCE_ROOTS[kind];
  const files = walk(sourceRoot);
  const mismatches = [];
  let migrated = 0;

  for (const absolutePath of files) {
    const relativePath = relative(sourceRoot, absolutePath).replaceAll('\\', '/');
    const target =
      kind === 'main'
        ? classifyProductionFile(relativePath)
        : classifyTestFile(relativePath);
    if (!target) continue;

    const source = readFileSync(absolutePath, 'utf8');
    const rewritten = rewriteJavaSource(source, target);
    const destination = outputPath(kind, absolutePath, rewritten, target);
    const existing = statSync(destination, { throwIfNoEntry: false })
      ? readFileSync(destination, 'utf8')
      : null;

    if (checkOnly) {
      if (existing !== rewritten) mismatches.push(destination);
    } else {
      mkdirSync(dirname(destination), { recursive: true });
      writeFileSync(destination, rewritten);
    }
    migrated += 1;
  }

  return { migrated, mismatches };
}

function ensurePackageMetadata(kind, checkOnly) {
  const mismatches = [];
  for (const target of TARGET_MODULES) {
    const packageRoot = join(
      REPOSITORY_ROOT,
      `modules/${target}/src/${kind === 'main' ? 'main/java' : 'test/java'}/com/emme/${target}`,
    );
    for (const directory of walkDirectories(packageRoot)) {
      const javaFiles = walk(directory).filter((path) => path.endsWith('.java'));
      if (javaFiles.length === 0) continue;

      const packageInfo = join(directory, 'package-info.java');
      const packageName = relative(join(REPOSITORY_ROOT, `modules/${target}/src/${kind === 'main' ? 'main/java' : 'test/java'}`), directory)
        .replaceAll('\\', '/')
        .replaceAll('/', '.')
        .replace(/^com\.emme\./, 'com.emme.');
      const expected = `package ${packageName};\n`;
      const existing = statSync(packageInfo, { throwIfNoEntry: false })
        ? readFileSync(packageInfo, 'utf8')
        : null;

      if (existing === null) {
        if (checkOnly) mismatches.push(packageInfo);
        else writeFileSync(packageInfo, expected);
      }
    }
  }
  return mismatches;
}

function walkDirectories(directory) {
  if (!statSync(directory, { throwIfNoEntry: false })?.isDirectory()) return [];
  return [
    directory,
    ...readdirSync(directory, { withFileTypes: true })
      .filter((entry) => entry.isDirectory())
      .flatMap((entry) => walkDirectories(join(directory, entry.name))),
  ];
}

export function migrate({ checkOnly = false } = {}) {
  const main = migrateKind('main', checkOnly);
  const test = migrateKind('test', checkOnly);
  const mismatches = [
    ...main.mismatches,
    ...test.mismatches,
    ...ensurePackageMetadata('main', checkOnly),
    ...ensurePackageMetadata('test', checkOnly),
  ];

  if (mismatches.length > 0) {
    throw new Error(
      `Migration output differs for ${mismatches.length} file(s):\n${mismatches.join('\n')}`,
    );
  }

  console.log(
    `${checkOnly ? 'Checked' : 'Migrated'} ${main.migrated} production and ${test.migrated} test files`,
  );
}

if (process.argv[1]?.endsWith('/migrate-studio.mjs')) {
  migrate({ checkOnly: process.argv.includes('--check') });
}

import { access, readdir, readFile } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';

const repositoryRoot = process.cwd();
const ignoredDirectories = new Set([
  '.git',
  '.gradle',
  'build',
  'node_modules',
  'dist',
  'target',
]);

async function markdownFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    if (entry.isDirectory() && !ignoredDirectories.has(entry.name)) {
      files.push(...await markdownFiles(path.join(directory, entry.name)));
    } else if (entry.isFile() && entry.name.endsWith('.md')) {
      files.push(path.join(directory, entry.name));
    }
  }

  return files;
}

function validateFences(file, source, errors) {
  let fence = null;

  source.split('\n').forEach((line, index) => {
    const match = line.match(/^ {0,3}(`{3,}|~{3,})/);
    if (!match) return;

    const marker = match[1][0];
    if (fence === null) {
      fence = marker;
    } else if (fence === marker) {
      fence = null;
    }

    if (fence !== null && index === source.split('\n').length - 1) {
      errors.push(`${path.relative(repositoryRoot, file)}:${index + 1}: unclosed code fence`);
    }
  });

  if (fence !== null && !errors.some((error) => error.startsWith(path.relative(repositoryRoot, file)))) {
    errors.push(`${path.relative(repositoryRoot, file)}: unclosed code fence`);
  }
}

async function validateLinks(file, source, errors) {
  const relativeFile = path.relative(repositoryRoot, file);
  const linkPattern = /!?\[[^\]]*\]\((?:<([^>]+)>|([^\s)]+))/g;
  let match;

  while ((match = linkPattern.exec(source)) !== null) {
    const target = (match[1] ?? match[2]).trim();
    if (/^(?:[a-z][a-z\d+.-]*:|\/\/|#)/i.test(target)) continue;

    const targetPath = decodeURIComponent(target.split('#', 1)[0].split('?', 1)[0]);
    if (!targetPath) continue;

    const resolvedTarget = path.resolve(path.dirname(file), targetPath);
    try {
      await access(resolvedTarget);
    } catch {
      errors.push(`${relativeFile}: broken relative link ${target}`);
    }
  }
}

const errors = [];
for (const file of await markdownFiles(repositoryRoot)) {
  const source = await readFile(file, 'utf8');
  validateFences(file, source, errors);
  await validateLinks(file, source, errors);
}

if (errors.length > 0) {
  console.error(errors.join('\n'));
  process.exitCode = 1;
} else {
  console.log('Markdown validation passed.');
}

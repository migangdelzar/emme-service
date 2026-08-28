import { readFile } from 'node:fs/promises';

const workflow = await readFile('.github/workflows/ci-backend.yml', 'utf8');

const requiredFragments = [
  'run_integration:',
  'run_e2e:',
  'coverageCheck',
  'boundaries:',
  'e2eTest',
  'node scripts/verify-java25-runtime.mjs',
  'needs: [quality, test, integration, build-logic, infrastructure, boundaries]',
  'if: always()',
];

for (const fragment of requiredFragments) {
  if (!workflow.includes(fragment)) {
    throw new Error(`Backend workflow is missing required fragment: ${fragment}`);
  }
}

if (/^  coverage:\s*$/m.test(workflow)) {
  throw new Error('Backend workflow must not contain a separate coverage job.');
}

if (workflow.includes('ubuntu-latest')) {
  throw new Error('Backend workflow must pin Ubuntu runners to ubuntu-24.04.');
}

for (const job of ['test', 'integration', 'build-logic', 'infrastructure']) {
  const jobBlock = workflow.match(
    new RegExp(`^  ${job}:\\n([\\s\\S]*?)(?=^  [a-zA-Z][\\w-]*:|$)`, 'm'),
  )?.[1] ?? '';
  if (/^    needs: quality$/m.test(jobBlock)) {
    throw new Error(`Backend job ${job} must not depend on quality.`);
  }
}

console.log('Backend workflow contract passed.');

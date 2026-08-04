import { readFile } from 'node:fs/promises';

const workflow = await readFile('.github/workflows/container-image.yml', 'utf8');

const requiredFragments = [
  'workflow_dispatch:',
  'bootBuildImage',
  '--imageName=',
  'aquasecurity/trivy-action@v0.36.0',
  'docker image inspect',
  'docker push',
  'packages: write',
  "github.event_name != 'pull_request'",
  'IMAGE_DIGEST',
];

for (const fragment of requiredFragments) {
  if (!workflow.includes(fragment)) {
    throw new Error(`Container workflow is missing required fragment: ${fragment}`);
  }
}

const nativeFragments = [
  'name: Build and scan native image (manual)',
  "if: github.event_name == 'workflow_dispatch' && inputs.native == true",
  'graalvm/setup-graalvm@v1',
  '-Pemme.native-image=true',
  'BP_NATIVE_IMAGE:',
  'sha-${{ github.sha }}-native',
  "skip-java-setup: 'true'",
];

for (const fragment of nativeFragments) {
  if (!workflow.includes(fragment)) {
    throw new Error(`Container workflow is missing native fragment: ${fragment}`);
  }
}

if (workflow.includes('build-image.sh')) {
  throw new Error('Container workflow must not delegate image creation to a shell script.');
}

console.log('Container image workflow contract passed.');

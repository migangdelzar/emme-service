import { execFileSync } from 'node:child_process';
import assert from 'node:assert/strict';
import process from 'node:process';

const composeExecutable = (() => {
  try {
    execFileSync('docker', ['compose', 'version'], { stdio: 'ignore' });
    return ['docker', 'compose'];
  } catch {
    return ['docker-compose'];
  }
})();

const composeFiles = [
  'deployment/compose/compose.yaml',
  'deployment/compose/compose.runtime-jvm.yaml',
  'deployment/compose/compose.environment-kafka.yaml',
];

const output = execFileSync(
  composeExecutable[0],
  [
    ...composeExecutable.slice(1),
    ...composeFiles.flatMap((file) => ['-f', file]),
    'config',
    '--format',
    'json',
  ],
  {
    cwd: process.cwd(),
    encoding: 'utf8',
    env: {
      PATH: process.env.PATH,
      EMME_SERVICE_IMAGE: 'emme-service:kafka-contract',
    },
  },
);

const services = JSON.parse(output).services;
const platform = services['emme-platform'];
const kafka = services.kafka;

assert.ok(platform, 'missing emme-platform service');
assert.ok(kafka, 'missing kafka service');
assert.equal(kafka.image, 'apache/kafka:3.8.0');
assert.equal(platform.environment.EMME_KAFKA_EVENTS_ENABLED, 'true');
assert.equal(platform.environment.KAFKA_BOOTSTRAP_SERVERS, 'kafka:9092');
assert.equal(platform.environment.KAFKA_SECURITY_PROTOCOL, 'PLAINTEXT');
assert.equal(platform.environment.KAFKA_SASL_JAAS_CONFIG, '');
assert.equal(platform.depends_on.kafka.condition, 'service_healthy');
assert.match(kafka.healthcheck.test.join(' '), /kafka-topics\.sh/);

console.log('Kafka Compose deployment contract passed.');

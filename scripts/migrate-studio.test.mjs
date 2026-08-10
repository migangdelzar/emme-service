import test from 'node:test';
import assert from 'node:assert/strict';

import {
  classifyProductionFile,
  classifyTestFile,
  rewriteJavaSource,
} from './migrate-studio.mjs';

test('classifies nested capabilities before generic Studio paths', () => {
  assert.equal(
    classifyProductionFile('com/emme/studio/documents/api/command/UploadDocumentCommand.java'),
    'documents',
  );
  assert.equal(
    classifyProductionFile(
      'com/emme/studio/subscriptions/domain/model/Subscription.java',
    ),
    'subscriptions',
  );
});

test('classifies core production types by bounded context', () => {
  assert.equal(
    classifyProductionFile(
      'com/emme/studio/adapter/in/web/controller/AppointmentController.java',
    ),
    'appointments',
  );
  assert.equal(
    classifyProductionFile(
      'com/emme/studio/adapter/in/web/controller/CustomerController.java',
    ),
    'clients',
  );
  assert.equal(
    classifyProductionFile(
      'com/emme/studio/adapter/in/web/controller/ServiceController.java',
    ),
    'services',
  );
  assert.equal(
    classifyProductionFile(
      'com/emme/studio/adapter/in/web/controller/BusinessConfigurationController.java',
    ),
    'salon',
  );
  assert.equal(
    classifyProductionFile('com/emme/studio/application/service/CreateCustomerService.java'),
    'clients',
  );
  assert.equal(
    classifyProductionFile('com/emme/studio/application/service/UpdateBusinessProfileService.java'),
    'salon',
  );
});

test('classifies historical salon test packages by test behavior', () => {
  assert.equal(
    classifyTestFile('com/emme/salon/module/CustomerModuleTest.java'),
    'clients',
  );
  assert.equal(
    classifyTestFile('com/emme/salon/web/DashboardWebTest.java'),
    'appointments',
  );
  assert.equal(
    classifyTestFile('com/emme/studio/documents/domain/DocumentTest.java'),
    'documents',
  );
});

test('rewrites packages and imports using the target bounded context', () => {
  const source = [
    'package com.emme.studio.adapter.in.web.controller;',
    '',
    'import com.emme.studio.api.result.AppointmentDetails;',
    'import com.emme.studio.subscriptions.api.type.PlanType;',
    'com.emme.studio.domain.model.Service service;',
  ].join('\n');

  assert.equal(
    rewriteJavaSource(source, 'appointments'),
    [
      'package com.emme.appointments.adapter.in.web.controller;',
      '',
      'import com.emme.appointments.api.result.AppointmentDetails;',
      'import com.emme.subscriptions.api.type.PlanType;',
      'com.emme.services.domain.model.Service service;',
    ].join('\n'),
  );
});

test('does not duplicate nested capability package names', () => {
  const source =
    'package com.emme.studio.documents.application.service;\n\n'
    + 'import com.emme.studio.documents.domain.model.Document;';

  assert.equal(
    rewriteJavaSource(source, 'documents'),
    'package com.emme.documents.application.service;\n\n'
      + 'import com.emme.documents.domain.model.Document;',
  );
});

test('fails closed for an unmapped production type', () => {
  assert.throws(
    () => classifyProductionFile('com/emme/studio/domain/model/Unknown.java'),
    /No Studio module mapping/,
  );
});

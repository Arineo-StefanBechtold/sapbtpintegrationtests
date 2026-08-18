const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { createApp } = require('../src/app');

async function withApp(run) {
  const dataDir = await fs.mkdtemp(path.join(os.tmpdir(), 'collector-'));
  const { app } = createApp({ dataDir });
  await app.ready();
  try {
    await run(app);
  } finally {
    await app.close();
    await fs.rm(dataDir, { recursive: true, force: true });
  }
}

test('collects and retrieves multiple documents per message id', async () => {
  await withApp(async (app) => {
    const headers = { 'x-test-run-id': 'run-1', 'x-message-id': 'MSG-1', 'content-type': 'application/xml' };
    const first = await app.inject({ method: 'POST', url: '/collect', headers, payload: '<one />' });
    const second = await app.inject({ method: 'POST', url: '/collect', headers, payload: '<two />' });

    assert.equal(first.statusCode, 202);
    assert.equal(second.statusCode, 202);
    assert.equal(first.json().sequenceNumber, 1);
    assert.equal(second.json().sequenceNumber, 2);

    const manifest = await app.inject({ method: 'GET', url: '/runs/run-1/messages' });
    assert.equal(manifest.statusCode, 200);
    assert.equal(manifest.json().messages[0].documents.length, 2);

    const payload = await app.inject({ method: 'GET', url: '/runs/run-1/messages/MSG-1/payload?sequenceNumber=2' });
    assert.equal(payload.statusCode, 200);
    assert.equal(payload.body, '<two />');

    const header = await app.inject({ method: 'GET', url: '/runs/run-1/messages/MSG-1/header?sequenceNumber=1' });
    assert.equal(header.statusCode, 200);
    assert.equal(header.json()['x-message-id'], 'MSG-1');
  });
});

test('residual state reflects released and unreleased message ids', async () => {
  await withApp(async (app) => {
    await app.inject({ method: 'POST', url: '/collect', headers: { 'x-test-run-id': 'run-2', 'x-message-id': 'MSG-A', 'content-type': 'application/xml' }, payload: '<a />' });
    await app.inject({ method: 'POST', url: '/collect', headers: { 'x-test-run-id': 'run-2', 'x-message-id': 'MSG-B', 'content-type': 'application/xml' }, payload: '<b />' });

    const release = await app.inject({ method: 'DELETE', url: '/runs/run-2/messages/MSG-A/release' });
    assert.equal(release.statusCode, 204);

    const residual = await app.inject({ method: 'GET', url: '/runs/run-2/residual' });
    assert.equal(residual.statusCode, 200);
    assert.deepEqual(residual.json().releasedMessageIds, ['MSG-A']);
    assert.deepEqual(residual.json().residualMessages.map((entry) => entry.messageId), ['MSG-B']);
  });
});

test('rejects path traversal in identifiers', async () => {
  await withApp(async (app) => {
    const response = await app.inject({
      method: 'POST',
      url: '/collect',
      headers: { 'x-test-run-id': '../run', 'x-message-id': 'MSG-1', 'content-type': 'application/xml' },
      payload: '<invalid />',
    });

    assert.equal(response.statusCode, 400);
    assert.match(response.json().message, /Invalid runId/);
  });
});

const fastifyFactory = require('fastify');
const { FileCollectorStorage } = require('./storage');

function createApp(options = {}) {
  const storage = options.storage || new FileCollectorStorage({ dataDir: options.dataDir });
  const app = fastifyFactory({ logger: false, bodyLimit: 10 * 1024 * 1024 });

  app.addContentTypeParser(/^.*$/, { parseAs: 'buffer' }, (request, payload, done) => {
    done(null, payload);
  });

  app.get('/health', async (_request, reply) => {
    try {
      const health = await storage.health();
      reply.code(200).send(health);
    } catch (error) {
      reply.code(503).send({ status: 'DOWN', message: error.message });
    }
  });

  app.post('/runs', async (request, reply) => {
    const runId = request.body && request.body.runId ? String(request.body.runId) : undefined;
    const run = await storage.createRun(runId);
    reply.code(201).send(run);
  });

  app.get('/runs/current', async (_request, reply) => {
    const run = await storage.getCurrentRun();
    if (!run) {
      reply.code(404).send({ message: 'No active run' });
      return;
    }
    reply.send(run);
  });

  app.post('/runs/:runId/end', async (request) => storage.endRun(request.params.runId));

  app.delete('/runs/:runId', async (request, reply) => {
    await storage.deleteRun(request.params.runId);
    reply.code(204).send();
  });

  app.post('/collect', async (request, reply) => {
    try {
      const result = await storage.collect({
        runId: request.headers['x-test-run-id'] ? String(request.headers['x-test-run-id']) : undefined,
        messageId: request.headers['x-message-id'] ? String(request.headers['x-message-id']) : undefined,
        payload: Buffer.isBuffer(request.body) ? request.body : Buffer.from(request.body || ''),
        headers: request.headers,
        contentType: request.headers['content-type'] ? String(request.headers['content-type']) : 'application/octet-stream',
      });
      reply.code(202).send(result);
    } catch (error) {
      reply.code(error.statusCode || 500).send({ message: error.message });
    }
  });

  app.get('/runs/:runId/messages', async (request) => storage.getRunMessages(request.params.runId));

  app.get('/runs/:runId/messages/:messageId', async (request, reply) => {
    const message = await storage.getMessage(request.params.runId, request.params.messageId);
    if (!message) {
      reply.code(404).send({ message: 'Message not found' });
      return;
    }
    reply.send(message);
  });

  app.get('/runs/:runId/messages/:messageId/payload', async (request, reply) => {
    const sequenceNumber = request.query.sequenceNumber ? Number(request.query.sequenceNumber) : 1;
    const document = await storage.getPayload(request.params.runId, request.params.messageId, sequenceNumber);
    if (!document) {
      reply.code(404).send({ message: 'Payload not found' });
      return;
    }
    reply.header('content-type', document.contentType).send(document.payload);
  });

  app.get('/runs/:runId/messages/:messageId/header', async (request, reply) => {
    const sequenceNumber = request.query.sequenceNumber ? Number(request.query.sequenceNumber) : 1;
    const header = await storage.getHeader(request.params.runId, request.params.messageId, sequenceNumber);
    if (!header) {
      reply.code(404).send({ message: 'Header not found' });
      return;
    }
    reply.header('content-type', 'application/json').send(header);
  });

  app.delete('/runs/:runId/messages/:messageId/release', async (request, reply) => {
    const deleted = await storage.releaseMessage(request.params.runId, request.params.messageId);
    reply.code(deleted ? 204 : 404).send();
  });

  app.get('/runs/:runId/residual', async (request) => storage.getResidual(request.params.runId));

  return { app, storage };
}

module.exports = {
  createApp,
};

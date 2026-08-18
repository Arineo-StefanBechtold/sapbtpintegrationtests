'use strict';

const storage = require('../storage');

async function collectRoutes(fastify) {
  fastify.addContentTypeParser('*', { parseAs: 'buffer' }, (_req, body, done) => {
    done(null, body);
  });

  fastify.post('/collect', async (request, reply) => {
    const messageId = request.headers['x-message-id'];
    if (!messageId) {
      return reply.code(400).send({ error: 'Missing X-Message-Id header' });
    }

    const testRunId = request.headers['x-test-run-id'] || 'default';
    const payload = request.body || Buffer.alloc(0);

    // Collect all headers (exclude internal ones if needed, but spec says store all)
    const headers = { ...request.headers };

    let result;
    try {
      result = await storage.storeDocument(testRunId, messageId, payload, headers);
    } catch (err) {
      request.log.error(err, 'Storage error');
      return reply.code(503).send({ error: 'Storage unavailable' });
    }

    return reply.code(202).send({
      messageId: result.messageId,
      testRunId: result.testRunId,
      sequenceNumber: result.seqNo,
      storedAt: result.storedAt,
    });
  });
}

module.exports = collectRoutes;

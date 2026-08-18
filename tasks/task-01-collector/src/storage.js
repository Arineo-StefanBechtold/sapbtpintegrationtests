const fs = require('node:fs/promises');
const path = require('node:path');
const { randomUUID } = require('node:crypto');

const DEFAULT_RUN = 'default';

class FileCollectorStorage {
  constructor(options = {}) {
    this.dataDir = options.dataDir || path.resolve(process.cwd(), 'data');
    this.currentRunFile = path.join(this.dataDir, 'current-run.json');
    this.locks = new Map();
  }

  async init() {
    await fs.mkdir(this.dataDir, { recursive: true });
  }

  async health() {
    await this.init();
    const probeDir = path.join(this.dataDir, '.health');
    await fs.mkdir(probeDir, { recursive: true });
    await fs.rm(probeDir, { recursive: true, force: true });
    return { status: 'UP', dataDir: this.dataDir };
  }

  async createRun(runId = randomUUID()) {
    const normalizedRunId = this.#safeSegment(runId, 'runId');
    await this.init();
    const manifest = this.#emptyManifest(normalizedRunId);
    await this.#writeManifest(normalizedRunId, manifest);
    await this.#writeCurrentRun(normalizedRunId);
    return { runId: normalizedRunId };
  }

  async getCurrentRun() {
    try {
      const content = await fs.readFile(this.currentRunFile, 'utf8');
      return JSON.parse(content);
    } catch (error) {
      if (error.code === 'ENOENT') {
        return null;
      }
      throw error;
    }
  }

  async endRun(runId) {
    const normalizedRunId = this.#safeSegment(runId, 'runId');
    const currentRun = await this.getCurrentRun();
    if (currentRun && currentRun.runId === normalizedRunId) {
      await fs.rm(this.currentRunFile, { force: true });
    }
    return { runId: normalizedRunId, ended: true };
  }

  async deleteRun(runId) {
    const normalizedRunId = this.#safeSegment(runId, 'runId');
    await fs.rm(this.#runDir(normalizedRunId), { recursive: true, force: true });
    const currentRun = await this.getCurrentRun();
    if (currentRun && currentRun.runId === normalizedRunId) {
      await fs.rm(this.currentRunFile, { force: true });
    }
  }

  async collect({ runId, messageId, payload, headers, contentType }) {
    if (!messageId) {
      const error = new Error('Missing messageId');
      error.statusCode = 400;
      throw error;
    }

    const normalizedMessageId = this.#safeSegment(messageId, 'messageId');
    const effectiveRunId = this.#safeSegment(runId || (await this.getCurrentRun())?.runId || DEFAULT_RUN, 'runId');
    const lockKey = `${effectiveRunId}:${normalizedMessageId}`;
    return this.#withLock(lockKey, async () => {
      await this.init();
      const manifest = await this.#readManifest(effectiveRunId);
      const existing = manifest.messages[normalizedMessageId] || [];
      const sequenceNumber = existing.length + 1;
      const messageDir = this.#messageDir(effectiveRunId, normalizedMessageId);
      await fs.mkdir(messageDir, { recursive: true });
      const payloadFile = path.join(messageDir, `${sequenceNumber}.payload`);
      const headerFile = path.join(messageDir, `${sequenceNumber}.headers.json`);
      await fs.writeFile(payloadFile, payload);
      await fs.writeFile(headerFile, JSON.stringify(headers, null, 2));

      const entry = {
        sequenceNumber,
        contentType,
        storedAt: new Date().toISOString(),
        payloadPath: path.relative(this.#runDir(effectiveRunId), payloadFile).replace(/\\/g, '/'),
        headerPath: path.relative(this.#runDir(effectiveRunId), headerFile).replace(/\\/g, '/'),
      };
      manifest.messages[normalizedMessageId] = existing.concat(entry);
      manifest.updatedAt = new Date().toISOString();
      await this.#writeManifest(effectiveRunId, manifest);
      return {
        runId: effectiveRunId,
        messageId: normalizedMessageId,
        sequenceNumber,
        contentType,
        location: `/runs/${encodeURIComponent(effectiveRunId)}/messages/${encodeURIComponent(normalizedMessageId)}`,
      };
    });
  }

  async getRunMessages(runId) {
    const normalizedRunId = this.#safeSegment(runId, 'runId');
    const manifest = await this.#readManifest(normalizedRunId);
    return {
      runId: normalizedRunId,
      messages: Object.entries(manifest.messages)
        .sort(([left], [right]) => left.localeCompare(right))
        .map(([messageId, documents]) => ({ messageId, documents })),
      releasedMessageIds: [...manifest.releasedMessageIds].sort(),
      createdAt: manifest.createdAt,
      updatedAt: manifest.updatedAt,
    };
  }

  async getMessage(runId, messageId) {
    const normalizedRunId = this.#safeSegment(runId, 'runId');
    const normalizedMessageId = this.#safeSegment(messageId, 'messageId');
    const manifest = await this.#readManifest(normalizedRunId);
    const documents = manifest.messages[normalizedMessageId];
    if (!documents) {
      return null;
    }
    return { runId: normalizedRunId, messageId: normalizedMessageId, documents };
  }

  async getPayload(runId, messageId, sequenceNumber) {
    const normalizedRunId = this.#safeSegment(runId, 'runId');
    const document = await this.#getDocument(normalizedRunId, messageId, sequenceNumber);
    if (!document) {
      return null;
    }
    return {
      contentType: document.contentType,
      payload: await fs.readFile(path.join(this.#runDir(normalizedRunId), document.payloadPath)),
    };
  }

  async getHeader(runId, messageId, sequenceNumber) {
    const normalizedRunId = this.#safeSegment(runId, 'runId');
    const document = await this.#getDocument(normalizedRunId, messageId, sequenceNumber);
    if (!document) {
      return null;
    }
    const content = await fs.readFile(path.join(this.#runDir(normalizedRunId), document.headerPath), 'utf8');
    return JSON.parse(content);
  }

  async releaseMessage(runId, messageId) {
    const normalizedRunId = this.#safeSegment(runId, 'runId');
    const normalizedMessageId = this.#safeSegment(messageId, 'messageId');
    const manifest = await this.#readManifest(normalizedRunId);
    if (!manifest.messages[normalizedMessageId]) {
      return false;
    }
    delete manifest.messages[normalizedMessageId];
    manifest.releasedMessageIds = Array.from(new Set([...manifest.releasedMessageIds, normalizedMessageId]));
    manifest.updatedAt = new Date().toISOString();
    await fs.rm(this.#messageDir(normalizedRunId, normalizedMessageId), { recursive: true, force: true });
    await this.#writeManifest(normalizedRunId, manifest);
    return true;
  }

  async getResidual(runId) {
    const normalizedRunId = this.#safeSegment(runId, 'runId');
    const manifest = await this.#readManifest(normalizedRunId);
    return {
      runId: normalizedRunId,
      residualMessages: Object.entries(manifest.messages)
        .sort(([left], [right]) => left.localeCompare(right))
        .map(([messageId, documents]) => ({
          messageId,
          documentCount: documents.length,
          sequenceNumbers: documents.map((document) => document.sequenceNumber),
        })),
      releasedMessageIds: [...manifest.releasedMessageIds].sort(),
    };
  }

  async #getDocument(runId, messageId, sequenceNumber) {
    const message = await this.getMessage(runId, messageId);
    if (!message) {
      return null;
    }
    const document = message.documents.find((entry) => entry.sequenceNumber === sequenceNumber) || message.documents[0];
    if (!document) {
      return null;
    }
    return document;
  }

  #runDir(runId) {
    return path.join(this.dataDir, this.#safeSegment(runId, 'runId'));
  }

  #messageDir(runId, messageId) {
    return path.join(this.#runDir(runId), this.#safeSegment(messageId, 'messageId'));
  }

  async #readManifest(runId) {
    const runDir = this.#runDir(runId);
    await fs.mkdir(runDir, { recursive: true });
    const manifestFile = path.join(runDir, 'manifest.json');
    try {
      const content = await fs.readFile(manifestFile, 'utf8');
      const parsed = JSON.parse(content);
      parsed.messages ||= {};
      parsed.releasedMessageIds ||= [];
      return parsed;
    } catch (error) {
      if (error.code !== 'ENOENT') {
        throw error;
      }
      const manifest = this.#emptyManifest(runId);
      await this.#writeManifest(runId, manifest);
      return manifest;
    }
  }

  async #writeManifest(runId, manifest) {
    const runDir = this.#runDir(runId);
    await fs.mkdir(runDir, { recursive: true });
    await fs.writeFile(path.join(runDir, 'manifest.json'), JSON.stringify(manifest, null, 2));
  }

  async #writeCurrentRun(runId) {
    await fs.writeFile(this.currentRunFile, JSON.stringify({ runId }, null, 2));
  }

  #emptyManifest(runId) {
    const now = new Date().toISOString();
    return {
      runId,
      createdAt: now,
      updatedAt: now,
      releasedMessageIds: [],
      messages: {},
    };
  }

  #safeSegment(value, fieldName) {
    const normalized = String(value || '').trim();
    if (!normalized || !/^[A-Za-z0-9._-]+$/.test(normalized)) {
      const error = new Error(`Invalid ${fieldName}`);
      error.statusCode = 400;
      throw error;
    }
    return normalized;
  }

  async #withLock(key, task) {
    const previous = this.locks.get(key) || Promise.resolve();
    const next = previous.then(task, task);
    this.locks.set(key, next.catch(() => {}));
    try {
      return await next;
    } finally {
      if (this.locks.get(key) === next) {
        this.locks.delete(key);
      }
    }
  }
}

module.exports = {
  FileCollectorStorage,
};

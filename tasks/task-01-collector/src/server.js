const path = require('node:path');
const { createApp } = require('./app');

async function start() {
  const port = Number(process.env.PORT || '8080');
  const host = process.env.HOST || '127.0.0.1';
  const dataDir = process.env.COLLECTOR_DATA_DIR || path.resolve(process.cwd(), 'data');
  const { app } = createApp({ dataDir });
  try {
    await app.listen({ port, host });
    console.log(`collector-listening:${host}:${port}`);
  } catch (error) {
    console.error(error);
    process.exit(1);
  }
}

start();

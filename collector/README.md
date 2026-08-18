# CPI Test Collector

A minimal HTTP collector service for CPI integration tests.

## Quick Start

```bash
npm install
npm start
```

Server runs on `http://localhost:8080` by default.

## Configuration

| Variable              | Default    | Description                            |
|-----------------------|------------|----------------------------------------|
| `PORT`                | `8080`     | HTTP port                              |
| `HOST`                | `0.0.0.0`  | Bind address                           |
| `COLLECTOR_DATA_DIR`  | `./data`   | Base directory for stored data         |
| `COLLECTOR_TTL_HOURS` | `2`        | TTL for runs in hours                  |

## API

See [`api-contract/openapi.yaml`](./api-contract/openapi.yaml) for the full contract.

### Key endpoints

| Method   | Path                                              | Description                        |
|----------|---------------------------------------------------|------------------------------------|
| GET      | `/health`                                         | Health check                       |
| POST     | `/collect`                                        | Store a document                   |
| GET      | `/runs`                                           | List all runs                      |
| DELETE   | `/runs/:runId`                                    | Delete a run                       |
| GET      | `/runs/:runId/messages`                           | List messages in a run             |
| GET      | `/runs/:runId/messages/:messageId/payload`        | Get raw payload (latest seq)       |
| GET      | `/runs/:runId/messages/:messageId/header`         | Get headers as JSON (latest seq)   |
| DELETE   | `/runs/:runId/messages/:messageId/release`        | Release a message group            |
| GET      | `/runs/:runId/residual`                           | Get residual items                 |

### Ingest headers

| Header            | Required | Description                          |
|-------------------|----------|--------------------------------------|
| `X-Message-Id`    | Yes      | Message identifier                   |
| `X-Test-Run-Id`   | No       | Run identifier (default: `default`)  |

## Tests

```bash
npm test
```

## Storage Structure

```
data/
  {testRunId}/
    manifest.json
    {messageId}/
      1.payload
      1.headers.json
      2.payload
      2.headers.json
```

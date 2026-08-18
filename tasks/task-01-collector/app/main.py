from __future__ import annotations

import os
import uuid
from pathlib import Path

from fastapi import FastAPI, HTTPException, Query, Request, Response
from fastapi.responses import JSONResponse

from app.storage import DEFAULT_RUN_ID, FileStorage


DEFAULT_STORAGE_DIR = Path("./data")
MESSAGE_ID_HEADERS = ("x-message-id", "message-id", "message_id")


def create_app(storage_dir: str | Path | None = None) -> FastAPI:
    app = FastAPI(title="CPI Test Collector API", version="0.1.0")

    resolved_storage_dir = Path(storage_dir or os.getenv("COLLECTOR_STORAGE_DIR") or DEFAULT_STORAGE_DIR)
    storage = FileStorage(resolved_storage_dir)

    @app.exception_handler(ValueError)
    async def handle_value_error(_: Request, exc: ValueError) -> JSONResponse:
        return JSONResponse(status_code=400, content={"detail": str(exc)})

    @app.get("/health")
    def health() -> Response:
        if storage.ensure_available():
            return Response(status_code=200)
        return Response(status_code=503)

    @app.post("/collect", status_code=202)
    async def collect(request: Request) -> JSONResponse:
        if not storage.ensure_available():
            raise HTTPException(status_code=503, detail="Storage unavailable")

        payload = await request.body()
        message_id = _resolve_message_id(request)
        run_id = storage.get_current_run() or DEFAULT_RUN_ID

        metadata = storage.collect(run_id=run_id, message_id=message_id, payload=payload, headers=dict(request.headers))
        return JSONResponse(status_code=202, content=metadata)

    @app.post("/runs", status_code=201)
    def create_run() -> dict:
        if not storage.ensure_available():
            raise HTTPException(status_code=503, detail="Storage unavailable")
        run_id = storage.create_run()
        return {"runId": run_id}

    @app.get("/runs/current")
    def get_current_run() -> dict:
        run_id = storage.get_current_run()
        if run_id is None:
            raise HTTPException(status_code=404, detail="No active run")
        return {"runId": run_id}

    @app.post("/runs/{run_id}/end")
    def end_run(run_id: str) -> dict:
        if not storage.end_run(run_id):
            raise HTTPException(status_code=404, detail="Run not found")
        return {"runId": run_id, "ended": True}

    @app.delete("/runs/{run_id}", status_code=204)
    def delete_run(run_id: str) -> Response:
        if not storage.delete_run(run_id):
            raise HTTPException(status_code=404, detail="Run not found")
        return Response(status_code=204)

    @app.get("/runs/{run_id}/messages")
    def get_manifest(run_id: str) -> dict:
        return storage.get_manifest(run_id)

    @app.get("/runs/{run_id}/messages/{message_id}")
    def get_message(run_id: str, message_id: str) -> dict:
        group = storage.get_message_group(run_id, message_id)
        if group is None:
            raise HTTPException(status_code=404, detail="Message not found")
        return group

    @app.get("/runs/{run_id}/messages/{message_id}/payload")
    def get_payload(run_id: str, message_id: str, seq: int | None = Query(default=None, ge=1)) -> Response:
        payload = storage.get_payload(run_id, message_id, sequence=seq)
        if payload is None:
            raise HTTPException(status_code=404, detail="Payload not found")
        return Response(content=payload, media_type="application/octet-stream")

    @app.get("/runs/{run_id}/messages/{message_id}/header")
    def get_header(run_id: str, message_id: str, seq: int | None = Query(default=None, ge=1)) -> dict:
        headers = storage.get_headers(run_id, message_id, sequence=seq)
        if headers is None:
            raise HTTPException(status_code=404, detail="Header not found")
        return headers

    @app.delete("/runs/{run_id}/messages/{message_id}/release", status_code=204)
    def release_message(run_id: str, message_id: str) -> Response:
        if not storage.release_message(run_id, message_id):
            raise HTTPException(status_code=404, detail="Message not found")
        return Response(status_code=204)

    @app.get("/runs/{run_id}/residual")
    def residual(run_id: str) -> dict:
        return storage.residual(run_id)

    return app


def _resolve_message_id(request: Request) -> str:
    for header_name in MESSAGE_ID_HEADERS:
        value = request.headers.get(header_name)
        if value and value.strip():
            return value.strip()
    return uuid.uuid4().hex


app = create_app()

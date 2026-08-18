from __future__ import annotations

from pathlib import Path

from fastapi.testclient import TestClient

from app.main import create_app


def _client(tmp_path: Path) -> TestClient:
    app = create_app(tmp_path / "storage")
    return TestClient(app)


def test_health_ok_when_storage_available(tmp_path: Path) -> None:
    client = _client(tmp_path)

    response = client.get("/health")

    assert response.status_code == 200


def test_health_503_when_storage_unavailable(tmp_path: Path) -> None:
    storage_file = tmp_path / "storage-as-file"
    storage_file.write_text("x", encoding="utf-8")
    app = create_app(storage_file)
    client = TestClient(app)

    response = client.get("/health")

    assert response.status_code == 503


def test_collect_without_active_run_is_stored_in_default_run(tmp_path: Path) -> None:
    client = _client(tmp_path)

    response = client.post(
        "/collect",
        content=b"raw payload",
        headers={"x-message-id": "msg-1", "x-custom": "test"},
    )

    assert response.status_code == 202
    body = response.json()
    assert body["runId"] == "default"
    assert body["messageId"] == "msg-1"
    assert body["sequence"] == 1
    assert "storedAt" in body

    payload_response = client.get("/runs/default/messages/msg-1/payload")
    assert payload_response.status_code == 200
    assert payload_response.content == b"raw payload"

    header_response = client.get("/runs/default/messages/msg-1/header")
    assert header_response.status_code == 200
    assert header_response.json()["x-custom"] == "test"


def test_same_message_id_creates_multiple_sequences_without_overwrite(tmp_path: Path) -> None:
    client = _client(tmp_path)

    first = client.post("/collect", content=b"first", headers={"x-message-id": "same-id", "x-custom": "1"})
    second = client.post("/collect", content=b"second", headers={"x-message-id": "same-id", "x-custom": "2"})

    assert first.status_code == 202
    assert second.status_code == 202
    assert first.json()["sequence"] == 1
    assert second.json()["sequence"] == 2

    payload_one = client.get("/runs/default/messages/same-id/payload", params={"seq": 1})
    payload_two = client.get("/runs/default/messages/same-id/payload", params={"seq": 2})

    assert payload_one.content == b"first"
    assert payload_two.content == b"second"


def test_manifest_and_message_group_and_separate_payload_header_retrieval(tmp_path: Path) -> None:
    client = _client(tmp_path)

    client.post("/collect", content=b"one", headers={"x-message-id": "m-1", "x-a": "a"})
    client.post("/collect", content=b"two", headers={"x-message-id": "m-1", "x-a": "b"})

    manifest = client.get("/runs/default/messages")
    assert manifest.status_code == 200
    messages = manifest.json()["messages"]
    assert len(messages) == 1
    assert messages[0]["messageId"] == "m-1"
    assert messages[0]["sequences"] == [1, 2]

    group = client.get("/runs/default/messages/m-1")
    assert group.status_code == 200
    entries = group.json()["entries"]
    assert [entry["sequence"] for entry in entries] == [1, 2]

    latest_payload = client.get("/runs/default/messages/m-1/payload")
    latest_header = client.get("/runs/default/messages/m-1/header")

    assert latest_payload.content == b"two"
    assert latest_header.json()["x-a"] == "b"


def test_release_removes_group_and_new_messages_start_new_sequence(tmp_path: Path) -> None:
    client = _client(tmp_path)

    client.post("/collect", content=b"old", headers={"x-message-id": "release-me"})

    release = client.delete("/runs/default/messages/release-me/release")
    assert release.status_code == 204

    not_found_group = client.get("/runs/default/messages/release-me")
    assert not_found_group.status_code == 404

    new_collect = client.post("/collect", content=b"new", headers={"x-message-id": "release-me"})
    assert new_collect.status_code == 202
    assert new_collect.json()["sequence"] == 1


def test_run_lifecycle_and_residual_endpoint(tmp_path: Path) -> None:
    client = _client(tmp_path)

    created = client.post("/runs")
    assert created.status_code == 201
    run_id = created.json()["runId"]

    current = client.get("/runs/current")
    assert current.status_code == 200
    assert current.json()["runId"] == run_id

    collect = client.post("/collect", content=b"run-payload", headers={"x-message-id": "run-msg"})
    assert collect.status_code == 202
    assert collect.json()["runId"] == run_id

    residual = client.get(f"/runs/{run_id}/residual")
    assert residual.status_code == 200
    assert residual.json()["messageGroups"] == 1
    assert residual.json()["entries"] == 1

    ended = client.post(f"/runs/{run_id}/end")
    assert ended.status_code == 200

    no_current = client.get("/runs/current")
    assert no_current.status_code == 404

    deleted = client.delete(f"/runs/{run_id}")
    assert deleted.status_code == 204

    deleted_manifest = client.get(f"/runs/{run_id}/messages")
    assert deleted_manifest.status_code == 200
    assert deleted_manifest.json()["messages"] == []

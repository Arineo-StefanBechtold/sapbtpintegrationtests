from __future__ import annotations

import json
import re
import shutil
import uuid
from dataclasses import dataclass
from datetime import UTC, datetime
from pathlib import Path
from tempfile import NamedTemporaryFile


DEFAULT_RUN_ID = "default"
_RUN_ID_PATTERN = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")
_INDEX_FILE = "messages-index.json"


@dataclass(frozen=True)
class MessageEntry:
    sequence: int
    payload_path: Path
    header_path: Path
    created_at: str


class FileStorage:
    def __init__(self, root: Path) -> None:
        self.root = root.resolve()
        self.runs_root = self.root / "runs"
        self.current_run_file = self.root / "current_run.txt"

    def ensure_available(self) -> bool:
        try:
            self.runs_root.mkdir(parents=True, exist_ok=True)
            with NamedTemporaryFile(dir=self.root, delete=True):
                pass
            return True
        except OSError:
            return False

    def create_run(self) -> str:
        self.clear_all_runs()
        run_id = uuid.uuid4().hex
        self._create_run_dir(run_id)
        self.set_current_run(run_id)
        return run_id

    def get_current_run(self) -> str | None:
        if not self.current_run_file.exists():
            return None
        run_id = self.current_run_file.read_text(encoding="utf-8").strip()
        if not run_id:
            return None
        try:
            return _validated_run_id(run_id)
        except ValueError:
            return None

    def set_current_run(self, run_id: str | None) -> None:
        if run_id is None:
            if self.current_run_file.exists():
                self.current_run_file.unlink()
            return
        run_id = _validated_run_id(run_id)
        self.root.mkdir(parents=True, exist_ok=True)
        self.current_run_file.write_text(run_id, encoding="utf-8")

    def end_run(self, run_id: str) -> bool:
        run_id = _validated_run_id(run_id)
        run_dir = self._find_run_dir(run_id)
        if run_dir is None:
            return False

        run_meta = run_dir / "run.json"
        if run_meta.exists():
            data = self._read_json(run_meta)
            data["endedAt"] = _now_iso()
            self._write_json(run_meta, data)

        if self.get_current_run() == run_id:
            self.set_current_run(None)
        return True

    def delete_run(self, run_id: str) -> bool:
        run_id = _validated_run_id(run_id)
        run_dir = self._find_run_dir(run_id)
        if run_dir is None:
            return False

        shutil.rmtree(run_dir)
        if self.get_current_run() == run_id:
            self.set_current_run(None)
        return True

    def clear_all_runs(self) -> None:
        if self.runs_root.exists():
            shutil.rmtree(self.runs_root)
        self.runs_root.mkdir(parents=True, exist_ok=True)

    def collect(self, run_id: str, message_id: str, payload: bytes, headers: dict[str, str]) -> dict:
        run_id = _validated_run_id(run_id)
        run_dir = self._ensure_run_dir(run_id)

        message_key, message_dir = self._resolve_message_group(run_dir, message_id)
        sequence = self._reserve_next_sequence(message_dir)

        entry_dir = message_dir / str(sequence)
        entry_dir.mkdir(parents=True, exist_ok=False)

        payload_path = entry_dir / "payload.bin"
        header_path = entry_dir / "headers.json"
        metadata_path = entry_dir / "metadata.json"

        payload_path.write_bytes(payload)
        self._write_json(header_path, headers)

        created_at = _now_iso()
        self._write_json(
            metadata_path,
            {
                "messageId": message_id,
                "messageKey": message_key,
                "sequence": sequence,
                "runId": run_id,
                "createdAt": created_at,
            },
        )

        return {
            "runId": run_id,
            "messageId": message_id,
            "messageKey": message_key,
            "sequence": sequence,
            "storedAt": str(entry_dir.relative_to(self.root)),
            "createdAt": created_at,
        }

    def get_manifest(self, run_id: str) -> dict:
        run_id = _validated_run_id(run_id)
        run_dir = self._find_run_dir(run_id)
        if run_dir is None:
            return {"runId": run_id, "messages": []}

        messages_root = run_dir / "messages"
        if not messages_root.exists():
            return {"runId": run_id, "messages": []}

        messages: list[dict] = []
        for message_dir in sorted(path for path in messages_root.iterdir() if path.is_dir()):
            entries = self._message_entries(message_dir)
            if not entries:
                continue
            message_id = self._read_message_id(entries[0])
            messages.append(
                {
                    "messageId": message_id,
                    "messageKey": message_dir.name,
                    "count": len(entries),
                    "sequences": [entry.sequence for entry in entries],
                    "latestSequence": entries[-1].sequence,
                }
            )

        messages.sort(key=lambda item: item["messageId"])
        return {"runId": run_id, "messages": messages}

    def get_message_group(self, run_id: str, message_id: str) -> dict | None:
        run_id = _validated_run_id(run_id)
        message_dir = self._message_dir_for_id(run_id, message_id)
        if message_dir is None or not message_dir.exists():
            return None

        entries = self._message_entries(message_dir)
        if not entries:
            return None

        return {
            "runId": run_id,
            "messageId": message_id,
            "messageKey": message_dir.name,
            "entries": [
                {
                    "sequence": entry.sequence,
                    "createdAt": entry.created_at,
                    "payloadPath": str(entry.payload_path.relative_to(self.root)),
                    "headerPath": str(entry.header_path.relative_to(self.root)),
                }
                for entry in entries
            ],
        }

    def get_headers(self, run_id: str, message_id: str, sequence: int | None = None) -> dict | None:
        run_id = _validated_run_id(run_id)
        entry = self._get_entry(run_id, message_id, sequence)
        if entry is None:
            return None
        return self._read_json(entry.header_path)

    def get_payload(self, run_id: str, message_id: str, sequence: int | None = None) -> bytes | None:
        run_id = _validated_run_id(run_id)
        entry = self._get_entry(run_id, message_id, sequence)
        if entry is None:
            return None
        return entry.payload_path.read_bytes()

    def release_message(self, run_id: str, message_id: str) -> bool:
        run_id = _validated_run_id(run_id)
        run_dir = self._find_run_dir(run_id)
        if run_dir is None:
            return False

        message_dir = self._message_dir_for_id(run_id, message_id)
        if message_dir is None or not message_dir.exists():
            return False

        shutil.rmtree(message_dir)
        self._remove_message_from_index(run_dir, message_id)
        return True

    def residual(self, run_id: str) -> dict:
        run_id = _validated_run_id(run_id)
        manifest = self.get_manifest(run_id)
        return {
            "runId": run_id,
            "messageGroups": len(manifest["messages"]),
            "entries": sum(item["count"] for item in manifest["messages"]),
            "messages": manifest["messages"],
        }

    def _ensure_run_dir(self, run_id: str) -> Path:
        run_dir = self._find_run_dir(run_id)
        if run_dir is not None:
            return run_dir
        return self._create_run_dir(run_id)

    def _create_run_dir(self, run_id: str) -> Path:
        run_internal_key = uuid.uuid4().hex
        run_dir = self.runs_root / run_internal_key
        run_dir.mkdir(parents=True, exist_ok=False)
        self._write_json(
            run_dir / "run.json",
            {"runId": run_id, "createdAt": _now_iso(), "endedAt": None},
        )
        self._write_json(run_dir / _INDEX_FILE, {})
        return run_dir

    def _find_run_dir(self, run_id: str) -> Path | None:
        if not self.runs_root.exists():
            return None

        for run_dir in self.runs_root.iterdir():
            if not run_dir.is_dir():
                continue
            run_meta = run_dir / "run.json"
            if not run_meta.exists():
                continue
            meta = self._read_json(run_meta)
            if meta.get("runId") == run_id:
                return run_dir
        return None

    def _message_dir_for_id(self, run_id: str, message_id: str) -> Path | None:
        run_dir = self._find_run_dir(run_id)
        if run_dir is None:
            return None
        mapping = self._read_index(run_dir)
        message_key = mapping.get(message_id)
        if not message_key:
            return None
        return run_dir / "messages" / message_key

    def _resolve_message_group(self, run_dir: Path, message_id: str) -> tuple[str, Path]:
        mapping = self._read_index(run_dir)
        message_key = mapping.get(message_id)
        if message_key is None:
            message_key = uuid.uuid4().hex
            mapping[message_id] = message_key
            self._write_index(run_dir, mapping)

        message_dir = run_dir / "messages" / message_key
        message_dir.mkdir(parents=True, exist_ok=True)
        return message_key, message_dir

    def _reserve_next_sequence(self, message_dir: Path) -> int:
        existing_sequences = [int(path.name) for path in message_dir.iterdir() if path.is_dir() and path.name.isdigit()]
        next_sequence = max(existing_sequences, default=0) + 1
        while (message_dir / str(next_sequence)).exists():
            next_sequence += 1
        return next_sequence

    def _get_entry(self, run_id: str, message_id: str, sequence: int | None = None) -> MessageEntry | None:
        message_dir = self._message_dir_for_id(run_id, message_id)
        if message_dir is None or not message_dir.exists():
            return None

        entries = self._message_entries(message_dir)
        if not entries:
            return None

        if sequence is None:
            return entries[-1]
        for entry in entries:
            if entry.sequence == sequence:
                return entry
        return None

    def _message_entries(self, message_dir: Path) -> list[MessageEntry]:
        entries: list[MessageEntry] = []

        for entry_dir in message_dir.iterdir():
            if not entry_dir.is_dir() or not entry_dir.name.isdigit():
                continue

            metadata_file = entry_dir / "metadata.json"
            if not metadata_file.exists():
                continue

            meta = self._read_json(metadata_file)
            payload_path = entry_dir / "payload.bin"
            header_path = entry_dir / "headers.json"

            if not payload_path.exists() or not header_path.exists():
                continue

            entries.append(
                MessageEntry(
                    sequence=int(entry_dir.name),
                    payload_path=payload_path,
                    header_path=header_path,
                    created_at=str(meta.get("createdAt", "")),
                )
            )

        entries.sort(key=lambda item: item.sequence)
        return entries

    def _read_message_id(self, entry: MessageEntry) -> str:
        metadata_path = entry.payload_path.parent / "metadata.json"
        meta = self._read_json(metadata_path)
        return str(meta.get("messageId", ""))

    def _read_index(self, run_dir: Path) -> dict[str, str]:
        index_file = run_dir / _INDEX_FILE
        if not index_file.exists():
            return {}
        content = self._read_json(index_file)
        return {str(key): str(value) for key, value in content.items()}

    def _write_index(self, run_dir: Path, mapping: dict[str, str]) -> None:
        self._write_json(run_dir / _INDEX_FILE, mapping)

    def _remove_message_from_index(self, run_dir: Path, message_id: str) -> None:
        mapping = self._read_index(run_dir)
        if message_id in mapping:
            mapping.pop(message_id)
            self._write_index(run_dir, mapping)

    @staticmethod
    def _write_json(path: Path, data: dict) -> None:
        path.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")

    @staticmethod
    def _read_json(path: Path) -> dict:
        return json.loads(path.read_text(encoding="utf-8"))


def _validated_run_id(run_id: str) -> str:
    if not _RUN_ID_PATTERN.fullmatch(run_id):
        raise ValueError("Invalid run id")
    return run_id


def _now_iso() -> str:
    return datetime.now(UTC).isoformat()

#!/usr/bin/env python3
"""Migrate Colecciones.xlsx + Drive images into Firestore + Firebase Storage.

Usage
-----
    pip install firebase-admin openpyxl requests

    export GOOGLE_APPLICATION_CREDENTIALS=~/.config/batman-collection-sa.json

    # Dry-run first — prints the planned mapping, touches nothing.
    python tools/migrate_colecciones.py \\
        --xlsx ~/Downloads/Colecciones.xlsx \\
        --project-id batman-collection \\
        --dry-run

    # Real run.
    python tools/migrate_colecciones.py \\
        --xlsx ~/Downloads/Colecciones.xlsx \\
        --project-id batman-collection \\
        [--bucket batman-collection.firebasestorage.app]

The service-account JSON must have Firestore + Storage write access.
Drive images are fetched over the public URL, so the Drive folder must be
"Anyone with the link, Viewer" for the duration of the run.
"""

from __future__ import annotations

import argparse
import mimetypes
import re
import sys
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path

import openpyxl
import requests


SHEET_NAME = "Lista"
VALID_TIPOS = {"Funko", "Hot Wheels", "Otros"}
DRIVE_ID_RE = re.compile(r"https://drive\.google\.com/open\?id=([A-Za-z0-9_-]+)")


@dataclass
class PlannedFigure:
    sheet_row: int
    tipo: str
    nombre: str
    serie: str
    file_id: str
    doc_id: str


def slugify(value: str) -> str:
    s = value.lower()
    s = re.sub(r"[^a-z0-9]+", "-", s)
    return s.strip("-")


def deterministic_id(tipo: str, nombre: str, serie: str) -> str:
    return "-".join(part for part in (slugify(tipo), slugify(nombre), slugify(serie)) if part)


def extract_file_id(url: str) -> str | None:
    match = DRIVE_ID_RE.match(url or "")
    return match.group(1) if match else None


def _serie_to_str(raw) -> str:
    # openpyxl reads numeric cells as float — "1187" comes back as 1187.0.
    if isinstance(raw, float) and raw.is_integer():
        return str(int(raw))
    return str(raw or "").strip()


def read_rows(xlsx_path: Path):
    wb = openpyxl.load_workbook(xlsx_path, data_only=True)
    sheet = wb[SHEET_NAME]
    header_seen = False
    for i, raw in enumerate(sheet.iter_rows(values_only=True), start=1):
        if not header_seen:
            header_seen = True
            continue
        if not any(cell is not None for cell in raw):
            continue
        _, tipo, nombre, serie, image_url = raw
        yield (
            i,
            (tipo or "").strip(),
            (nombre or "").strip(),
            _serie_to_str(serie),
            (image_url or "").strip(),
        )


def plan(rows) -> list[PlannedFigure]:
    planned: list[PlannedFigure] = []
    # base-id -> count of times we've seen it so far; duplicates after the first get a
    # "-2", "-3", ... suffix so a repeat run still produces the same ids.
    seen: dict[str, int] = {}
    for sheet_row, tipo, nombre, serie, url in rows:
        reason = None
        if tipo not in VALID_TIPOS:
            reason = f"unknown Tipo {tipo!r}"
        elif not nombre:
            reason = "missing Nombre"
        elif not serie:
            reason = "missing Serie"
        file_id = extract_file_id(url)
        if reason is None and not file_id:
            reason = f"no Drive file id in {url!r}"
        if reason:
            print(f"[row {sheet_row}] SKIP: {reason}", file=sys.stderr)
            continue
        base_id = deterministic_id(tipo, nombre, serie)
        occurrence = seen.get(base_id, 0) + 1
        seen[base_id] = occurrence
        doc_id = base_id if occurrence == 1 else f"{base_id}-{occurrence}"
        planned.append(
            PlannedFigure(
                sheet_row=sheet_row,
                tipo=tipo,
                nombre=nombre,
                serie=serie,
                file_id=file_id,  # type: ignore[arg-type]
                doc_id=doc_id,
            )
        )
    return planned


def print_plan(planned: list[PlannedFigure]) -> None:
    width = max((len(p.doc_id) for p in planned), default=10)
    print()
    print(f"{'DOC_ID':<{width}}  {'TIPO':<10}  {'SERIE':<24}  NOMBRE")
    print("-" * (width + 10 + 24 + 20))
    for p in planned:
        print(f"{p.doc_id:<{width}}  {p.tipo:<10}  {p.serie:<24}  {p.nombre}")


def download_drive_file(file_id: str) -> tuple[bytes, str]:
    url = f"https://drive.google.com/uc?export=download&id={file_id}"
    response = requests.get(url, allow_redirects=True, timeout=60)
    response.raise_for_status()
    content_type = response.headers.get("Content-Type", "")
    if content_type.startswith("text/html"):
        raise RuntimeError(
            f"Drive returned HTML for {file_id} — the file is likely not public, "
            "or large enough to require the virus-scan confirm step."
        )
    return response.content, content_type


def ext_from_mime(content_type: str) -> str:
    main = content_type.split(";", 1)[0].strip()
    ext = mimetypes.guess_extension(main) or ".bin"
    return {".jpe": ".jpg", ".jpeg": ".jpg"}.get(ext, ext)


def do_migrate(planned: list[PlannedFigure], project_id: str, bucket: str) -> None:
    import firebase_admin
    from firebase_admin import credentials, firestore, storage

    firebase_admin.initialize_app(
        credentials.ApplicationDefault(),
        {"projectId": project_id, "storageBucket": bucket},
    )
    db = firestore.client()
    bkt = storage.bucket()
    now = datetime.now(timezone.utc)

    ok = 0
    for p in planned:
        print(f"[row {p.sheet_row}] {p.doc_id} ...", end=" ", flush=True)
        try:
            data, content_type = download_drive_file(p.file_id)
            ext = ext_from_mime(content_type)
            blob = bkt.blob(f"figures/{p.doc_id}{ext}")
            blob.upload_from_string(data, content_type=content_type)
            blob.make_public()
            db.collection("figures").document(p.doc_id).set(
                {
                    "tipo": p.tipo,
                    "nombre": p.nombre,
                    "serie": p.serie,
                    "imageUrl": blob.public_url,
                    "createdAt": now,
                }
            )
            ok += 1
            print("ok")
        except Exception as exc:  # noqa: BLE001 — one-off script, log and continue
            print(f"FAILED: {exc}")

    print(f"\nDone. {ok}/{len(planned)} figures migrated.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--xlsx", type=Path, required=True)
    parser.add_argument("--project-id", required=True)
    parser.add_argument(
        "--bucket",
        help="Storage bucket; defaults to <project-id>.appspot.com",
    )
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    rows = list(read_rows(args.xlsx))
    print(f"Loaded {len(rows)} data rows from {args.xlsx}")

    planned = plan(rows)
    print(f"Planned {len(planned)} figures.")

    if args.dry_run:
        print_plan(planned)
        return

    bucket = args.bucket or f"{args.project_id}.appspot.com"
    do_migrate(planned, args.project_id, bucket)


if __name__ == "__main__":
    main()

# tools

One-off scripts that aren't shipped with the app.

## `migrate_colecciones.py`

Migrates the pre-Firebase collection (an `.xlsx` export of the Google Form responses
sheet, with Drive-hosted images) into Firestore + Firebase Storage.

### Target schema

```
figures/<id>  (document)
  tipo:        "Funko" | "Hot Wheels" | "Otros"
  nombre:      "The Batman"
  serie:       "1187"
  imageUrl:    "https://storage.googleapis.com/.../figures/<id>.jpg"
  createdAt:   Timestamp(now)
```

`<id>` is `slug(tipo)-slug(nombre)-slug(serie)`. True duplicates (same tipo + nombre +
serie bought twice) get `-2`, `-3`, ... suffixes so re-running is idempotent.

### Prereqs

1. **Firebase project** with Firestore (Native) and Storage enabled.
2. **Service-account JSON** with Firestore + Storage write access, downloaded from
   Project Settings → Service accounts → Generate new private key. Keep it outside the
   repo (e.g., `~/.config/batman-collection-sa.json`); never commit it.
3. **Drive folder public** (Anyone with the link, Viewer) for the duration of the run,
   so the script can fetch each image via the plain Drive URL. Flip it back when done.
4. **Dependencies** (Python 3.10+):
   ```
   pip install firebase-admin openpyxl requests
   ```

### Dry-run (recommended first)

```
python tools/migrate_colecciones.py \
    --xlsx ~/Downloads/Colecciones.xlsx \
    --project-id <your-firebase-project-id> \
    --dry-run
```

Prints the planned mapping, flags skipped rows, and surfaces any id collisions —
touches neither Drive nor Firebase.

### Real run

```
export GOOGLE_APPLICATION_CREDENTIALS=~/.config/batman-collection-sa.json

python tools/migrate_colecciones.py \
    --xlsx ~/Downloads/Colecciones.xlsx \
    --project-id <your-firebase-project-id> \
    [--bucket <your-firebase-project-id>.firebasestorage.app]
```

The default bucket is `<project-id>.appspot.com`; pass `--bucket` if your project uses
the newer `.firebasestorage.app` host.

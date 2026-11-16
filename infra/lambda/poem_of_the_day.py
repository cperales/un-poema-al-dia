"""Pick one poem per day and publish it as today.json.

Reads every ``*.md`` under ``POEMS_PREFIX`` in ``POEMS_BUCKET`` and writes the
chosen poem (full text included) to ``OUT_BUCKET/today.json``. A private
``history.json`` next to it remembers which poems were already used, so none
repeats until all of them have been shown.

The run is idempotent: if ``today.json`` already holds today's date, nothing
changes (pass ``{"force": true}`` as the event to pick a new poem anyway).
Nothing is overwritten unless a new poem was picked successfully, so a failed
run leaves yesterday's poem in place.
"""

import json
import os
import random
from datetime import datetime
from zoneinfo import ZoneInfo

TODAY_KEY = "today.json"
HISTORY_KEY = "history.json"


# ---- pure helpers (no AWS, covered by tests) -------------------------------

def parse_key(key, prefix):
    """Return (author, title) for a poem key, like ``find_poems`` in app.py.

    The title is the filename without ``.md``. The author is every directory
    below ``prefix``; a leading ``Poemas/`` segment is dropped so that
    ``Poemas/Poemas/<Author>/<Title>.md`` gives ``<Author>``.
    """
    rel = key[len(prefix):] if key.startswith(prefix) else key
    parts = [p for p in rel.split("/") if p]
    title = parts[-1]
    if title.lower().endswith(".md"):
        title = title[:-3]
    dirs = parts[:-1]
    if dirs and dirs[0] == "Poemas":
        dirs = dirs[1:]
    return "/".join(dirs), title


def pick(keys, used, rng=random):
    """Choose a key not in ``used``. Returns (key, new_used).

    When every key has been used, the history restarts from the chosen poem.
    """
    if not keys:
        raise ValueError("no poems found")
    used_set = set(used)
    candidates = [k for k in keys if k not in used_set]
    if not candidates:
        used = []
        candidates = list(keys)
    key = rng.choice(sorted(candidates))
    # Keep only keys that still exist, so deleted poems don't pile up.
    existing = set(keys)
    new_used = [k for k in used if k in existing] + [key]
    return key, new_used


def build_payload(date, key, prefix, text):
    author, title = parse_key(key, prefix)
    return {"date": date, "id": key, "title": title, "author": author, "text": text}


# ---- AWS plumbing -----------------------------------------------------------

def _s3():
    import boto3  # imported lazily so the helpers above are testable without it

    return boto3.client("s3")


def _get_json(s3, bucket, key):
    try:
        body = s3.get_object(Bucket=bucket, Key=key)["Body"].read()
    except s3.exceptions.NoSuchKey:
        return None
    return json.loads(body.decode("utf-8"))


def _list_poem_keys(s3, bucket, prefix):
    keys = []
    for page in s3.get_paginator("list_objects_v2").paginate(Bucket=bucket, Prefix=prefix):
        for obj in page.get("Contents", []):
            if obj["Key"].lower().endswith(".md"):
                keys.append(obj["Key"])
    return keys


def _put_json(s3, bucket, key, data, cache_control):
    s3.put_object(
        Bucket=bucket,
        Key=key,
        Body=json.dumps(data, ensure_ascii=False).encode("utf-8"),
        ContentType="application/json; charset=utf-8",
        CacheControl=cache_control,
    )


def handler(event, context):
    poems_bucket = os.environ["POEMS_BUCKET"]
    prefix = os.environ.get("POEMS_PREFIX", "")
    out_bucket = os.environ["OUT_BUCKET"]
    tz = os.environ.get("TZ_NAME", "Europe/Madrid")
    force = bool((event or {}).get("force"))

    s3 = _s3()
    today = datetime.now(ZoneInfo(tz)).date().isoformat()
    keys = _list_poem_keys(s3, poems_bucket, prefix)

    current = _get_json(s3, out_bucket, TODAY_KEY)
    if (
        not force
        and current
        and current.get("date") == today
        and current.get("id") in keys
    ):
        print(f"today.json already set for {today}: {current['id']}")
        return {"date": today, "id": current["id"], "changed": False}

    history = _get_json(s3, out_bucket, HISTORY_KEY) or {}
    key, used = pick(keys, history.get("used", []))

    text = s3.get_object(Bucket=poems_bucket, Key=key)["Body"].read().decode("utf-8")
    payload = build_payload(today, key, prefix, text)

    # today.json first: if the history write fails the poem is still published.
    _put_json(s3, out_bucket, TODAY_KEY, payload, "public, max-age=300")
    _put_json(s3, out_bucket, HISTORY_KEY, {"used": used}, "private, no-store")
    print(f"picked {key} for {today}")
    return {"date": today, "id": key, "changed": True}

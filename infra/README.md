# Poem of the day (AWS)

A Lambda picks one poem per day and publishes it as `today.json`, so the web
server and the mobile app show the same poem.

```
EventBridge Scheduler (00:05 Europe/Madrid) -> Lambda -> <out bucket>/today.json  (public GET)
                                                      -> <out bucket>/history.json (private)
poems bucket (private, *.md under POEMS_PREFIX) ---^
```

## `today.json`

```json
{"date": "2026-11-17", "id": "Poemas/Poemas/Antonio Machado/Anoche cuando dormía.md",
 "title": "Anoche cuando dormía", "author": "Antonio Machado", "text": "..."}
```

- Served with `Cache-Control: public, max-age=300` and CORS `GET` from any origin.
- Only `today.json` is public; the poems bucket and `history.json` are not.
- A poem is not repeated until every poem has been used (`history.json`).
- If a run fails, the previous `today.json` stays. Runs are idempotent: a
  second run on the same day changes nothing.

## Clients

Both clients read `today.json` and fall back to local poems when it is
unreachable:

- **Web server (`app.py`)**: fetched per request with a 60 s cache; override the
  URL with `WEBPOEMA_TODAY_URL`.
- **Android app**: fetched at launch and when a notification fires, cached for
  offline use; the URL comes from `todayJsonUrl` in `gradle.properties`
  (`-PtodayJsonUrl=...` overrides it), and `app/src/main/assets/poems.json` is
  the final fallback.

Deploying to another account/region? Copy the `TodayUrl` output into both
settings.

## Deploy

Locally (profile `personal`, us-east-1; the account is already CDK-bootstrapped):

```bash
cd infra
uv venv && uv pip install -r requirements.txt
export AWS_PROFILE=personal CDK_DEFAULT_REGION=us-east-1
export CDK_DEFAULT_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
cdk diff   -c poemsBucket=<bucket with the .md poems>
cdk deploy -c poemsBucket=<bucket with the .md poems>
```

`poemsPrefix` (default `Poemas/`) is the key prefix of the poems; override it
with `-c poemsPrefix=...`. Account and region come from the environment, so the
same code works with a profile locally and with OIDC credentials in GitHub
Actions. The installed `cdk` CLI must be at least as new as `aws-cdk-lib`
(pinned in `requirements.txt`).

## Operate

```bash
# run now (no-op if today already has a poem); add {"force": true} to re-pick
aws lambda invoke --profile personal --function-name <FunctionName output> \
  --cli-binary-format raw-in-base64-out --payload '{"force": true}' /dev/stdout

# choose a specific poem for today: upload your own today.json
aws s3 cp today.json s3://<out bucket>/today.json --profile personal \
  --content-type 'application/json; charset=utf-8' --cache-control 'public, max-age=300'
```

Tests (no AWS needed): `python -m unittest discover infra/tests`

# AGENTS.md

## Branches

- **`pages`** (default, this branch): the static web page in `web/` (published by
  GitHub Pages) and the AWS stack in `infra/` that picks the poem of the day.
- **`app`**: the Android app only. It reads the same `today.json`. The two
  branches never merge; their only shared contract is `today.json`.

Poems live only in the private S3 poems bucket (all public domain). Nothing in
this repo bundles poem texts; `Poemas/` and `s3-poemas/` are gitignored local copies.

## Web (`web/`)

Static HTML + JS, no build step, no dependencies.

- `web/index.html`: markup and CSS (follows `prefers-color-scheme`).
- `web/app.js`: fetches `TODAY_URL` (the stack's `TodayUrl` output) and renders it
  with `textContent`. Shows an error message if the fetch fails; there is no local fallback.
- Preview locally: `python -m http.server -d web 8000` → `http://localhost:8000`.
- Deploy: `.github/workflows/pages.yml` publishes `web/` on every push to `pages`
  that touches `web/`.

## Infra (`infra/`)

AWS CDK (Python) stack that publishes one shared "poem of the day" as `today.json`
(Lambda + EventBridge Scheduler at 00:05 Europe/Madrid, public-read `today.json`
only). See `infra/README.md`.

- Deploy with the `personal` AWS profile (us-east-1): `cdk deploy -c poemsBucket=<name>` from `infra/`. Deploys will eventually run from GitHub Actions, so keep account/region environment-driven (no hardcoded profile or account).
- Python env is managed with `uv` (`cd infra && uv venv && uv pip install -r requirements.txt`).
- Lambda code is `infra/lambda/poem_of_the_day.py` (boto3 only); tests: `python -m unittest discover infra/tests`.
- `today.json` contract: `{date, id, title, author, text}`. If the `TodayUrl` changes, update `TODAY_URL` in `web/app.js` and `todayJsonUrl` on branch `app`.

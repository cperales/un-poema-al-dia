# AGENTS.md

## Environment

This project uses Python with `uv` for virtual environment management.

```bash
uv venv
source .venv/bin/activate
python app.py
```

## CLI

```bash
./webpoem start      # Start the server
./webpoem stop       # Stop the server
./webpoem restart    # Restart the server
./webpoem status     # Show server status
```

## Project

Simple static poetry website that serves a random poem from `Poemas/` on each page load.

- **Server**: `app.py` — Python built-in HTTP server, no dependencies needed
- **CLI**: `webpoem` — manages the server process (start/stop/restart/status)
- **Poems**: `Poemas/` — add or remove `.md` files to update the collection
- **Run**: `./webpoem start` → open `http://localhost:8000`

## Infra (`infra/`)

AWS CDK (Python) stack that publishes one shared "poem of the day" as `today.json`
(Lambda + EventBridge Scheduler at 00:05 Europe/Madrid, public-read `today.json`
only). See `infra/README.md`.

- Deploy with the `personal` AWS profile (us-east-1): `cdk deploy -c poemsBucket=<name>` from `infra/`. Deploys will eventually run from GitHub Actions, so keep account/region environment-driven (no hardcoded profile or account).
- Lambda code is `infra/lambda/poem_of_the_day.py` (boto3 only); tests: `python -m unittest discover infra/tests`.
- `today.json` contract: `{date, id, title, author, text}`. Clients (`app.py`, Android app) should fetch it and fall back to local poems.

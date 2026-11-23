# Poem of the day (AWS)

A Lambda picks one poem per day and publishes it as `today.json`, so the web
page and the mobile app show the same poem.

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

Both clients read `today.json` and nothing else; neither bundles poems:

- **Web page** (`web/` on branch `pages`, published by GitHub Pages): the URL
  is `TODAY_URL` in `web/app.js`. It shows an error message if the fetch fails.
- **Android app** (branch `app`): fetched at launch and when a notification
  fires, and cached for offline use. The URL comes from `todayJsonUrl` in
  `gradle.properties` (`-PtodayJsonUrl=...` overrides it).

Deploying to another account/region (new `TodayUrl` output)? Update both
settings and publish a new APK.

## Deploy

The CDK app has two stacks:

| Stack | What | Deployed by |
|---|---|---|
| `PoemOfTheDay` | Lambda, scheduler, out bucket | GitHub Actions on every push to `pages` touching `infra/` |
| `GitHubDeploy` | OIDC provider + `DeployRole` for GitHub Actions | by hand, only when it changes |

### From GitHub Actions (normal path)

`.github/workflows/deploy-infra.yml` runs the unit tests and a credential-less
`cdk synth` on pushes and PRs touching `infra/`, then (only on `pages`) assumes
`DeployRole` via OIDC and runs `cdk deploy PoemOfTheDay`. It can also be started by
hand from the Actions tab (`workflow_dispatch`). No AWS keys are stored in GitHub;
it needs these repository **variables**: `AWS_DEPLOY_ROLE_ARN` (the
`GitHubDeploy.DeployRoleArn` output), `AWS_REGION` (`eu-west-3`) and `POEMS_BUCKET`.

`DeployRole` only trusts OIDC tokens whose `sub` is
`<githubSubjectPrefix>:ref:refs/heads/<githubBranch>` (context in `cdk.json`) and can
only assume the CDK bootstrap roles of this account/region. This repo uses GitHub's
immutable subject format, so the prefix includes the owner and repo IDs
(`repo:cperales@17336458/un-poema-al-dia@1460477649`, from
`gh api repos/{owner}/{repo}/actions/oidc/customization/sub`); a renamed or re-created
repo will not match. The
bootstrap execution role has `AdministratorAccess` (CDK default), so anything merged
into `pages` can change the account through CloudFormation: protect that branch.

### Locally

Profile `personal`, eu-west-3 Paris (the account is already CDK-bootstrapped there):

```bash
cd infra
uv venv && uv pip install -r requirements.txt
export AWS_PROFILE=personal AWS_REGION=eu-west-3
cdk diff   --all -c poemsBucket=webpoema-140874247434-eu-west-3
cdk deploy GitHubDeploy -c poemsBucket=webpoema-140874247434-eu-west-3   # OIDC role
cdk deploy PoemOfTheDay -c poemsBucket=webpoema-140874247434-eu-west-3   # usually CI's job
```

Set the region with **`AWS_REGION`**, not only `CDK_DEFAULT_REGION`: the `cdk` CLI
overwrites `CDK_DEFAULT_REGION` (and `CDK_DEFAULT_ACCOUNT`) with the profile's own
region before running `app.py`, so a stale `CDK_DEFAULT_REGION` silently targets the
profile's default region. Always check that `cdk diff` shows the region you expect.

Current deployment: poems bucket `webpoema-140874247434-eu-west-3` (private),
`TodayUrl` = `https://poemoftheday-outb8d7ccdd-tcmrg2fvcp9b.s3.eu-west-3.amazonaws.com/today.json`.

`poemsPrefix` (set to `Poemas/` in `cdk.json`) is the key prefix of the poems;
override it with `-c poemsPrefix=...`. Account and region come from the environment, so the
same code works with a profile locally and with OIDC credentials in GitHub
Actions. The installed `cdk` CLI must be at least as new as `aws-cdk-lib`
(pinned in `requirements.txt`).

## Operate

```bash
# run now (no-op if today already has a poem); add {"force": true} to re-pick
aws lambda invoke --profile personal --region eu-west-3 --function-name <FunctionName output> \
  --cli-binary-format raw-in-base64-out --payload '{"force": true}' /dev/stdout

# choose a specific poem for today: upload your own today.json
aws s3 cp today.json s3://<out bucket>/today.json --profile personal --region eu-west-3 \
  --content-type 'application/json; charset=utf-8' --cache-control 'public, max-age=300'
```

Tests (no AWS needed): `python -m unittest discover infra/tests`

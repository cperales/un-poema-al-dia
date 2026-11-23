#!/usr/bin/env python3
"""CDK app with two stacks:

- PoemOfTheDay: daily Lambda that publishes today.json (deployed by CI).
- GitHubDeploy: OIDC provider + role that lets GitHub Actions deploy
  PoemOfTheDay (deployed by hand, once).

    cdk deploy PoemOfTheDay -c poemsBucket=<bucket holding the .md poems>

Account/region come from the standard AWS environment (profile locally,
credentials from the OIDC role in CI), nothing is hardcoded.
"""

import os

import aws_cdk as cdk

from github_stack import GitHubDeployStack
from stack import PoemOfTheDayStack

app = cdk.App()

poems_bucket = app.node.try_get_context("poemsBucket")
if not poems_bucket:
    raise SystemExit("Pass the poems bucket: cdk <cmd> -c poemsBucket=<name>")

env = cdk.Environment(
    account=os.environ.get("CDK_DEFAULT_ACCOUNT"),
    region=os.environ.get("CDK_DEFAULT_REGION"),
)

PoemOfTheDayStack(
    app,
    "PoemOfTheDay",
    poems_bucket_name=poems_bucket,
    poems_prefix=app.node.try_get_context("poemsPrefix") or "",
    env=env,
)

GitHubDeployStack(
    app,
    "GitHubDeploy",
    repo=app.node.try_get_context("githubRepo"),
    branch=app.node.try_get_context("githubBranch") or "pages",
    subject_prefix=app.node.try_get_context("githubSubjectPrefix"),
    env=env,
)

app.synth()

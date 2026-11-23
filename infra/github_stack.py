from aws_cdk import CfnOutput, Duration, Stack, aws_iam as iam
from constructs import Construct

GITHUB_OIDC_URL = "https://token.actions.githubusercontent.com"


class GitHubDeployStack(Stack):
    """OIDC trust so GitHub Actions can run `cdk deploy` without stored AWS keys.

    Deployed by hand (profile `personal`), never from CI:

        cdk deploy GitHubDeploy -c poemsBucket=... -c githubRepo=owner/repo

    The role can only assume this account's CDK bootstrap roles in this stack's
    region, and only workflows running on `refs/heads/<branch>` of `repo` can
    assume it (matched on the OIDC `sub` claim, see `subject_prefix`). Note that the bootstrap execution role is AdministratorAccess (CDK
    default), so the branch restriction is the real security boundary.
    """

    def __init__(self, scope: Construct, construct_id: str, *, repo: str, branch: str,
                 subject_prefix: str | None = None, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # The token's `sub` starts with this prefix. Repos using GitHub's immutable
        # subject format get "repo:<owner>@<owner_id>/<repo>@<repo_id>" instead of
        # "repo:<owner>/<repo>" (see GET /repos/{repo}/actions/oidc/customization/sub,
        # field sub_claim_prefix).
        subject_prefix = subject_prefix or f"repo:{repo}"

        # One provider per account and URL; native CloudFormation resource (no
        # custom-resource Lambda, no thumbprints needed).
        provider = iam.OidcProviderNative(
            self,
            "GitHubOidc",
            url=GITHUB_OIDC_URL,
            client_ids=["sts.amazonaws.com"],
        )

        role = iam.Role(
            self,
            "DeployRole",
            description=f"GitHub Actions cdk deploy from {repo}@{branch}",
            max_session_duration=Duration.hours(1),
            assumed_by=iam.WebIdentityPrincipal(
                provider.oidc_provider_arn,
                conditions={
                    "StringEquals": {
                        "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
                        "token.actions.githubusercontent.com:sub":
                            f"{subject_prefix}:ref:refs/heads/{branch}",
                    },
                },
            ),
        )

        # Least privilege on our side: everything else happens through the CDK
        # bootstrap roles (deploy, file-publishing, lookup, image-publishing).
        role.add_to_policy(
            iam.PolicyStatement(
                actions=["sts:AssumeRole", "sts:TagSession"],
                resources=[
                    f"arn:{self.partition}:iam::{self.account}:role/"
                    f"cdk-hnb659fds-*-{self.account}-{self.region}"
                ],
            )
        )

        CfnOutput(self, "DeployRoleArn", value=role.role_arn)

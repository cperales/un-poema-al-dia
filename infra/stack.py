from aws_cdk import (
    CfnOutput,
    Duration,
    RemovalPolicy,
    Stack,
    aws_iam as iam,
    aws_lambda as _lambda,
    aws_s3 as s3,
    aws_scheduler as scheduler,
)
from constructs import Construct

TIMEZONE = "Europe/Madrid"
TODAY_KEY = "today.json"


class PoemOfTheDayStack(Stack):
    def __init__(self, scope: Construct, construct_id: str, *, poems_bucket_name: str,
                 poems_prefix: str, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # Existing, private bucket with the .md poems (not modified).
        poems_bucket = s3.Bucket.from_bucket_name(self, "Poems", poems_bucket_name)

        # New bucket: the only public thing. Its policy exposes today.json only.
        out_bucket = s3.Bucket(
            self,
            "Out",
            encryption=s3.BucketEncryption.S3_MANAGED,
            block_public_access=s3.BlockPublicAccess(
                block_public_acls=True,
                ignore_public_acls=True,
                block_public_policy=False,
                restrict_public_buckets=False,
            ),
            cors=[
                s3.CorsRule(
                    allowed_methods=[s3.HttpMethods.GET, s3.HttpMethods.HEAD],
                    allowed_origins=["*"],
                    allowed_headers=["*"],
                    max_age=3600,
                )
            ],
            removal_policy=RemovalPolicy.RETAIN,
        )
        out_bucket.add_to_resource_policy(
            iam.PolicyStatement(
                actions=["s3:GetObject"],
                principals=[iam.AnyPrincipal()],
                resources=[out_bucket.arn_for_objects(TODAY_KEY)],
            )
        )

        fn = _lambda.Function(
            self,
            "Picker",
            runtime=_lambda.Runtime.PYTHON_3_12,
            handler="poem_of_the_day.handler",
            # Exclude bytecode: running the tests writes lambda/__pycache__, which
            # would otherwise change the asset hash (and redeploy) for no reason.
            code=_lambda.Code.from_asset("lambda", exclude=["__pycache__", "*.pyc"]),
            timeout=Duration.seconds(30),
            memory_size=256,
            environment={
                "POEMS_BUCKET": poems_bucket_name,
                "POEMS_PREFIX": poems_prefix,
                "OUT_BUCKET": out_bucket.bucket_name,
                "TZ_NAME": TIMEZONE,
            },
        )
        poems_bucket.grant_read(fn)
        out_bucket.grant_read_write(fn)

        role = iam.Role(
            self,
            "SchedulerRole",
            assumed_by=iam.ServicePrincipal("scheduler.amazonaws.com"),
        )
        fn.grant_invoke(role)

        scheduler.CfnSchedule(
            self,
            "Daily",
            description="Pick the poem of the day just after midnight (Madrid)",
            schedule_expression="cron(5 0 * * ? *)",
            schedule_expression_timezone=TIMEZONE,
            flexible_time_window=scheduler.CfnSchedule.FlexibleTimeWindowProperty(mode="OFF"),
            target=scheduler.CfnSchedule.TargetProperty(
                arn=fn.function_arn,
                role_arn=role.role_arn,
                retry_policy=scheduler.CfnSchedule.RetryPolicyProperty(
                    maximum_retry_attempts=2
                ),
            ),
        )

        CfnOutput(
            self,
            "TodayUrl",
            value=f"https://{out_bucket.bucket_name}.s3.{self.region}.amazonaws.com/{TODAY_KEY}",
        )
        CfnOutput(self, "FunctionName", value=fn.function_name)

package com.sdehandbook.springBootEcsFargateCdk;

import com.google.common.base.Preconditions;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

public class SpringBootEcsFargateCdkApp {
    public static void main(final String[] args) {
        final App app = new App();

        final String awsAccountNumber = Preconditions.checkNotNull(
                System.getenv("aws_account"),
                "This stack requires the environment variable aws_account to bet set. It should contain the aws account number of the account you wish to deploy to.");
        final String awsRegion = Preconditions.checkNotNull(
                System.getenv("aws_region"),
                "This stack requires the environment variable aws_region to be set. It should contain the region you wish to deploy to.");

        final Environment environment = Environment.builder()
                .account(awsAccountNumber)
                .region(awsRegion)
                .build();

        new SpringBootEcsFargateCdkStack(app, "SpringBootEcsFargateCdkStack", StackProps.builder()
                .env(environment)
                .build());

        app.synth();
    }
}

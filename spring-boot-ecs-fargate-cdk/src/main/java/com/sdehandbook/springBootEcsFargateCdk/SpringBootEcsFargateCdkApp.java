package com.sdehandbook.springBootEcsFargateCdk;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

public class SpringBootEcsFargateCdkApp {
    public static void main(final String[] args) {
        final App app = new App();

        final Environment environment = Environment.builder()
                .account("638779372364")
                .region("us-west-2")
                .build();

        new SpringBootEcsFargateCdkStack(app, "SpringBootEcsFargateCdkStack", StackProps.builder()
                .env(environment)
                .build());

        app.synth();
    }
}

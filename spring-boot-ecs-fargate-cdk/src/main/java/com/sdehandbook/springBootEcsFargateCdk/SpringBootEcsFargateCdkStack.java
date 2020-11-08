package com.sdehandbook.springBootEcsFargateCdk;

import com.google.common.collect.Lists;
import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.CfnOutputProps;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinition;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.TargetType;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.RoleProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;

public class SpringBootEcsFargateCdkStack extends Stack {
    public SpringBootEcsFargateCdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        final Vpc vpc = Vpc.Builder.create(this, "SpringBootEcsFargateCdk")
                .subnetConfiguration(Vpc.DEFAULT_SUBNETS_NO_NAT)
                .build();

        final Cluster cluster = Cluster.Builder.create(this, "SpringBootEcsFargateCluster")
                .vpc(vpc)
                .build();

        final DockerImageAsset dockerImageAsset = DockerImageAsset.Builder.create(this, "SpringBootEcsFargateDockerImageAsset")
                .directory("../spring-boot-ecs-fargate-web")
                .build();

        final Role executionRole = new Role(this, "SpringBootEcsFargateExecutionRole", RoleProps.builder()
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .managedPolicies(Lists.newArrayList(
                        ManagedPolicy.fromManagedPolicyArn(
                                this,
                                "AmazonECSTaskExecutionRolePolicy",
                                "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy")))
                .build());

        final Role taskRole = new Role(this, "SpringBootEcsFargateTaskExecutionRole", RoleProps.builder()
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .managedPolicies(Lists.newArrayList(
                        ManagedPolicy.fromAwsManagedPolicyName("CloudWatchFullAccess")))
                .build());

        final FargateTaskDefinition fargateTaskDefinition = FargateTaskDefinition.Builder.create(this, "SpringBootEcsFargateTaskDefinition")
                .cpu(256)
                .memoryLimitMiB(512)
                .executionRole(executionRole)
                .taskRole(taskRole)
                .build();

        final ContainerDefinition containerDefinition = ContainerDefinition.Builder.create(this, "SpringBootEcsFargateContainerDefinition")
                .image(ContainerImage.fromDockerImageAsset(dockerImageAsset))
                .taskDefinition(fargateTaskDefinition)
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .streamPrefix("springbootecsfargate")
                        .build()))
                .build();
        containerDefinition.addPortMappings(PortMapping.builder()
                .containerPort(8080)
                .build());

        final ApplicationLoadBalancer applicationLoadBalancer = ApplicationLoadBalancer.Builder.create(this, "SpringBootEcsFargateApplicationLoadBalancer")
                .vpc(vpc)
                .internetFacing(true)
                .vpcSubnets(SubnetSelection.builder()
                        .onePerAz(true)
                        .subnetType(SubnetType.PUBLIC)
                        .build())
                .build();

        final ApplicationTargetGroup applicationTargetGroup = ApplicationTargetGroup.Builder.create(this, "SpringBootEcsFargateApplicationTargetGroup")
                .port(8080)
                .protocol(ApplicationProtocol.HTTP)
                .targetType(TargetType.IP)
                .vpc(vpc)
                .build();

        ApplicationListener.Builder.create(this, "SpringBootEcsFargateApplicationSecureListener")
                .loadBalancer(applicationLoadBalancer)
                .defaultTargetGroups(Lists.newArrayList(applicationTargetGroup))
                .protocol(ApplicationProtocol.HTTP)
                .build();

        final SecurityGroup securityGroup = SecurityGroup.Builder.create(this, "SpringBootEcsFargateServiceSecurityGroup")
                .vpc(vpc)
                .build();
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(8080));

        final FargateService fargateService = FargateService.Builder.create(this, "SpringBootEcsFargateService")
                .cluster(cluster)
                .taskDefinition(fargateTaskDefinition)
                .assignPublicIp(true)
                .desiredCount(1)
                .healthCheckGracePeriod(Duration.seconds(0))
                .vpcSubnets(SubnetSelection.builder()
                        .onePerAz(true)
                        .subnetType(SubnetType.PUBLIC)
                        .build())
                .securityGroups(Lists.newArrayList(securityGroup))
                .build();
        fargateService.attachToApplicationTargetGroup(applicationTargetGroup);

        new CfnOutput(this, "AlbUrl", CfnOutputProps.builder()
                .value(applicationLoadBalancer.getLoadBalancerDnsName())
                .build());
    }
}

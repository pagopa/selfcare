package it.pagopa.selfcare.user.conf;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.Data;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@ApplicationScoped
@Data
public class AwsSesConfig {


    @ConfigProperty(name = "user-ms.aws.ses.secret-id")
    String secretId;

    @ConfigProperty(name = "user-ms.aws.ses.secret-key")
    String secretKey;

    @ConfigProperty(name = "user-ms.aws.ses.region")
    String region;

    @ApplicationScoped
    public SesClient sesClient(AwsSesConfig awsSesConfig) {

        StaticCredentialsProvider staticCredentials = StaticCredentialsProvider
                .create(AwsBasicCredentials.create(awsSesConfig.getSecretId(), awsSesConfig.getSecretKey()));

        return SesClient.builder()
                .region(Region.of(awsSesConfig.getRegion()))
                .credentialsProvider(staticCredentials)
                .build();
    }
}

package konkuk.clog.domain.llm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

/**
 * AWS Lambda SDK 클라이언트 — EC2 IAM Role 또는 환경변수 자격 증명 사용.
 */
@Configuration
public class AwsLambdaConfig {

    @Bean
    public LambdaClient lambdaClient(@Value("${aws.lambda.region}") String region) {
        return LambdaClient.builder()
                .region(Region.of(region))
                .build();
    }
}

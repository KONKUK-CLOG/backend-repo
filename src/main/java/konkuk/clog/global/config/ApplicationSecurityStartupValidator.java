package konkuk.clog.global.config;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 운영(prod) 프로파일에서 필수 시크릿·CORS·외부 연결 설정이 비어 있으면 기동을 막는다.
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ApplicationSecurityStartupValidator implements ApplicationRunner {

    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        require("jwt.secret", "JWT_SECRET");
        require("spring.datasource.password", "DB_PASSWORD");
        require("spring.data.mongodb.uri", "MONGODB_URI");
        require("app.blog.public-base-url", "APP_BLOG_PUBLIC_BASE_URL");
        require("app.security.cors-allowed-origins", "APP_CORS_ALLOWED_ORIGINS");
        require("aws.lambda.function-name", "AWS_LAMBDA_FUNCTION_NAME");
        require("app.crypto.secret", "APP_CRYPTO_SECRET");
        require("github.oauth.client-id", "GITHUB_CLIENT_ID");
        require("github.oauth.client-secret", "GITHUB_CLIENT_SECRET");
        require("github.oauth.redirect-uri", "GITHUB_REDIRECT_URI");

        String jwt = environment.getProperty("jwt.secret", "");
        if (jwt.length() < 32) {
            throw new IllegalStateException(
                    "[prod] jwt.secret(JWT_SECRET) 는 최소 32자 이상이어야 합니다.");
        }
        String crypto = environment.getProperty("app.crypto.secret", "");
        if (crypto.length() < 32) {
            throw new IllegalStateException(
                    "[prod] app.crypto.secret(APP_CRYPTO_SECRET) 는 최소 32자 이상이어야 합니다.");
        }
    }

    private void require(String property, String envName) {
        String v = environment.getProperty(property);
        if (!StringUtils.hasText(v)) {
            throw new IllegalStateException(
                    "[prod] 설정 누락: " + property + " (환경 변수 " + envName + " 로 주입하세요). 활성 프로파일: "
                            + Arrays.toString(environment.getActiveProfiles()));
        }
    }
}

package konkuk.clog.domain.user.github;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GitHub OAuth 앱 설정(client id/secret, 토큰·사용자 API URI).
 * <p>application.properties 의 {@code github.oauth.*} 에 바인딩된다.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "github.oauth")
public class GithubOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String tokenUri;
    private String userUri;
    private String userEmailsUri;
}

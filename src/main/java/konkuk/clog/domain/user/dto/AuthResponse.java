package konkuk.clog.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * OAuth 완료 후 클라이언트에 내려주는 JWT 응답.
 */
@Getter
@AllArgsConstructor
public class AuthResponse {

    private final String accessToken;
    private final String tokenType;
}

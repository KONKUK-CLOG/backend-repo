package konkuk.clog.domain.user.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import konkuk.clog.domain.user.dto.GithubUserInfo;
import konkuk.clog.domain.user.github.GithubOAuthProperties;
import konkuk.clog.global.exception.BusinessException;
import konkuk.clog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * GitHub OAuth authorization code 를 액세스 토큰으로 교환하고, 사용자 프로필을 조회한다.
 */
@Service
@RequiredArgsConstructor
public class GithubOAuthService {

    private final GithubOAuthProperties properties;
    private final RestClient restClient = RestClient.create();

    /**
     * @param code GitHub 가 콜백으로 넘긴 authorization code
     * @return GitHub 사용자 정보(이메일은 공개 프로필 또는 /user/emails 보조 조회)
     */
    public GithubUserInfo fetchUserFromAuthorizationCode(String code) {
        String accessToken = exchangeCodeForAccessToken(code);
        GithubUserInfo user = fetchUser(accessToken);
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            String email = fetchPrimaryEmail(accessToken);
            if (email == null || email.isBlank()) {
                throw new BusinessException(ErrorCode.SOCIAL_EMAIL_REQUIRED);
            }
            user.setEmail(email);
        }
        return user;
    }

    private String exchangeCodeForAccessToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("code", code);
        form.add("redirect_uri", properties.getRedirectUri());

        String body = restClient.post()
                .uri(properties.getTokenUri())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);

        if (body == null || body.isBlank()) {
            throw new BusinessException(ErrorCode.GITHUB_AUTH_FAILED);
        }
        // GitHub 는 x-www-form-urlencoded 또는 JSON — 보통 access_token=...&scope=... 형태
        String token = parseAccessTokenFromBody(body);
        if (token == null) {
            throw new BusinessException(ErrorCode.GITHUB_AUTH_FAILED);
        }
        return token;
    }

    private String parseAccessTokenFromBody(String body) {
        if (body.trim().startsWith("{")) {
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> map = mapper.readValue(body, Map.class);
                if (map.containsKey("error") || map.containsKey("error_description")) {
                    throw new BusinessException(ErrorCode.GITHUB_AUTH_FAILED);
                }
                Object at = map.get("access_token");
                return at != null ? at.toString() : null;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                return null;
            }
        }
        return Arrays.stream(body.split("&"))
                .map(s -> s.split("=", 2))
                .filter(a -> a.length == 2 && "access_token".equals(a[0]))
                .map(a -> java.net.URLDecoder.decode(a[1], StandardCharsets.UTF_8))
                .findFirst()
                .orElse(null);
    }

    private GithubUserInfo fetchUser(String accessToken) {
        GithubUserInfo user = restClient.get()
                .uri(properties.getUserUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .retrieve()
                .body(GithubUserInfo.class);
        if (user == null || user.getId() == null) {
            throw new BusinessException(ErrorCode.GITHUB_AUTH_FAILED);
        }
        return user;
    }

    private String fetchPrimaryEmail(String accessToken) {
        var list = restClient.get()
                .uri(properties.getUserEmailsUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .retrieve()
                .body(new ParameterizedTypeReference<java.util.List<Map<String, Object>>>() {
                });
        if (list == null) {
            return null;
        }
        for (Map<String, Object> row : list) {
            Object primary = row.get("primary");
            Boolean verified = (Boolean) row.get("verified");
            if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                Object email = row.get("email");
                return email != null ? email.toString() : null;
            }
        }
        return list.stream()
                .filter(m -> Boolean.TRUE.equals(m.get("verified")))
                .map(m -> m.get("email"))
                .map(Object::toString)
                .findFirst()
                .orElse(null);
    }
}

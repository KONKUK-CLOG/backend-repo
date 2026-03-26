package konkuk.clog.domain.user.service;

import java.util.UUID;
import konkuk.clog.domain.user.domain.User;
import konkuk.clog.domain.user.dto.AuthResponse;
import konkuk.clog.domain.user.dto.GithubUserInfo;
import konkuk.clog.domain.user.repository.UserRepository;
import konkuk.clog.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth 콜백 이후 사용자 upsert 및 JWT 발급.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final GithubOAuthService githubOAuthService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * GitHub authorization code 로 로그인(또는 가입) 후 액세스 토큰을 발급한다.
     */
    @Transactional
    public AuthResponse authenticateWithGithubCode(String code) {
        GithubUserInfo gh = githubOAuthService.fetchUserFromAuthorizationCode(code);
        String socialId = "github-" + gh.getId();
        User user = userRepository.findBySocialId(socialId)
                .orElseGet(() -> registerGithubUser(gh, socialId));
        String jwt = jwtTokenProvider.generateToken(user);
        return new AuthResponse(jwt, "Bearer");
    }

    private User registerGithubUser(GithubUserInfo gh, String socialId) {
        String baseNickname = sanitizeNickname(gh.getLogin());
        String nickname = resolveUniqueNickname(baseNickname);
        String name = gh.getName() != null && !gh.getName().isBlank() ? gh.getName() : gh.getLogin();
        String randomPassword = UUID.randomUUID().toString();
        User user = User.builder()
                .name(name)
                .nickname(nickname)
                .email(gh.getEmail())
                .socialId(socialId)
                .passwordHash(passwordEncoder.encode(randomPassword))
                .build();
        return userRepository.save(user);
    }

    private String sanitizeNickname(String login) {
        if (login == null || login.isBlank()) {
            return "github-user";
        }
        return login.length() > 30 ? login.substring(0, 30) : login;
    }

    private String resolveUniqueNickname(String base) {
        if (userRepository.findByNickname(base).isEmpty()) {
            return base;
        }
        for (int i = 1; i < 1000; i++) {
            String candidate = base + "-" + i;
            if (userRepository.findByNickname(candidate).isEmpty()) {
                return candidate;
            }
        }
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

package konkuk.clog.domain.user.controller;

import konkuk.clog.domain.user.dto.AuthResponse;
import konkuk.clog.domain.user.service.AuthService;
import konkuk.clog.global.dto.ApiResponse;
import konkuk.clog.global.exception.BusinessException;
import konkuk.clog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GitHub OAuth 콜백 — code 를 서버가 교환하고 JWT 를 JSON 으로 반환한다.
 * <p>CSRF 완화를 위해 GitHub 권장 {@code state} 는 프론트·백이 세션 없이 맞추기 어려우므로,
 * 프로덕션에서는 HTTPS, GitHub 앱에 등록한 redirect URI 와의 정확한 일치,
 * 그리고 짧은 authorization code 수명에 의존한다.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * GitHub 가 리다이렉트하는 콜백 URL. 쿼리 {@code code} 로 액세스 토큰을 발급한다.
     */
    @GetMapping("/github/callback")
    public ApiResponse<AuthResponse> githubCallback(@RequestParam String code) {
        if (code.length() < 10 || code.length() > 512) {
            throw new BusinessException(ErrorCode.GITHUB_AUTH_FAILED);
        }
        return ApiResponse.success(authService.authenticateWithGithubCode(code));
    }
}

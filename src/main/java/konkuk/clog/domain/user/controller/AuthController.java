package konkuk.clog.domain.user.controller;

import konkuk.clog.domain.user.dto.AuthResponse;
import konkuk.clog.domain.user.service.AuthService;
import konkuk.clog.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GitHub OAuth 콜백 — code 를 서버가 교환하고 JWT 를 JSON 으로 반환한다.
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
        return ApiResponse.success(authService.authenticateWithGithubCode(code));
    }
}

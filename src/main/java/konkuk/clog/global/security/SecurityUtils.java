package konkuk.clog.global.security;

import java.util.Optional;
import konkuk.clog.domain.user.domain.User;
import konkuk.clog.global.exception.BusinessException;
import konkuk.clog.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * {@link SecurityContextHolder} 에서 현재 인증된 {@link User} 를 꺼내는 유틸.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** JWT 필수 API — 인증되지 않았거나 User 가 아니면 {@link BusinessException}(UNAUTHORIZED). */
    public static User requireCurrentUser() {
        return tryGetCurrentUser()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    public static Long requireCurrentUserId() {
        return requireCurrentUser().getId();
    }

    /** 게스트 댓글 등 — 로그인 없을 수 있음. */
    public static Optional<Long> tryGetCurrentUserId() {
        return tryGetCurrentUser().map(User::getId);
    }

    public static Optional<User> tryGetCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        Object p = auth.getPrincipal();
        if (p instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /** 경로의 userId 가 로그인 사용자와 일치하는지 검사한다. */
    public static void assertSelf(Long pathUserId) {
        if (!requireCurrentUserId().equals(pathUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_OPERATION);
        }
    }
}

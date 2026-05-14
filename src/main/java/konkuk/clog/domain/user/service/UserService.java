package konkuk.clog.domain.user.service;

import konkuk.clog.domain.user.domain.User;
import konkuk.clog.domain.user.dto.GithubTokenUpdateRequest;
import konkuk.clog.domain.user.dto.UserResponse;
import konkuk.clog.domain.user.repository.UserRepository;
import konkuk.clog.global.exception.BusinessException;
import konkuk.clog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 본인 프로필·GitHub 토큰·탈퇴 — 가입은 {@link konkuk.clog.domain.user.service.AuthService}(GitHub OAuth) 전용.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void updateGithubToken(Long userId, GithubTokenUpdateRequest request) {
        User user = getUser(userId);
        user.updateGithubToken(request.getEncryptedToken(), request.getExpiresAt());
    }

    @Transactional
    public void revokeGithubToken(Long userId) {
        User user = getUser(userId);
        user.updateGithubToken(null, null);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = getUser(userId);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public UserResponse findUser(Long userId) {
        return UserResponse.from(getUser(userId));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}

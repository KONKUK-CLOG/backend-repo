package konkuk.clog.domain.user.service;

import konkuk.clog.domain.user.domain.User;
import konkuk.clog.domain.user.dto.GithubTokenUpdateRequest;
import konkuk.clog.domain.user.dto.UserCreateRequest;
import konkuk.clog.domain.user.dto.UserResponse;
import konkuk.clog.domain.user.repository.UserRepository;
import konkuk.clog.global.exception.BusinessException;
import konkuk.clog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse registerUser(UserCreateRequest request) {
        userRepository.findByEmail(request.getEmail())
                .ifPresent(user -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
                });

        userRepository.findByNickname(request.getNickname())
                .ifPresent(user -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
                });

        userRepository.findBySocialId(request.getSocialId())
                .ifPresent(user -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_SOCIAL_ID);
                });

        User user = User.builder()
                .name(request.getName())
                .nickname(request.getNickname())
                .email(request.getEmail())
                .socialId(request.getSocialId())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        return UserResponse.from(userRepository.save(user));
    }

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


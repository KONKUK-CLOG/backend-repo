package konkuk.clog.domain.user.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GithubTokenUpdateRequest {

    @NotBlank(message = "암호화된 토큰은 필수입니다.")
    private String encryptedToken;

    @FutureOrPresent(message = "만료 시간은 현재 이후여야 합니다.")
    private LocalDateTime expiresAt;
}


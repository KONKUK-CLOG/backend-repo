package konkuk.clog.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GitHub {@code GET /user} 응답을 역직렬화하기 위한 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class GithubUserInfo {

    private Long id;

    private String login;

    private String name;

    private String email;

    @JsonProperty("avatar_url")
    private String avatarUrl;
}

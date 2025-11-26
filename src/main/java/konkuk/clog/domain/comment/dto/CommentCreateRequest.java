package konkuk.clog.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import konkuk.clog.domain.comment.domain.AuthorType;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Getter
@NoArgsConstructor
public class CommentCreateRequest {

    @NotNull(message = "블로그 ID는 필수입니다.")
    private Long blogId;

    @NotBlank(message = "댓글 내용은 필수입니다.")
    private String content;

    @NotNull(message = "작성자 타입은 필수입니다.")
    private AuthorType authorType;

    @Size(max = 30, message = "게스트 닉네임은 30자를 넘을 수 없습니다.")
    private String guestNickname;

    public boolean isGuestComment() {
        return AuthorType.GUEST.equals(authorType);
    }
}




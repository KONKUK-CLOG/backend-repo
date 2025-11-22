package konkuk.clog.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentCreateRequest {

    @NotNull(message = "블로그 ID는 필수입니다.")
    private Long blogId;

    @NotBlank(message = "댓글 내용은 필수입니다.")
    private String content;
}



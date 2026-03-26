package konkuk.clog.domain.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import konkuk.clog.domain.blog.domain.BlogVisibility;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Extension 에서 수정한 마크다운을 즉시 PUBLISHED 로 저장할 때 사용 — 취소 시 서버 호출 없음.
 */
@Getter
@Setter
@NoArgsConstructor
public class BlogPublishRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotNull
    private BlogVisibility visibility;

    private String codeDiff;
    private String codeContext;
    private String prompt;
    private String chatSessionId;
}

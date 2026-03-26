package konkuk.clog.domain.llm.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import konkuk.clog.domain.chat.domain.CodeSnippet;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code POST /api/blogs/generate} 요청 — 채팅 전송과 동일한 페이로드(플랜 호환).
 */
@Getter
@Setter
@NoArgsConstructor
public class BlogGenerateRequest {

    /** 클라이언트가 알고 있는 세션 id(없으면 서버가 활성 세션 사용). */
    private String chatSessionId;

    @NotBlank
    private String message;

    private List<CodeSnippet> codeSnippets;
}

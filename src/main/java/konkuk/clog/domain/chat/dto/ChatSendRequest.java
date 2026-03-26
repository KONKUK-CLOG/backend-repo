package konkuk.clog.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import konkuk.clog.domain.chat.domain.CodeSnippet;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Extension 채팅 전송 — 본문 메시지와 선택적 코드 스니펫, 기존 세션 id.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatSendRequest {

    /** 없으면 서버가 활성 세션을 찾거나 새로 만든다. */
    private String chatSessionId;

    @NotBlank
    private String message;

    private List<CodeSnippet> codeSnippets;
}

package konkuk.clog.domain.chat.document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import konkuk.clog.domain.chat.domain.ChatRole;
import konkuk.clog.domain.chat.domain.CodeSnippet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 개별 채팅 메시지 — USER/ASSISTANT/SYSTEM 역할과 선택적 코드 스니펫, AI 응답 메타를 보관.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "chat_messages")
public class ChatMessage {

    @Id
    private String id;

    private String sessionId;

    private ChatRole role;

    private String content;

    @Builder.Default
    private List<CodeSnippet> codeSnippets = new ArrayList<>();

    /** AI 가 반환한 사고 과정(스트리밍과 동일 텍스트 저장). */
    private String reasoning;

    /** AI 가 반환한 마크다운 본문. */
    private String markdown;

    private Long blogId;

    private int estimatedTokens;

    private Instant createdAt;
}

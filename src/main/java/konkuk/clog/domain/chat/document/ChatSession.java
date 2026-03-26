package konkuk.clog.domain.chat.document;

import java.time.Instant;
import konkuk.clog.domain.chat.domain.ChatSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB 채팅 세션 — MySQL {@code users.id} 를 {@code userId} 로 참조한다.
 * <p>60K 토큰 초과 시 요약문을 {@code systemMessage} 로 가진 새 세션으로 전환하고 이전 세션은 ARCHIVED.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "chat_sessions")
public class ChatSession {

    @Id
    private String id;

    private Long userId;

    private ChatSessionStatus status;

    /** 요약으로 압축된 이전 대화 맥락(첫 세션은 null). */
    private String systemMessage;

    /** 세션 누적 토큰 추정치(대략적). */
    private int totalTokenCount;

    private Instant createdAt;

    private Instant updatedAt;
}

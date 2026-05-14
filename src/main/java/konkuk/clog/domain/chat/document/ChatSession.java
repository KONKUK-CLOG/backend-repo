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
 * <p>컨텍스트 토큰 한도 초과 시 요약 후 이전 세션·메시지는 삭제하고 {@code systemMessage} 를 담은 새 세션으로 전환한다.</p>
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

    /** 같은 유저 내 프로젝트 단위 활성 세션 구분(null 이면 프로젝트 미연동·레거시). */
    private String projectId;

    private ChatSessionStatus status;

    /** 요약으로 압축된 이전 대화 맥락(첫 세션은 null). */
    private String systemMessage;

    /** 세션 누적 토큰 추정치(대략적). */
    private int totalTokenCount;

    private Instant createdAt;

    private Instant updatedAt;
}

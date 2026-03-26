package konkuk.clog.domain.chat.dto;

import java.util.List;
import java.util.stream.Collectors;
import konkuk.clog.domain.chat.document.ChatMessage;
import lombok.Builder;
import lombok.Getter;

/** 첫 로그인·새로고침 시 활성 세션 전체 메시지 로드. */
@Getter
@Builder
public class ChatHistoryResponse {

    private String sessionId;
    private List<ChatMessageView> messages;

    public static ChatHistoryResponse of(String sessionId, List<ChatMessage> messages) {
        return ChatHistoryResponse.builder()
                .sessionId(sessionId)
                .messages(messages.stream()
                        .map(ChatMessageView::from)
                        .collect(Collectors.toList()))
                .build();
    }
}

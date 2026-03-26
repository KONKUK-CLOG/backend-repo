package konkuk.clog.domain.chat.dto;

import java.time.Instant;
import java.util.List;
import konkuk.clog.domain.chat.document.ChatMessage;
import konkuk.clog.domain.chat.domain.ChatRole;
import konkuk.clog.domain.chat.domain.CodeSnippet;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageView {

    private String id;
    private ChatRole role;
    private String content;
    private List<CodeSnippet> codeSnippets;
    private String reasoning;
    private String markdown;
    private Long blogId;
    private Instant createdAt;

    public static ChatMessageView from(ChatMessage m) {
        return ChatMessageView.builder()
                .id(m.getId())
                .role(m.getRole())
                .content(m.getContent())
                .codeSnippets(m.getCodeSnippets())
                .reasoning(m.getReasoning())
                .markdown(m.getMarkdown())
                .blogId(m.getBlogId())
                .createdAt(m.getCreatedAt())
                .build();
    }
}

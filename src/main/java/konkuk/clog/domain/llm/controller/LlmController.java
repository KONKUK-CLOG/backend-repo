package konkuk.clog.domain.llm.controller;

import jakarta.validation.Valid;
import konkuk.clog.domain.chat.dto.ChatSendRequest;
import konkuk.clog.domain.chat.service.ChatService;
import konkuk.clog.domain.llm.dto.BlogGenerateRequest;
import konkuk.clog.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 플랜 호환 별칭 — {@code POST /api/blogs/generate} 는 채팅 SSE 와 동일 동작.
 */
@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
public class LlmController {

    private static final long SSE_TIMEOUT_MS = 900_000L;

    private final ChatService chatService;

    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generate(@Valid @RequestBody BlogGenerateRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        ChatSendRequest chat = new ChatSendRequest();
        chat.setChatSessionId(request.getChatSessionId());
        chat.setMessage(request.getMessage());
        chat.setCodeSnippets(request.getCodeSnippets());
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        chatService.streamChatAndSendSse(userId, chat, emitter);
        return emitter;
    }
}

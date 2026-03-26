package konkuk.clog.domain.chat.controller;

import jakarta.validation.Valid;
import konkuk.clog.domain.chat.dto.ChatHistoryResponse;
import konkuk.clog.domain.chat.dto.ChatSendRequest;
import konkuk.clog.domain.chat.service.ChatService;
import konkuk.clog.global.dto.ApiResponse;
import konkuk.clog.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Extension 채팅 API — {@code /send} 는 Lambda 응답을 SSE 로 스트리밍한다.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final long SSE_TIMEOUT_MS = 900_000L;

    private final ChatService chatService;

    @GetMapping("/history")
    public ApiResponse<ChatHistoryResponse> history() {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.success(chatService.loadActiveHistory(userId));
    }

    /**
     * 이벤트 순서: {@code reasoning} → {@code markdown} → {@code done} (또는 {@code error}).
     */
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter send(@Valid @RequestBody ChatSendRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        chatService.streamChatAndSendSse(userId, request, emitter);
        return emitter;
    }
}

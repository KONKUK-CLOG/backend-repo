package konkuk.clog.domain.chat.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import konkuk.clog.domain.chat.document.ChatMessage;
import konkuk.clog.domain.chat.document.ChatSession;
import konkuk.clog.domain.chat.domain.ChatRole;
import konkuk.clog.domain.chat.domain.ChatSessionStatus;
import konkuk.clog.domain.chat.domain.CodeSnippet;
import konkuk.clog.domain.chat.dto.ChatHistoryResponse;
import konkuk.clog.domain.chat.dto.ChatSendRequest;
import konkuk.clog.domain.chat.repository.ChatMessageRepository;
import konkuk.clog.domain.chat.repository.ChatSessionRepository;
import konkuk.clog.domain.llm.dto.LambdaChatTurn;
import konkuk.clog.domain.llm.dto.LambdaPayload;
import konkuk.clog.domain.llm.dto.LambdaResult;
import konkuk.clog.domain.llm.service.LlmService;
import konkuk.clog.global.exception.BusinessException;
import konkuk.clog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 채팅 세션 생명주기, 토큰 한도 초과 시 요약·세션 전환, Lambda 호출 및 SSE 이벤트 순서(reasoning → markdown → done).
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final LlmService llmService;

    @Value("${app.chat.max-context-tokens:60000}")
    private int maxContextTokens;

    @Transactional(readOnly = true)
    public ChatHistoryResponse loadActiveHistory(Long userId) {
        ChatSession session = chatSessionRepository
                .findByUserIdAndStatus(userId, ChatSessionStatus.ACTIVE)
                .orElse(null);
        if (session == null) {
            return ChatHistoryResponse.of(null, List.of());
        }
        List<ChatMessage> messages =
                chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        return ChatHistoryResponse.of(session.getId(), messages);
    }

    /**
     * 유저 메시지 저장 → (필요 시) 요약 후 세션 회전 → Lambda GENERATE → SSE 스트리밍 → 어시스턴트 메시지 저장.
     * <p>이벤트 순서: {@code reasoning} → {@code markdown} → {@code done} (플랜 UX).</p>
     * <p>Mongo 단일 문서 저장 위주라 긴 트랜잭션은 사용하지 않는다.</p>
     */
    public void streamChatAndSendSse(Long userId, ChatSendRequest request, SseEmitter emitter) {
        try {
            ChatSession resolved = resolveSession(userId, request.getChatSessionId());
            maybeRotateSessionForTokenBudget(userId, resolved, request.getMessage(), request.getCodeSnippets());

            ChatSession session = chatSessionRepository
                    .findByUserIdAndStatus(userId, ChatSessionStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND));

            Instant now = Instant.now();
            ChatMessage userMsg = ChatMessage.builder()
                    .sessionId(session.getId())
                    .role(ChatRole.USER)
                    .content(request.getMessage())
                    .codeSnippets(request.getCodeSnippets() != null ? request.getCodeSnippets() : List.of())
                    .estimatedTokens(estimateTokens(request.getMessage(), request.getCodeSnippets()))
                    .createdAt(now)
                    .build();
            userMsg = chatMessageRepository.save(userMsg);

            session.setTotalTokenCount(session.getTotalTokenCount() + userMsg.getEstimatedTokens());
            session.setUpdatedAt(now);
            chatSessionRepository.save(session);

            LambdaPayload payload = buildGeneratePayload(userId, session, request);
            LambdaResult result = llmService.invoke(payload);

            if (result.getMarkdown() == null && result.getReasoning() == null) {
                throw new BusinessException(ErrorCode.LLM_RESPONSE_PARSE_FAILED);
            }

            sendSseEvent(emitter, "reasoning", Map.of(
                    "content", result.getReasoning() != null ? result.getReasoning() : ""));
            sendSseEvent(emitter, "markdown", Map.of(
                    "content", result.getMarkdown() != null ? result.getMarkdown() : ""));

            ChatMessage assistantMsg = ChatMessage.builder()
                    .sessionId(session.getId())
                    .role(ChatRole.ASSISTANT)
                    .content(result.getMarkdown() != null ? result.getMarkdown() : "")
                    .reasoning(result.getReasoning())
                    .markdown(result.getMarkdown())
                    .estimatedTokens(estimateTokens(
                            String.valueOf(result.getReasoning()) + String.valueOf(result.getMarkdown()), null))
                    .createdAt(Instant.now())
                    .build();
            assistantMsg = chatMessageRepository.save(assistantMsg);

            session.setTotalTokenCount(session.getTotalTokenCount() + assistantMsg.getEstimatedTokens());
            session.setUpdatedAt(Instant.now());
            chatSessionRepository.save(session);

            sendSseEvent(emitter, "done", Map.of(
                    "sessionId", session.getId(),
                    "assistantMessageId", String.valueOf(assistantMsg.getId())));

            emitter.complete();
        } catch (BusinessException e) {
            try {
                sendSseEvent(emitter, "error", Map.of("message", e.getErrorCode().getMessage()));
            } catch (IOException ignored) {
                // ignore
            }
            emitter.completeWithError(e);
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, Map<String, ?> data) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data));
    }

    private ChatSession resolveSession(Long userId, String chatSessionId) {
        if (chatSessionId != null && !chatSessionId.isBlank()) {
            ChatSession s = chatSessionRepository.findById(chatSessionId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND));
            if (!s.getUserId().equals(userId) || s.getStatus() != ChatSessionStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND);
            }
            return s;
        }
        return chatSessionRepository
                .findByUserIdAndStatus(userId, ChatSessionStatus.ACTIVE)
                .orElseGet(() -> createFreshSession(userId));
    }

    private ChatSession createFreshSession(Long userId) {
        Instant now = Instant.now();
        ChatSession s = ChatSession.builder()
                .userId(userId)
                .status(ChatSessionStatus.ACTIVE)
                .systemMessage(null)
                .totalTokenCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return chatSessionRepository.save(s);
    }

    /**
     * 새 유저 입력을 넣기 전에 합산 토큰이 한도를 넘기면, 기존 대화를 Lambda 로 요약하고 새 ACTIVE 세션으로 교체한다.
     */
    private void maybeRotateSessionForTokenBudget(
            Long userId,
            ChatSession session,
            String incomingMessage,
            List<CodeSnippet> incomingSnippets) {

        List<ChatMessage> existing =
                chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        int historical = existing.stream().mapToInt(ChatMessage::getEstimatedTokens).sum();
        int incoming = estimateTokens(incomingMessage, incomingSnippets);

        if (historical + incoming <= maxContextTokens) {
            return;
        }

        if (existing.isEmpty() && (session.getSystemMessage() == null || session.getSystemMessage().isBlank())) {
            throw new BusinessException(ErrorCode.CHAT_CONTEXT_OVERFLOW);
        }

        LambdaPayload summarizePayload = LambdaPayload.builder()
                .action("SUMMARIZE")
                .userId(userId)
                .chatHistory(buildTurnsFromSession(session, existing))
                .codeSnippets(List.of())
                .prompt("Summarize the following conversation for continued context.")
                .build();

        LambdaResult sumResult = llmService.invoke(summarizePayload);
        if (sumResult.getSummary() == null || sumResult.getSummary().isBlank()) {
            throw new BusinessException(ErrorCode.LLM_RESPONSE_PARSE_FAILED);
        }

        session.setStatus(ChatSessionStatus.ARCHIVED);
        session.setUpdatedAt(Instant.now());
        chatSessionRepository.save(session);

        Instant now = Instant.now();
        ChatSession fresh = ChatSession.builder()
                .userId(userId)
                .status(ChatSessionStatus.ACTIVE)
                .systemMessage(sumResult.getSummary())
                .totalTokenCount(estimateTokens(sumResult.getSummary(), null))
                .createdAt(now)
                .updatedAt(now)
                .build();
        chatSessionRepository.save(fresh);
    }

    private List<LambdaChatTurn> buildTurnsFromSession(ChatSession session, List<ChatMessage> messages) {
        List<LambdaChatTurn> turns = new ArrayList<>();
        if (session.getSystemMessage() != null && !session.getSystemMessage().isBlank()) {
            turns.add(LambdaChatTurn.builder()
                    .role("system")
                    .content(session.getSystemMessage())
                    .build());
        }
        for (ChatMessage m : messages) {
            String role = switch (m.getRole()) {
                case USER -> "user";
                case ASSISTANT -> "assistant";
                case SYSTEM -> "system";
            };
            turns.add(LambdaChatTurn.builder()
                    .role(role)
                    .content(m.getContent())
                    .codeSnippets(m.getCodeSnippets())
                    .build());
        }
        return turns;
    }

    private LambdaPayload buildGeneratePayload(Long userId, ChatSession session, ChatSendRequest request) {
        List<ChatMessage> messages =
                chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        List<LambdaChatTurn> turns = buildTurnsFromSession(session, messages);
        return LambdaPayload.builder()
                .action("GENERATE")
                .userId(userId)
                .chatHistory(turns)
                .codeSnippets(request.getCodeSnippets())
                .prompt(request.getMessage())
                .build();
    }

    /** 대략적 토큰 수(영문 기준 문자/4 근사 — 한글은 더 짧게 잡힐 수 있음). */
    private int estimateTokens(String text, List<CodeSnippet> snippets) {
        int n = text != null ? text.length() / 4 : 0;
        if (snippets == null) {
            return Math.max(n, 1);
        }
        for (CodeSnippet s : snippets) {
            if (s.getCode() != null) {
                n += s.getCode().length() / 4;
            }
        }
        return Math.max(n, 1);
    }
}

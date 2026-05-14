package konkuk.clog.domain.chat.service;

import konkuk.clog.domain.chat.document.ChatMessage;
import konkuk.clog.domain.chat.document.ChatSession;
import konkuk.clog.domain.chat.domain.ChatRole;
import konkuk.clog.domain.chat.domain.ChatSessionStatus;
import konkuk.clog.domain.chat.dto.ChatSendRequest;
import konkuk.clog.domain.chat.repository.ChatMessageRepository;
import konkuk.clog.domain.chat.repository.ChatSessionRepository;
import konkuk.clog.domain.llm.dto.LambdaPayload;
import konkuk.clog.domain.llm.dto.LambdaResult;
import konkuk.clog.domain.llm.service.LlmService;
import konkuk.clog.domain.project.repository.ProjectFileRepository;
import konkuk.clog.global.exception.BusinessException;
import konkuk.clog.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ChatService 의 세션 생성, Lambda 호출, SSE 이벤트 순서를 AWS/MongoDB 없이 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock ChatSessionRepository chatSessionRepository;
    @Mock ChatMessageRepository chatMessageRepository;
    @Mock LlmService llmService;
    @Mock ProjectFileRepository projectFileRepository;

    ChatService chatService;

    static final Long USER_ID = 1L;
    static final String SESSION_ID = "session-abc";

    @BeforeEach
    void setUp() {
        chatService =
                new ChatService(chatSessionRepository, chatMessageRepository, llmService, projectFileRepository);
        ReflectionTestUtils.setField(chatService, "maxContextTokens", 60000);
        lenient().when(projectFileRepository.findAllByProjectIdOrderByFilePathAsc(any()))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("활성 세션 없을 때 새 세션 생성 후 GENERATE 호출, SSE 3이벤트 전송, complete()")
    void streamChat_new_session_full_success() throws Exception {
        ChatSession session = activeSession();
        when(chatSessionRepository.findByUserIdAndProjectIdAndStatus(
                        USER_ID, null, ChatSessionStatus.ACTIVE))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(session));
        when(chatSessionRepository.save(any(ChatSession.class))).thenReturn(session);
        when(chatMessageRepository.save(any())).thenReturn(userMessage(session));
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of());

        LambdaResult lambdaResult = makeResult("사고 과정입니다", "# 생성된 블로그 내용");
        when(llmService.invoke(any())).thenReturn(lambdaResult);

        SseEmitter emitter = mock(SseEmitter.class);
        chatService.streamChatAndSendSse(USER_ID, request("블로그 글 써줘"), emitter);

        ArgumentCaptor<LambdaPayload> payloadCaptor = ArgumentCaptor.forClass(LambdaPayload.class);
        verify(llmService).invoke(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().getAction()).isEqualTo("GENERATE");
        assertThat(payloadCaptor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(payloadCaptor.getValue().getPrompt()).isEqualTo("블로그 글 써줘");

        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
        verify(emitter, times(3)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
        verify(emitter, never()).completeWithError(any(Throwable.class));
    }

    @Test
    @DisplayName("기존 활성 세션 있을 때 세션 재사용, Lambda 호출 후 complete()")
    void streamChat_existing_session_reused() throws Exception {
        ChatSession session = activeSession();
        when(chatSessionRepository.findByUserIdAndProjectIdAndStatus(
                        USER_ID, null, ChatSessionStatus.ACTIVE))
                .thenReturn(Optional.of(session));
        when(chatSessionRepository.save(any())).thenReturn(session);
        when(chatMessageRepository.save(any())).thenReturn(userMessage(session));
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of());
        when(llmService.invoke(any())).thenReturn(makeResult("thinking", "content"));

        SseEmitter emitter = mock(SseEmitter.class);
        chatService.streamChatAndSendSse(USER_ID, request("질문입니다"), emitter);

        verify(chatSessionRepository, never()).save(argThat(s -> ((ChatSession) s).getId() == null));

        verify(llmService).invoke(any());
        verify(emitter).complete();
    }

    @Test
    @DisplayName("Lambda 실패 시 error SSE 이벤트 전송 후 completeWithError()")
    void streamChat_lambda_failure_sends_error_event() throws Exception {
        ChatSession session = activeSession();
        when(chatSessionRepository.findByUserIdAndProjectIdAndStatus(
                        USER_ID, null, ChatSessionStatus.ACTIVE))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(session));
        when(chatSessionRepository.save(any(ChatSession.class))).thenReturn(session);
        when(chatMessageRepository.save(any())).thenReturn(userMessage(session));
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of());

        when(llmService.invoke(any()))
                .thenThrow(new BusinessException(ErrorCode.LLM_INVOCATION_FAILED));

        SseEmitter emitter = mock(SseEmitter.class);
        chatService.streamChatAndSendSse(USER_ID, request("test"), emitter);

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).completeWithError(any(BusinessException.class));
        verify(emitter, never()).complete();
    }

    @Test
    @DisplayName("토큰 한도 초과 시 SUMMARIZE 후 메시지·세션 삭제, 새 세션으로 GENERATE 호출")
    void streamChat_rotates_session_on_token_overflow() throws Exception {
        int maxTokens = 90;
        ReflectionTestUtils.setField(chatService, "maxContextTokens", maxTokens);

        ChatSession heavySession = activeSession();
        ChatSession freshSession = ChatSession.builder()
                .id("session-fresh")
                .userId(USER_ID)
                .projectId(null)
                .status(ChatSessionStatus.ACTIVE)
                .totalTokenCount(10)
                .systemMessage("이전 대화 요약본")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ChatMessage heavyMsg = ChatMessage.builder()
                .id("old-msg")
                .sessionId(SESSION_ID)
                .role(ChatRole.USER)
                .content("긴 대화")
                .estimatedTokens(95)
                .createdAt(Instant.now())
                .build();

        when(chatSessionRepository.findByUserIdAndProjectIdAndStatus(
                        USER_ID, null, ChatSessionStatus.ACTIVE))
                .thenReturn(Optional.of(heavySession))
                .thenReturn(Optional.of(freshSession));

        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(inv -> {
            ChatSession arg = inv.getArgument(0);
            if (arg.getId() != null) {
                return arg;
            }
            if ("이전 대화 요약본".equals(arg.getSystemMessage())) {
                return freshSession;
            }
            return ChatSession.builder()
                    .id(SESSION_ID)
                    .userId(arg.getUserId())
                    .projectId(arg.getProjectId())
                    .status(arg.getStatus())
                    .systemMessage(arg.getSystemMessage())
                    .totalTokenCount(arg.getTotalTokenCount())
                    .createdAt(arg.getCreatedAt())
                    .updatedAt(arg.getUpdatedAt())
                    .build();
        });

        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of(heavyMsg));
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc("session-fresh"))
                .thenReturn(List.of());

        LambdaResult summarizeResult = new LambdaResult();
        summarizeResult.setSummary("이전 대화 요약본");
        LambdaResult generateResult = makeResult("thinking", "new blog");
        when(llmService.invoke(any())).thenReturn(summarizeResult).thenReturn(generateResult);

        when(chatMessageRepository.save(any()))
                .thenAnswer(inv -> {
                    ChatMessage m = inv.getArgument(0);
                    if (m.getId() == null) {
                        m = ChatMessage.builder()
                                .id("msg-new")
                                .sessionId(m.getSessionId())
                                .role(m.getRole())
                                .content(m.getContent())
                                .estimatedTokens(m.getEstimatedTokens())
                                .createdAt(Instant.now())
                                .build();
                    }
                    return m;
                });

        SseEmitter emitter = mock(SseEmitter.class);
        chatService.streamChatAndSendSse(USER_ID, request("새 질문"), emitter);

        ArgumentCaptor<LambdaPayload> captor = ArgumentCaptor.forClass(LambdaPayload.class);
        verify(llmService, times(2)).invoke(captor.capture());
        assertThat(captor.getAllValues().get(0).getAction()).isEqualTo("SUMMARIZE");
        assertThat(captor.getAllValues().get(1).getAction()).isEqualTo("GENERATE");

        verify(chatMessageRepository).deleteBySessionId(eq(SESSION_ID));
        verify(chatSessionRepository).deleteById(eq(SESSION_ID));
        verify(emitter).complete();
    }

    @Test
    @DisplayName("활성 세션 없으면 빈 히스토리 반환")
    void loadHistory_returns_empty_when_no_session() {
        when(chatSessionRepository.findByUserIdAndProjectIdAndStatus(
                        USER_ID, null, ChatSessionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        var history = chatService.loadActiveHistory(USER_ID, null);

        assertThat(history.getSessionId()).isNull();
        assertThat(history.getMessages()).isEmpty();
    }

    @Test
    @DisplayName("활성 세션 있으면 해당 메시지 목록 반환")
    void loadHistory_returns_messages_for_active_session() {
        ChatSession session = activeSession();
        when(chatSessionRepository.findByUserIdAndProjectIdAndStatus(
                        USER_ID, null, ChatSessionStatus.ACTIVE))
                .thenReturn(Optional.of(session));

        ChatMessage msg = userMessage(session);
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of(msg));

        var history = chatService.loadActiveHistory(USER_ID, null);

        assertThat(history.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(history.getMessages()).hasSize(1);
    }

    private ChatSession activeSession() {
        return ChatSession.builder()
                .id(SESSION_ID)
                .userId(USER_ID)
                .projectId(null)
                .status(ChatSessionStatus.ACTIVE)
                .totalTokenCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private ChatMessage userMessage(ChatSession session) {
        return ChatMessage.builder()
                .id("msg-1")
                .sessionId(session.getId())
                .role(ChatRole.USER)
                .content("메시지 내용")
                .estimatedTokens(3)
                .createdAt(Instant.now())
                .build();
    }

    private LambdaResult makeResult(String reasoning, String markdown) {
        LambdaResult r = new LambdaResult();
        r.setReasoning(reasoning);
        r.setMarkdown(markdown);
        return r;
    }

    private ChatSendRequest request(String message) {
        ChatSendRequest r = new ChatSendRequest();
        r.setMessage(message);
        return r;
    }
}

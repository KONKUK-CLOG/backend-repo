package konkuk.clog.domain.llm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import konkuk.clog.domain.chat.domain.CodeSnippet;
import konkuk.clog.domain.llm.dto.LambdaPayload;
import konkuk.clog.domain.llm.dto.LambdaResult;
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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AWS Lambda 호출 없이 LlmService 의 직렬화·역직렬화·에러 처리를 검증한다.
 * LambdaClient 를 Mockito 로 교체하여 실제 AWS 자격 증명 없이 실행된다.
 */
@ExtendWith(MockitoExtension.class)
class LlmServiceTest {

    @Mock
    LambdaClient lambdaClient;

    LlmService llmService;

    static final String FUNCTION_NAME = "clog-llm-generator";

    @BeforeEach
    void setUp() {
        llmService = new LlmService(lambdaClient, new ObjectMapper());
        ReflectionTestUtils.setField(llmService, "functionName", FUNCTION_NAME);
    }

    // ── 정상 케이스 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GENERATE 성공 — reasoning 과 markdown 이 LambdaResult 로 파싱된다")
    void invoke_generate_success() {
        String responseJson = "{\"reasoning\":\"생각 중...\",\"markdown\":\"# 블로그 본문\"}";
        stubLambda(responseJson);

        LambdaResult result = llmService.invoke(generatePayload("블로그 글 써줘"));

        assertThat(result.getReasoning()).isEqualTo("생각 중...");
        assertThat(result.getMarkdown()).isEqualTo("# 블로그 본문");
        assertThat(result.getSummary()).isNull();
    }

    @Test
    @DisplayName("SUMMARIZE 성공 — summary 필드가 LambdaResult 로 파싱된다")
    void invoke_summarize_success() {
        String responseJson = "{\"summary\":\"이전 대화 요약 내용입니다.\"}";
        stubLambda(responseJson);

        LambdaPayload payload = LambdaPayload.builder()
                .action("SUMMARIZE")
                .userId(1L)
                .chatHistory(List.of())
                .codeSnippets(List.of())
                .prompt("Summarize the conversation.")
                .build();

        LambdaResult result = llmService.invoke(payload);

        assertThat(result.getSummary()).isEqualTo("이전 대화 요약 내용입니다.");
        assertThat(result.getMarkdown()).isNull();
        assertThat(result.getReasoning()).isNull();
    }

    @Test
    @DisplayName("코드 스니펫 포함 요청 — payload 에 codeSnippets 가 직렬화된다")
    void invoke_with_code_snippets() {
        stubLambda("{\"reasoning\":\"r\",\"markdown\":\"m\"}");

        CodeSnippet snippet = CodeSnippet.builder()
                .fileName("Main.java")
                .language("java")
                .startLine(1)
                .endLine(10)
                .code("public class Main {}")
                .build();

        LambdaPayload payload = LambdaPayload.builder()
                .action("GENERATE")
                .userId(42L)
                .chatHistory(List.of())
                .codeSnippets(List.of(snippet))
                .prompt("이 코드 설명해줘")
                .build();

        ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
        llmService.invoke(payload);

        verify(lambdaClient).invoke(captor.capture());
        String sentJson = captor.getValue().payload().asUtf8String();
        assertThat(sentJson).contains("Main.java");
        assertThat(sentJson).contains("public class Main {}");
        assertThat(sentJson).contains("GENERATE");
    }

    @Test
    @DisplayName("Lambda 가 올바른 functionName 으로 호출된다")
    void invoke_uses_configured_function_name() {
        stubLambda("{\"reasoning\":\"r\",\"markdown\":\"m\"}");

        ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
        llmService.invoke(generatePayload("test"));

        verify(lambdaClient).invoke(captor.capture());
        assertThat(captor.getValue().functionName()).isEqualTo(FUNCTION_NAME);
    }

    // ── 에러 케이스 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Lambda functionError 응답 → LLM_INVOCATION_FAILED 예외")
    void invoke_throws_on_function_error() {
        InvokeResponse errorResponse = InvokeResponse.builder()
                .functionError("Unhandled")
                .payload(SdkBytes.fromUtf8String("{\"errorMessage\":\"Runtime exception\"}"))
                .build();
        when(lambdaClient.invoke(any(InvokeRequest.class))).thenReturn(errorResponse);

        assertThatThrownBy(() -> llmService.invoke(generatePayload("test")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LLM_INVOCATION_FAILED);
    }

    @Test
    @DisplayName("Lambda payload 가 null → LLM_RESPONSE_PARSE_FAILED 예외")
    void invoke_throws_on_null_payload() {
        InvokeResponse nullPayload = InvokeResponse.builder().build();
        when(lambdaClient.invoke(any(InvokeRequest.class))).thenReturn(nullPayload);

        assertThatThrownBy(() -> llmService.invoke(generatePayload("test")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LLM_RESPONSE_PARSE_FAILED);
    }

    @Test
    @DisplayName("Lambda 가 잘못된 JSON 반환 → LLM_RESPONSE_PARSE_FAILED 예외")
    void invoke_throws_on_malformed_json() {
        stubLambda("NOT_VALID_JSON");

        assertThatThrownBy(() -> llmService.invoke(generatePayload("test")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LLM_RESPONSE_PARSE_FAILED);
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────────

    private void stubLambda(String responseJson) {
        InvokeResponse response = InvokeResponse.builder()
                .payload(SdkBytes.fromUtf8String(responseJson))
                .build();
        when(lambdaClient.invoke(any(InvokeRequest.class))).thenReturn(response);
    }

    private LambdaPayload generatePayload(String prompt) {
        return LambdaPayload.builder()
                .action("GENERATE")
                .userId(1L)
                .chatHistory(List.of())
                .codeSnippets(List.of())
                .prompt(prompt)
                .build();
    }
}

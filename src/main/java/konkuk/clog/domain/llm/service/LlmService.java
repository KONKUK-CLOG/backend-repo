package konkuk.clog.domain.llm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import konkuk.clog.domain.llm.dto.LambdaPayload;
import konkuk.clog.domain.llm.dto.LambdaResult;
import konkuk.clog.global.exception.BusinessException;
import konkuk.clog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;

/**
 * Lambda 동기 호출 및 응답 JSON 역직렬화 — SSE 는 {@link konkuk.clog.domain.chat.service.ChatService} 가 전송.
 */
@Service
@RequiredArgsConstructor
public class LlmService {

    private final LambdaClient lambdaClient;
    /** Spring Boot 기본 ObjectMapper — Lambda JSON 직렬화에 사용. */
    private final ObjectMapper objectMapper;

    @Value("${aws.lambda.function-name}")
    private String functionName;

    /**
     * GENERATE / SUMMARIZE 공통 호출 — 페이로드의 {@code action} 에 따라 Lambda 내부 분기.
     */
    public LambdaResult invoke(LambdaPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            InvokeRequest req = InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromUtf8String(json))
                    .build();
            InvokeResponse res = lambdaClient.invoke(req);
            if (res.functionError() != null && !res.functionError().isEmpty()) {
                throw new BusinessException(ErrorCode.LLM_INVOCATION_FAILED);
            }
            SdkBytes out = res.payload();
            if (out == null) {
                throw new BusinessException(ErrorCode.LLM_RESPONSE_PARSE_FAILED);
            }
            return objectMapper.readValue(out.asUtf8String(), LambdaResult.class);
        } catch (BusinessException e) {
            throw e;
        } catch (LambdaException e) {
            throw new BusinessException(ErrorCode.LLM_INVOCATION_FAILED);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.LLM_RESPONSE_PARSE_FAILED);
        }
    }
}

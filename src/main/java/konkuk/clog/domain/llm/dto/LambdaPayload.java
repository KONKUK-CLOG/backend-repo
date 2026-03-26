package konkuk.clog.domain.llm.dto;

import java.util.List;
import konkuk.clog.domain.chat.domain.CodeSnippet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lambda 함수 JSON 입력 계약.
 * <pre>
 * {
 *   "action": "GENERATE" | "SUMMARIZE",
 *   "userId": 123,
 *   "chatHistory": [...],
 *   "codeSnippets": [...],
 *   "prompt": "..."
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LambdaPayload {

    private String action;

    private Long userId;

    private List<LambdaChatTurn> chatHistory;

    private List<CodeSnippet> codeSnippets;

    private String prompt;
}

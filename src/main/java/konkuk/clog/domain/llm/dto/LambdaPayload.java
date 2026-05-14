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
 *   "prompt": "...",
 *   "projectFiles": [ { "filePath": "...", "language": "...", "content": "..." } ]
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

    /** GENERATE 시 프로젝트 스냅샷 — Lambda 가 무시해도 됨(@JsonIgnoreProperties). */
    private List<ProjectFileContext> projectFiles;
}

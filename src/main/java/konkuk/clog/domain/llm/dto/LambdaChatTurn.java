package konkuk.clog.domain.llm.dto;

import java.util.List;
import konkuk.clog.domain.chat.domain.CodeSnippet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lambda 로 전달하는 대화 한 턴 — role + 본문 + (유저 턴에 한해) 코드 스니펫.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LambdaChatTurn {

    private String role;

    private String content;

    private List<CodeSnippet> codeSnippets;
}

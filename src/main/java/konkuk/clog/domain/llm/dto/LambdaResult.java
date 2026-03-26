package konkuk.clog.domain.llm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lambda 함수 JSON 출력 계약 — GENERATE 시 reasoning/markdown, SUMMARIZE 시 summary.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LambdaResult {

    private String reasoning;

    private String markdown;

    /** SUMMARIZE 액션일 때 압축된 이전 대화 텍스트. */
    private String summary;
}

package konkuk.clog.domain.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Extension Ctrl+L 로 첨부된 코드 조각 — MongoDB 에 임베디드 문서로 저장.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeSnippet {

    private String fileName;
    private String language;
    private Integer startLine;
    private Integer endLine;
    private String code;
}

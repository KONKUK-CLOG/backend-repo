package konkuk.clog.domain.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lambda 에 프로젝트 코드 스냅샷을 넘길 때 사용하는 최소 필드. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFileContext {

    private String filePath;

    private String language;

    private String content;
}

package konkuk.clog.domain.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProjectFileCreateRequest {

    @NotBlank
    private String filePath;

    private String language;

    @NotBlank
    private String content;
}

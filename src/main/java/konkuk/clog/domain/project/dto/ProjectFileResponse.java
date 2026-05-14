package konkuk.clog.domain.project.dto;

import java.time.Instant;
import konkuk.clog.domain.project.document.ProjectFile;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProjectFileResponse {

    private final String id;
    private final String projectId;
    private final String filePath;
    private final String language;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static ProjectFileResponse from(ProjectFile f) {
        return new ProjectFileResponse(
                f.getId(),
                f.getProjectId(),
                f.getFilePath(),
                f.getLanguage(),
                f.getCreatedAt(),
                f.getUpdatedAt());
    }
}

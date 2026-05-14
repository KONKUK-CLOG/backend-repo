package konkuk.clog.domain.project.dto;

import java.time.Instant;
import konkuk.clog.domain.project.document.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProjectResponse {

    private final String id;
    private final Long userId;
    private final String name;
    private final String description;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static ProjectResponse from(Project p) {
        return new ProjectResponse(
                p.getId(),
                p.getUserId(),
                p.getName(),
                p.getDescription(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}

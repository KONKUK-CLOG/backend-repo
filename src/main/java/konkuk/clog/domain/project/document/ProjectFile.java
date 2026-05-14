package konkuk.clog.domain.project.document;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "project_files")
public class ProjectFile {

    @Id
    private String id;

    private String projectId;

    private String filePath;

    private String language;

    private String content;

    private Instant createdAt;

    private Instant updatedAt;
}

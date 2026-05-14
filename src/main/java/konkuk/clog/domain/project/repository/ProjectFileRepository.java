package konkuk.clog.domain.project.repository;

import java.util.List;
import java.util.Optional;
import konkuk.clog.domain.project.document.ProjectFile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProjectFileRepository extends MongoRepository<ProjectFile, String> {

    List<ProjectFile> findAllByProjectIdOrderByFilePathAsc(String projectId);

    long countByProjectId(String projectId);

    boolean existsByProjectIdAndFilePath(String projectId, String filePath);

    void deleteAllByProjectId(String projectId);

    Optional<ProjectFile> findByIdAndProjectId(String id, String projectId);

    Optional<ProjectFile> findByProjectIdAndFilePath(String projectId, String filePath);
}

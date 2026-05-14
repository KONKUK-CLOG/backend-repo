package konkuk.clog.domain.project.repository;

import java.util.List;
import java.util.Optional;
import konkuk.clog.domain.project.document.Project;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProjectRepository extends MongoRepository<Project, String> {

    List<Project> findAllByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<Project> findByIdAndUserId(String id, Long userId);
}

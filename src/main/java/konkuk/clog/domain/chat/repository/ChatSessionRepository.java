package konkuk.clog.domain.chat.repository;

import java.util.List;
import java.util.Optional;
import konkuk.clog.domain.chat.document.ChatSession;
import konkuk.clog.domain.chat.domain.ChatSessionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    Optional<ChatSession> findByUserIdAndProjectIdAndStatus(
            Long userId, String projectId, ChatSessionStatus status);

    List<ChatSession> findAllByUserIdAndProjectId(Long userId, String projectId);
}

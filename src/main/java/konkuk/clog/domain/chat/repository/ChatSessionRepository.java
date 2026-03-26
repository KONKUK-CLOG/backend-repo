package konkuk.clog.domain.chat.repository;

import java.util.Optional;
import konkuk.clog.domain.chat.document.ChatSession;
import konkuk.clog.domain.chat.domain.ChatSessionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    Optional<ChatSession> findByUserIdAndStatus(Long userId, ChatSessionStatus status);
}

package konkuk.clog.domain.chat.repository;

import java.util.List;
import konkuk.clog.domain.chat.document.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}

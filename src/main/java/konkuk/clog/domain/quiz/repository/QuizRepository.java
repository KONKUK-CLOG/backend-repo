package konkuk.clog.domain.quiz.repository;

import konkuk.clog.domain.quiz.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
}



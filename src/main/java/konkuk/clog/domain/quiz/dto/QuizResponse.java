package konkuk.clog.domain.quiz.dto;

import java.time.LocalDateTime;
import konkuk.clog.domain.quiz.domain.Quiz;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizResponse {

    private Long id;
    private String question;
    private String answer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static QuizResponse from(Quiz quiz) {
        return QuizResponse.builder()
                .id(quiz.getId())
                .question(quiz.getQuestion())
                .answer(quiz.getAnswer())
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .build();
    }
}



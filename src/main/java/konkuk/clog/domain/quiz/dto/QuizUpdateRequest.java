package konkuk.clog.domain.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuizUpdateRequest {

    @NotBlank(message = "퀴즈 질문은 필수입니다.")
    private String question;

    @NotBlank(message = "퀴즈 정답은 필수입니다.")
    private String answer;
}




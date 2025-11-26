package konkuk.clog.domain.quiz.controller;

import jakarta.validation.Valid;
import java.util.List;
import konkuk.clog.domain.quiz.dto.QuizCreateRequest;
import konkuk.clog.domain.quiz.dto.QuizResponse;
import konkuk.clog.domain.quiz.dto.QuizUpdateRequest;
import konkuk.clog.domain.quiz.service.QuizService;
import konkuk.clog.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping
    public ApiResponse<QuizResponse> createQuiz(
            @Valid @RequestBody QuizCreateRequest request) {
        return ApiResponse.success(quizService.createQuiz(request));
    }

    @PutMapping("/{quizId}")
    public ApiResponse<QuizResponse> updateQuiz(
            @PathVariable Long quizId,
            @Valid @RequestBody QuizUpdateRequest request) {
        return ApiResponse.success(quizService.updateQuiz(quizId, request));
    }

    @DeleteMapping("/{quizId}")
    public ApiResponse<Void> deleteQuiz(@PathVariable Long quizId) {
        quizService.deleteQuiz(quizId);
        return ApiResponse.success();
    }

    @GetMapping("/{quizId}")
    public ApiResponse<QuizResponse> getQuiz(@PathVariable Long quizId) {
        return ApiResponse.success(quizService.getQuizDetail(quizId));
    }

    @GetMapping
    public ApiResponse<List<QuizResponse>> getQuizzes() {
        return ApiResponse.success(quizService.getQuizzes());
    }
}




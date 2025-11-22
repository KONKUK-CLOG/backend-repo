package konkuk.clog.domain.quiz.service;

import java.util.List;
import java.util.stream.Collectors;
import konkuk.clog.domain.quiz.domain.Quiz;
import konkuk.clog.domain.quiz.dto.QuizCreateRequest;
import konkuk.clog.domain.quiz.dto.QuizResponse;
import konkuk.clog.domain.quiz.dto.QuizUpdateRequest;
import konkuk.clog.domain.quiz.repository.QuizRepository;
import konkuk.clog.global.exception.BusinessException;
import konkuk.clog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;

    @Transactional
    public QuizResponse createQuiz(QuizCreateRequest request) {
        Quiz quiz = Quiz.builder()
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .build();
        return QuizResponse.from(quizRepository.save(quiz));
    }

    @Transactional
    public QuizResponse updateQuiz(Long quizId, QuizUpdateRequest request) {
        Quiz quiz = getQuiz(quizId);
        quiz.update(request.getQuestion(), request.getAnswer());
        return QuizResponse.from(quiz);
    }

    @Transactional
    public void deleteQuiz(Long quizId) {
        Quiz quiz = getQuiz(quizId);
        quizRepository.delete(quiz);
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuizDetail(Long quizId) {
        return QuizResponse.from(getQuiz(quizId));
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> getQuizzes() {
        return quizRepository.findAll().stream()
                .map(QuizResponse::from)
                .collect(Collectors.toList());
    }

    private Quiz getQuiz(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));
    }
}



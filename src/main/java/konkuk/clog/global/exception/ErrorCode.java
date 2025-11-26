package konkuk.clog.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 사용자입니다."),
    BLOG_NOT_FOUND(HttpStatus.NOT_FOUND, "BLOG_NOT_FOUND", "존재하지 않는 블로그입니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "존재하지 않는 댓글입니다."),
    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND", "존재하지 않는 퀴즈입니다."),
    BOOKMARK_ALREADY_EXISTS(HttpStatus.CONFLICT, "BOOKMARK_ALREADY_EXISTS", "이미 북마크한 게시글입니다."),
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOKMARK_NOT_FOUND", "존재하지 않는 북마크입니다."),
    FORBIDDEN_OPERATION(HttpStatus.FORBIDDEN, "FORBIDDEN_OPERATION", "권한이 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다."),
    DUPLICATE_SOCIAL_ID(HttpStatus.CONFLICT, "DUPLICATE_SOCIAL_ID", "이미 등록된 소셜 계정입니다."),
    GITHUB_AUTH_FAILED(HttpStatus.BAD_GATEWAY, "GITHUB_AUTH_FAILED", "깃허브 인증에 실패했습니다."),
    SOCIAL_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "SOCIAL_EMAIL_REQUIRED", "깃허브 계정의 이메일 조회에 실패했습니다."),
    MISSING_USER_ID_FOR_COMMENT(HttpStatus.BAD_REQUEST, "MISSING_USER_ID_FOR_COMMENT", "로그인 사용자가 필요한 요청입니다."),
    INVALID_AUTHOR_TYPE(HttpStatus.BAD_REQUEST, "INVALID_AUTHOR_TYPE", "지원하지 않는 작성자 유형입니다."),
    GUEST_NICKNAME_REQUIRED(HttpStatus.BAD_REQUEST, "GUEST_NICKNAME_REQUIRED", "게스트 닉네임을 입력해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}


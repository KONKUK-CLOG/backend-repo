package konkuk.clog.domain.comment.domain;

/**
 * 댓글 작성자 유형.
 * <p>회원(MEMBER)은 JWT로 식별되고, 게스트(GUEST)는 닉네임만으로 식별한다.</p>
 */
public enum AuthorType {
    /** 로그인 사용자 — SecurityContext의 User와 매핑 */
    MEMBER,
    /** 비로그인 — guestNickname 필수 */
    GUEST
}

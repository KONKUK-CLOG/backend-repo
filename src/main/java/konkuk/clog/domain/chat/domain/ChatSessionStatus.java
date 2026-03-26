package konkuk.clog.domain.chat.domain;

/**
 * MongoDB 채팅 세션 상태 — 활성 한 개만 유저에게 노출, 아카이브는 요약 후 이전 세션.
 */
public enum ChatSessionStatus {
    ACTIVE,
    ARCHIVED
}
